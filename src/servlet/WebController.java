package servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.fbk.textpro.modules.tokenpro.NormalizeText;
import eu.fbk.textpro.wrapper.CATWrapper;
import eu.fbk.textpro.wrapper.NormalizedWhitespaceAnalyzer;
import eu.fbk.textpro.wrapper.TokenProWrapper;
import fbk.hlt.web.WebCrawler;
import fbk.hlt.web.WebPage;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import servlet.parser.CATParser;
import servlet.storage.CrossDocAnnotations;
import servlet.storage.Entities;
import servlet.storage.Repositories;
import sun.jvm.hotspot.oops.LongField;


/**
 * User: cgirardi@fbk.eu
 * Date: 26-apr-2010
 * Time: 14.46.18
 */
public class WebController extends HttpServlet {
    private static final String WELCOMEPAGE = "/pages/index.jsp";
    private static final String ERRORPAGE = "/pages/error.jsp";

    public static Entities entities = null;
    public static Repositories repositories = null;
    public static CrossDocAnnotations annotations = null;
    public static UploadArchives uploadarchives = null;

    public static final Logger logger = Logger.getLogger("cromerLog");
    private static String INDEXPATH = null;
    private final static String ENTITIESINDEXNAME = "_instances";
    private final static String ANNOTATIONSINDEXNAME = "_crossdocannotations";

    private static NormalizeText normText = new NormalizeText();
    private static Hashtable<String, String> lockedResources = new Hashtable<String, String>();
    private static Analyzer analyzer = new NormalizedWhitespaceAnalyzer();

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static Pattern tPattern = Pattern.compile(".* t_id=\"(\\d+)\"");
    private static Pattern mPattern = Pattern.compile(".*<([^ ]+) m_id=\"(\\d+)\".*");
    private static Pattern rPattern = Pattern.compile(".* r_id=\"(\\d+)\".*");
    private static Pattern relatedPattern = Pattern.compile(".* m_id=\"(\\d+)\"\\s* RELATED_TO=\"(\\d+)\".*");

    static final ArrayList<String> entityClasses = new ArrayList();
    static final ArrayList<String> eventClasses = new ArrayList();

    WebCrawler crawler = new WebCrawler();
    public static Hashtable<String,Integer> hash_counter_type = new Hashtable<String,Integer>();
    /*
    If the tag pair is from the same component (5 groups):
        1) TIME_
        2) LOC_
        3) HUMAN_PART_
        4) NON_HUMAN_PART (with or without _!)
        5) ACTION_ or NEG_ACTION_
    */
    private static boolean isOneOfFive (String type1, String type2)  {
        //System.err.println("isOneOfFive "+type1 + " -- " + type2);
        String[] yes_label = {"time_", "loc_", "human_part_", "non_human_part"};
        String[] special_label = {"action_", "neg_action_"};
        for (String label : yes_label) {
            if (type1.startsWith(label) && type2.startsWith(label))
                return true;
        }

        //if one of the tags is ACTION_STATE or NEG_ACTION_STATE -> keep both tags
        int res=0;
        for (String label : special_label) {
            if (type1.startsWith(label))
                res++;
            if (type2.startsWith(label))
                res++;
        }
        return res == 2;
    }

    //if one of the tags is ACTION_STATE or NEG_ACTION_STATE -> keep both tags
    private static boolean atleastOneActionState (String type1, String type2)  {
        String[] no_label = {"action_state", "neg_action_state"};
        //if NONE of the tags is ACTION_STATE or NEG_ACTION_STATE -> delete CAT, keep CROMER
        int res=0;
        for (String label : no_label) {
            if (type1.startsWith(label))
                res++;
            if (type2.startsWith(label))
                res++;
        }
        return res > 0;
    }

    //if NONE of the tags is ACTION_STATE or NEG_ACTION_STATE -> delete CAT, keep CROMER
    private static boolean noneActionState (String type1, String type2)  {
        String[] no_label = {"action_state", "neg_action_state"};
        int res=0;
        for (String label : no_label) {
            if (type1.startsWith(label))
                res++;
            if (type2.startsWith(label))
                res++;
        }
        return res == 0;
    }


    /*
    1) Overlap between CAT vs. CROMER mentions:

    If the tag pair is from the same component (5 groups):
    1) TIME_
    2) LOC_
    3) HUMAN_PART_
    4) NON_HUMAN_PART (with or without _!)
    5) ACTION_ or NEG_ACTION_

    YES -> delete CAT, keep CROMER

    NO -> if one of the tags is ACTION_STATE or NEG_ACTION_STATE:
    ----> keep both mentions only if both mentions are not part of the same relation
    ----> keep CROMER delete CAT if both mentions are part of the same relation
    In our example case they both are part of the same relation so the CAT mention should be deleted.

    NO -> if NONE of the tags is ACTION_STATE or NEG_ACTION_STATE -> delete CAT, keep CROMER

    2) CAT vs. CAT and
    3) CROMER vs. CROMER mentions.

    If the tag pair is from the same component (5 groups):

    YES ->
    A) if only one of the two mentions is part of a relation keep the relation and chose the longest mention (higher number of tokens), delete shorter mention
    B)  what if both related?
    --> to the same relation -> keep one - longest
    --> to different relation IDS -- [HOW MANY?] -> keep both
    C) what of no relations involved  -- [HOW MANY?] -> keep both

    NO -> if one of the tags is ACTION_STATE or NEG_ACTION_STATE:
    ----> keep both mentions only if both mentions are not part of the same relation
    ----> keep one mention - the longest mention and delete the shorter one - if both mentions are part of the same relation

    NO -> if NONE of the tags is ACTION_STATE or NEG_ACTION_STATE ->
    TWO POSSIBILITIES:
    --> if only one of the two is part of a relation -> only keep the one that is part of a relation, delete the other
    --> if both or none of the two are part of a relation  [HOW MANY?] -> keep both

    Last check all source mentions in the relations
    */
    private static String checkPrevMarkable (String docurl, String mid1, String type1, String tids1, String owner1,
                                             String mid2, String type2, String tids2, String owner2,
                                             Hashtable<String,String> relationsFromCAT) {
        if (type1 != null && type2 != null) {
            type1 = type1.toLowerCase();
            type2 = type2.toLowerCase();
            //questo evita che le annotation di CROMER salvate che sono esattamente uguali a quelle fatte in CAT vengano duplicate
            if (type1.equals(type2) && tids1.equals(tids2)) {
                return null;
            }
            if ((type1.equals("entity") || type1.equals("event"))) {
                return "delete1";
            }
            //logger.debug("PASSO " +owner1+ " === "+owner2+ " (" +owner1.equals(owner2)+")");

            // CAT vs. CAT  or CROMER vs. CROMER
            if (owner1.equals(owner2)) {
                if (isOneOfFive(type1,type2)) {
                    if (relationsFromCAT.containsKey(mid2) && !relationsFromCAT.containsKey(mid1) ||
                            relationsFromCAT.containsKey(mid1) && !relationsFromCAT.containsKey(mid2)) {
                        if (tids2.split(" ").length > tids1.split(" ").length) {
                            //logger.debug("#1.1 "+docurl + " "+owner1+" vs. "+owner2+ ": isOneOfFive " + type1+", "+type2);
                            return "delete1";
                        } else if (tids1.split(" ").length > tids2.split(" ").length) {
                            //logger.debug("#1.2 "+docurl + " "+owner1+" vs. "+owner2+ ": isOneOfFive " + type1+", "+type2);
                            return "delete2";
                        }
                    } else if (relationsFromCAT.containsKey(mid2) && relationsFromCAT.containsKey(mid1)) {
                        if (tids2.split(" ").length > tids1.split(" ").length) {
                            //logger.debug("#1.3 "+docurl + " "+owner1+" vs. "+owner2+ ": isOneOfFive " + type1+", "+type2);
                            return "delete1";
                        } else if (tids1.split(" ").length > tids2.split(" ").length) {
                            //logger.debug("#1.4 "+docurl + " "+owner1+" vs. "+owner2+ ": isOneOfFive " + type1+", "+type2);
                            return "delete2";
                        }
                    } else {
                        if (tids2.split(" ").length > tids1.split(" ").length) {
                            //logger.debug("#1.5 "+docurl + " "+owner1+" vs. "+owner2+ ": isOneOfFive " + type1+", "+type2);
                            return "delete1";
                        } else if (tids1.split(" ").length > tids2.split(" ").length) {
                            //logger.debug("#1.6 "+docurl + " "+owner1+" vs. "+owner2+ ": isOneOfFive " + type1+", "+type2);
                            return "delete2";
                        }
                        return "keep";
                    }
                } else {
                    if (atleastOneActionState(type1,type2)) {
                        //logger.debug("#1 "+docurl + " "+owner1+" vs. "+owner2+ ": atleastOneActionState " + type1+", "+type2);
                        if (relationsFromCAT.containsKey(mid1)) {
                            if (relationsFromCAT.get(mid1).contains("<"+type2.toUpperCase())) {
                                //logger.debug("#1 CASE 1 "+relationsFromCAT.get(mid1) + " == "+type2);
                                return "keep";
                            } else {
                                //logger.debug("#1 CASE 2 "+relationsFromCAT.get(mid1) + " != "+type2);
                                return "delete1";
                            }
                        }
                        return "keep";
                    }
                    if (noneActionState(type1,type2)) {
                        if (relationsFromCAT.containsKey(mid2) && !relationsFromCAT.containsKey(mid1)) {
                            //logger.debug("#1.1 "+docurl + " "+owner1+" vs. "+owner2+ ": noneActionState " + type1+", "+type2);
                            return "delete1";
                        } else if (relationsFromCAT.containsKey(mid1) && !relationsFromCAT.containsKey(mid2)) {
                            //logger.debug("#1.2 "+docurl + " "+owner1+" vs. "+owner2+ ": noneActionState " + type1+", "+type2);
                            return "delete2";
                        } else {
                            //logger.debug("#1 "+docurl + " "+owner1+" vs. "+owner2+ ": noneActionState " + type1+", "+type2);
                            return "keep";
                        }
                    }
                }
                //CAT vs. CROMER
            } else {
                if (isOneOfFive(type1,type2)) {
                    //logger.debug("#2 "+docurl + " "+owner1+" vs. "+owner2+ ": isOneOfFive " + type1+", "+type2);
                    return "delete1";
                } else {
                    if (atleastOneActionState(type1, type2)) {
                        //logger.debug("#2 "+docurl + " "+owner1+" vs. "+owner2+ ": atleastOneActionState " + type1+", "+type2);

                        if (relationsFromCAT.containsKey(mid1)) {
                            if (relationsFromCAT.get(mid1).contains("<"+type2.toUpperCase())) {
                                //logger.debug("#2 CASE 1 "+relationsFromCAT.get(mid1) + " == "+type2);
                                return "keep";
                            } else {
                                //logger.debug("#2 CASE 2 "+relationsFromCAT.get(mid1) + " != "+type2);
                                return "delete1";
                            }
                        }
                        return "keep";
                    }
                    if (noneActionState(type1,type2)) {
                        //logger.debug("#2 "+docurl + " "+owner1+" vs. "+owner2+ ": noneActionState " + type1+", "+type2);

                        return "delete1";
                    }
                }
            }
        }
        return null;
    }

    /*
    If the tag pair is from the same component (5 groups):
    YES -> delete CAT, keep CROMER
    NO -> if one of the tags is ACTION_STATE or NEG_ACTION_STATE -> keep both tags
    NO -> if NONE of the tags is ACTION_STATE or NEG_ACTION_STATE -> delete CAT, keep CROMER

     caseVS value could be:
     1 for CAT vs. CAT
     2 for CAT vs. CROMER
     3 for CROMER vs. CROMER
*/
    private static boolean addMarkable (String docurl, int mid, String tids, String type, String owner,
                                        Hashtable<String, Integer> markable, Hashtable<String, String> markableType,
                                        Hashtable<String, String[]> tokenIDAndMarkableType, Hashtable<String,String> relationsFromCAT) {
        String[] tids_item = tids.split(" ");
        //logger.debug("-- addMarkable tokens:" +tids +" "+owner);

        boolean skipThisMarkable = false;
        for (String tmptID : tids_item) {
            if (tokenIDAndMarkableType.get(tmptID) != null) {
                //logger.debug("CROMER overlapping tokens: "+tids);
                String prev_type = tokenIDAndMarkableType.get(tmptID)[0];
                String prev_mid = tokenIDAndMarkableType.get(tmptID)[1];
                String prev_tokenids = tokenIDAndMarkableType.get(tmptID)[2];
                String prev_owner = tokenIDAndMarkableType.get(tmptID)[3];

                String check = checkPrevMarkable(docurl, prev_mid, prev_type, prev_tokenids, prev_owner,
                        String.valueOf(mid), type, tids, owner,
                        relationsFromCAT);
                //logger.debug("+ " +check + " "+mid+" "+prev_tokenids+"/"+tids+ " type1:" +prev_type + ", type2:" + type);
                if (check != null) {
                    boolean deleted = false;
                    if (check.equals("delete1")) {
                        for (String key : markable.keySet()) {
                            if (key.equals(prev_tokenids) || String.valueOf(markable.get(key)).equals(prev_mid)) {
                                markable.remove(key);
                                markableType.remove(key);
                                for (String tid : key.split(" ")) {
                                    tokenIDAndMarkableType.remove(tid);
                                }
                                deleted = true;
                                //logger.debug("! " +docurl + " (tokens: " + key+ ") move from " +prev_owner+ " " +prev_type+" m_id="+prev_mid
                                //        +" to " +owner + " " + type+" m_id="+mid);
                                if (hash_counter_type.containsKey(prev_type+"/"+type)) {
                                    hash_counter_type.put(prev_type+"/"+type,
                                            ((Integer) hash_counter_type.get(prev_type+"/"+type) +1));
                                } else {
                                    hash_counter_type.put(prev_type+"/"+type,new Integer(1));
                                }
                                break;
                            }
                        }
                        if (!deleted) {
                            System.err.println("DELETION FAILED1!" +docurl);
                            skipThisMarkable=true;
                        }
                    } else if (check.equals("delete2")) {
                        for (String key : markable.keySet()) {
                            //System.err.println(key + " (" +markable.get(key) + ") == " + tids + " ("+mid+")");
                            if (key.equals(tids) || markable.get(key) == mid) {
                                markable.remove(key);
                                markableType.remove(key);
                                for (String tid : key.split(" ")) {
                                    tokenIDAndMarkableType.remove(tid);
                                }
                                deleted = true;

                                skipThisMarkable=true;
                                //logger.debug("?? " +key + " CAT deletion " +mid + " " +type+" from " +owner + " ("+tids+")");
                                break;
                            }
                        }
                        if (!deleted) {
                            skipThisMarkable=true;

                            System.err.println("DELETION FAILED2! docurl: " + docurl +", tokenids: " + tids + " (mid: "+mid+")");
                        }

                    }

                } else {
                    skipThisMarkable=true;
                }
            }
        }
        if (!skipThisMarkable) {
            String[] tmp = {type, String.valueOf(mid), tids, owner};
            for (String tid : tids.split(" ")) {
                tokenIDAndMarkableType.remove(tid);
                tokenIDAndMarkableType.put(tid, tmp);
                //logger.debug("#tokenIDAndMarkableType (" + tid + ") ## " + type +" "+mid);
            }

            markable.put(tids, mid);
            markableType.put(tids,type);
        }
        return true;
    }

    //todo: procedura da controllare e testare!!
    //check the multiple user editing the same entity/document
    private boolean isLockedResource (String key, String sessionid) {
        String sessid = lockedResources.get(key);
        System.err.println("isLockedResource " + key +","+sessionid + " - " +lockedResources.containsKey(key) + " (sessid "+sessid+") locked.size="+lockedResources.size());
        if (sessid == null) {
            lockedResources.put(key, sessionid);
            lockedResources.put(sessionid, String.valueOf(System.currentTimeMillis()));
            return false;
        } else {
            System.err.println("isLockedResource " +sessid.equals(sessionid) + " ## " +
                    (System.currentTimeMillis() - Long.valueOf(lockedResources.get(sessid))));
            if (sessid.equals(sessionid)) {
                lockedResources.put(sessionid, String.valueOf(System.currentTimeMillis()));
                return false;
            } else {
                // ift he last update was up 10 minutes 
                if (System.currentTimeMillis() - Long.valueOf(lockedResources.get(sessid)) > 600000) {
                    System.err.println("isLockedResource ID " +key +" had old owner " +sessid+"  but the new is "+sessionid);
                    lockedResources.remove(sessid);
                    lockedResources.put(key, sessionid);
                    lockedResources.put(sessionid, String.valueOf(System.currentTimeMillis()));
                    return false;
                } else {
                    System.out.println("RESOURCE ID " +key +" is locked by " +sessid+ " "+(System.currentTimeMillis() - Long.valueOf(lockedResources.get(sessid)))/1000+"s ago)");
                }
            }
        }
        return true;
    }



    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        ServletContext application = getServletConfig().getServletContext();
        // config initialization
        //textpropath = application.getRealPath(servletConfig.getInitParameter("textproPath"));
        crawler.setUserAgent("HLTBot/1.3 (+http://hlt.fbk.eu/hltbot.html)");

        logger.info("CROMER is loading ... ");
        entityClasses.add("PER");
        entityClasses.add("LOC");
        entityClasses.add("ORG");
        entityClasses.add("PRO");
        entityClasses.add("FIN");
        eventClasses.add("SPEECH_COGNITIVE");
        eventClasses.add("GRAMMATICAL");
        eventClasses.add("OTHER");

        if (servletConfig.getInitParameter("indexPath") != null) {
            INDEXPATH = application.getRealPath(servletConfig.getInitParameter("indexPath"));
            if (INDEXPATH != null) {
                entities = new Entities();
                int numEntities = entities.indexLoader(INDEXPATH+File.separator+ENTITIESINDEXNAME, analyzer);
                System.out.println("Loaded " + numEntities + " entities");

                annotations = new CrossDocAnnotations();
                int numAnnotations = entities.indexLoader(INDEXPATH+File.separator+ANNOTATIONSINDEXNAME, analyzer);
                System.out.println("Loaded " + numAnnotations + " annotations");

                repositories = new Repositories();
                int numRepos = repositories.indexLoader(INDEXPATH);
                System.out.println("Loaded " + numRepos + " repositories");

            } else {
                logger.error("ERROR! Index path doesn't found or not valid ("+servletConfig.getInitParameter("indexPath")+").");
            }
        }
        if (servletConfig.getInitParameter("uploadPath") != null) {
            String indexpath = application.getRealPath(servletConfig.getInitParameter("uploadPath"));
            try {
                uploadarchives = UploadArchives.getInstance(indexpath);
                String[] uparchives = uploadarchives.getArchiveNames();
                for (String archivename : uparchives) {
                    File file = new File(indexpath, archivename + ".cgt");
                    file.delete();

                    file = new File(indexpath, archivename + ".set");
                    file.delete();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    static public boolean checkDBConnection () {
        try {
            return annotations.checkDBConnection();
        } catch (Exception e) {
            return false;
        }
    }

    static public String[] getIndexNames () {
        return repositories.getNames();

    }

    static public int getNumAnnotatedDocs (String entityid, String user) {
        return annotations.getNumDocs(entityid, user);

    }

    static public String getDocUrl (String index, String docid) throws IOException {
        return repositories.getDocUrl(index, docid);
    }

    // get doc ids for a query
    static public String[] getIndexSearch (String[] indexnames, String query, String user) throws IOException {
        if (query.startsWith("instanceid:")) {
            query = query.replaceFirst("instanceid:\\s*","");
            return annotations.getDocumentsByInstance(indexnames, query, user);
        }
        return repositories.searchIndex(indexnames, "token", query);
    }

    static public LinkedHashMap getInfoDocs (String indexname) throws IOException {
        return repositories.getInfoDocs(indexname);
    }

    static public int getLinkedInstancesFromDoc (String docid, String user) throws IOException {
        return annotations.getLinkedInstances(docid, user);
    }

    static public List getLinkedInstances (String mode) throws IOException {
        return annotations.getLinkedInstances(mode);
    }

    static public String[] getDocSentences (String indexname, String docid, String query) throws IOException {
        return repositories.getDocSentences(indexname, docid, query);
    }

    static public String getOriginalDoc(String index, String docid) throws IOException {
        Document doc = repositories.getDocument(index, docid);
        try {
            if (doc != null) {
                return doc.get("original");
            }
        } catch (Exception e) {
            logger.error("getOriginalDoc: " + index + ", " + docid + " " + e.getMessage());
        }
        return null;
    }

    static public String getXML( String index, String docid, String user) throws Exception {
        StringBuilder xml = new StringBuilder();
        StringBuilder relations = new StringBuilder();
        String docurl = WebController.getDocUrl(index, docid);
        logger.debug("## getXML: " +index+" "+ docurl+ " -- " + user);
        if (docurl == null)
            return null;
        xml.append("<Document doc_name=\""+docurl+"\" doc_id=\""+docid+"\">\n");
        // markable è un hash che ha come chiave i token_ids e valore l'm_id
                    /*
                    //HUMAN_PART_PER &#735;<u>lindsay lohan</u>#m4 -- 23 24,60 61,32,90,97,112,122,128,179,187
                    Map<String,String> CATannotation = WebController.getCATAnnotation(index, docid);
                    for (String key : CATannotation.keySet()) {
                        String[] tids = CATannotation.get(key).split(",");
                        for (String tid : tids)
                            markable.put(tid, key);
                        //out.println(key + " -- " + CATannotation.get(key));
                    } */


        Map<String, String> entityID2tokenIDs = new HashMap<String, String>();
        Map<String, String> linkedInstances = WebController.getAnnotatedTokensByInstances(index, docid, user);
        /*if (linkedInstances.size() > 0) {
            for (String tids : linkedInstances.keySet()) {
                logger.debug("CROMER " + " annotated tokens: "+tids);
            }
        } */
        //mantengo in una lista i tokens delle mentioni coreferite: queste verranno tolte delle relation create in CAT
        List<String> corefSourceIDs = new ArrayList<String>();
        List<String> corefSourceTokenIDs = new ArrayList<String>();
        Hashtable<String, String[]> tokenIDAndMarkableType = new Hashtable<String, String[]>();

        for (String tids : linkedInstances.keySet()) {
            corefSourceTokenIDs.add(tids);
            String[] entityIDs = linkedInstances.get(tids).split(" ");
            for (String entityID : entityIDs) {
                //System.err.println(">> " +entityID + "("+tids+") = " + entityID2tokenIDs.get(entityID));
                if (entityID2tokenIDs.containsKey(entityID)) {
                    entityID2tokenIDs.put(entityID, entityID2tokenIDs.get(entityID) + "," + tids);
                } else {
                    entityID2tokenIDs.put(entityID, tids);
                }
            }
        }

                    /*//105 106 107 == TIM15734406543689590
                    for (String key : tokenIDs2EntityID.keySet()) {
                        if (markable.containsKey(key))
                            out.println(key + " == " + tokenIDs2EntityID.get(key));
                    } */

        String text = getOriginalDoc(index, docid);

        /*
        // non usare fino a quando non vengono risolti i problemi in CAT altrimenti nelle funzioni di export
        //potrebbero verificarsi errori tipo: org.xml.sax.SAXParseException: The content of elements must consist of well-formed character data
        CATParser cat = new CATParser(text);
        int lastMID = cat.getLastMID();
        int lastRID = cat.getLastRID();
        Hashtable<String,Integer> markable = cat.getMarkables();
        List<String> targets = cat.getTargetList();
         */
        Hashtable<String,String> relationsFromCAT = new Hashtable<String,String>();
        Hashtable<String,Integer> markable = new Hashtable<String,Integer>();
        Hashtable<String,String> markableType = new Hashtable<String,String>();
        String curent_tids="", type="";
        int  lastMID=0, lastRID =0;
        List<String> targets = new ArrayList<String>();

        int lastMID_cromer=0, current_mid=0, source_relation=0;
        Matcher matcher;
        Hashtable<String, String> tag_desc_ids = new Hashtable<String, String>();
        Hashtable<String, String> relatedToMapping = new Hashtable<String, String>();
        boolean inMarkable = false, inRelation= false;
        int relationFromCAT = 0;

        //Hashtable<Integer, Integer> sources = new Hashtable<Integer, Integer>();

        //colleziono tutti gli id target per controllare se sono stati linkate tutte le chains (cioe` i TAG_DESCRIPTOR)
        //inoltre elimino i source già coreferiti in CROMER togliendo così relazioni che potrebbero essere state messe
        //in CAT ma senza il puntatore all'INSTANCEID
        String[] lines = text.split("\n");
        //get all relations
        String current_rid = "";
        for (String line : lines) {
            if (line.contains(" RELATED_TO=") || line.contains(" TAG_DESCRIPTOR=")) {
                matcher = mPattern.matcher(line);
                if (matcher.find()) {
                    tag_desc_ids.put(matcher.group(2), line);
                }
            } else {
                matcher = rPattern.matcher(line);
                if (matcher.find()) {
                    current_rid= matcher.group(1);
                } else {
                    if (line.contains("<source ")) {
                        matcher = mPattern.matcher(line);
                        if (matcher.find()) {
                            relationsFromCAT.put(matcher.group(2),current_rid);
                        }
                    }
                }
            }
        }

        int last_source_mid = 0;
        List<Integer> source_items = new ArrayList<Integer>();
        for (String line : lines) {
            matcher = mPattern.matcher(line);
            if (matcher.find()) {
                //logger.debug("LINE1: " + line);
                curent_tids = "";
                type = matcher.group(1);
                current_mid= Integer.valueOf(matcher.group(2));
                if (current_mid > lastMID) {
                    lastMID=current_mid;
                }
                if (line.contains("<source ")) {
                    if (!source_items.contains(current_mid))
                        source_items.add(current_mid);
                    last_source_mid = current_mid;
                    if (!corefSourceIDs.contains(String.valueOf(current_mid)))
                        source_relation++;
                } else if (line.contains("<target ")) {
                    //controllo che m_id del target non sia uguale all'ultimo source m_id
                    if (last_source_mid == current_mid)
                        source_relation = source_relation - 1;
                    if (inRelation && current_mid > 0 && source_relation > 1) {
                        //System.err.println("@@ TARGET: "+current_mid + " ("+source_relation+")");
                        //targets.add(String.valueOf(current_mid));
                    }
                    if (tag_desc_ids.containsKey(String.valueOf(current_mid))) {
                        for (int p=0; p<source_items.size(); p++) {
                            //logger.debug(source_items.get(p) + " ; " + current_mid + " " + tag_desc_ids.get(String.valueOf(current_mid)));
                            relationsFromCAT.put(String.valueOf(source_items.get(p)),tag_desc_ids.get(String.valueOf(current_mid)));
                        }
                    } else {
                        logger.debug("FILE: "+docurl+" TARGET m_id="+ current_mid + " doesn't find in TAG_DESCRIPTORS ");
                    }
                    source_items.clear();
                    inRelation=false;
                    current_mid=0;
                    source_relation=0;
                }

            } else {
                matcher = tPattern.matcher(line);
                if (matcher.find()) {
                    curent_tids += " " + matcher.group(1);
                } else {
                    matcher = rPattern.matcher(line);
                    if (matcher.find()) {
                        if (Integer.valueOf(matcher.group(1)) > lastRID) {
                            lastRID=Integer.valueOf(matcher.group(1));
                        }
                        current_mid=0;
                        source_relation=0;
                        inRelation=true;
                    }
                }
            }

            if (line.contains("</" + type + ">")) {
                curent_tids = curent_tids.trim();
                addMarkable(docurl, current_mid, curent_tids, type, "CAT", markable, markableType, tokenIDAndMarkableType, relationsFromCAT);


                if (corefSourceTokenIDs.contains(curent_tids))  {
                    //System.err.println("COREF m_id="+current_mid);
                    corefSourceIDs.add(String.valueOf(current_mid));
                }
                curent_tids="";
            }

            if (line.matches("^\\s*</.+")) {
                inRelation=false;
                current_mid=0;
                source_relation=0;
            }
        }


        //manage the inconsistency
        if (linkedInstances.size() > 0) {
            for (String tids : linkedInstances.keySet()) {
                for (String entityID : linkedInstances.get(tids).split(" ")) {
                    tids = tids.trim();
                    Hashtable instanceInfo = WebController.getInstance(entityID);
                    if (instanceInfo!= null && instanceInfo.containsKey("class")) {
                        String mType = (String) instanceInfo.get("class");
                        //considero i token span diversi da quelli creati in CAT o che hanno un type diverso
                        if (!markable.containsKey(tids) || !markableType.get(tids).equalsIgnoreCase(mType)) {
                            if (markableType.containsKey(tids) && !markableType.get(tids).equalsIgnoreCase(mType)) {
                                logger.debug("CROMER missed annotation with existing tokens but with different type: " +tids + " ("+docid+")");
                            }

                            lastMID++;
                            addMarkable(docurl, lastMID, tids, mType, "CROMER", markable, markableType, tokenIDAndMarkableType, relationsFromCAT);
                        }
                    }
                }
            }
        }
        List<Integer> checkedMarkable = new ArrayList<Integer>();
        List<Integer> alreadyMarked = new ArrayList<Integer>();
        for (String key : markable.keySet()) {
            checkedMarkable.add(markable.get(key));
        }

        logger.debug("checkedMarkable: " + checkedMarkable +"\ntargets: "+targets+"\n"+docurl+" markable: " + markable.size()+", markableType: " + markableType.size() + ", tokenIDAndMarkableType:"+tokenIDAndMarkableType.size());


        /*for (String target : targets) {
            System.err.println("@@ "+ target+"<br>");
        }*/
        //logger.debug("PRELIMINARY PARSER: TARGETs:" +targets.toString()+ ", lastMID:" + lastMID +"="+cat.getLastMID() + ", lastRID:" + lastRID +"="+cat.getLastRID()+ ", markable.size()="+markable.size());

        source_relation=0;
        String relSection = "";
        current_mid=0;
        for (String line : lines) {
            matcher = mPattern.matcher(line);
            if (matcher.find()) {
                current_mid=Integer.valueOf(matcher.group(2));
            }
            if (line.contains("</Markables>") ||  line.contains("<Markables />")) {
                //aggiungo i tag_descriptor e le menzioni annotate in CROMER (se esistono)
                if (linkedInstances.size() > 0) {
                    for (String tids : linkedInstances.keySet()) {
                        for (String entityID : linkedInstances.get(tids).split(" ")) {
                            tids = tids.trim();
                            Hashtable instanceInfo = WebController.getInstance(entityID);
                            if (instanceInfo!= null && instanceInfo.containsKey("class")) {
                                //String mType = (String) instanceInfo.get("class");
                                String mType = (String) instanceInfo.get("type");
                                //considero i token span diversi da quelli creati in CAT o che hanno un type diverso
                                if (markable.containsKey(tids) && !alreadyMarked.contains(markable.get(tids))) {

                                    //xml.append("<"+mType+" m_id=\""+markable.get(tids)+"\" note=\"byCROMER\" >\n");
                                    xml.append("<"+mType+"_MENTION m_id=\""+markable.get(tids)+"\" note=\"byCROMER\" >\n");
                                    String[] tokenIDs = tids.split(" ");
                                    for (String tid : tokenIDs) {
                                        xml.append("  <token_anchor t_id=\""+tid+"\"/>\n");
                                    }
                                    //xml.append("</"+mType+">\n");
                                    xml.append("</"+mType+"_MENTION>\n");
                                }
                            }
                            break;
                        }
                    }
                    xml.append("\n\n");
                }
                int entcount = 0;
                lastMID_cromer =  lastMID;
                for (String entityid : entityID2tokenIDs.keySet()) {
                    Hashtable<String, String> instanceInfo = WebController.getInstance(entityid);
                    String external_ref = instanceInfo.get("link");
                    if (external_ref != null && external_ref.length() > 0) {
                        external_ref = " external_ref=\""+external_ref+"\"";
                    } else {
                        external_ref = "";
                    }
                    entcount++;
                    if (entityClasses.contains(instanceInfo.get("class"))) {
                        tag_desc_ids.put(String.valueOf(lastMID + entcount), "<ENTITY m_id=\""+
                                String.valueOf(lastMID + entcount) + "\" RELATED_TO=\"\" TAG_DESCRIPTOR=\""+
                                instanceInfo.get("name")+"\" ent_type=\""+instanceInfo.get("class")+"\" instance_id=\""+entityid+"\""+external_ref+" />");
                    } else if (eventClasses.contains(instanceInfo.get("class"))) {
                        tag_desc_ids.put(String.valueOf(lastMID + entcount), "<EVENT m_id=\""+
                                String.valueOf(lastMID + entcount) + "\" RELATED_TO=\"\" TAG_DESCRIPTOR=\""+
                                instanceInfo.get("name")+"\" class=\""+instanceInfo.get("class")+"\" instance_id=\""+entityid+"\""+external_ref+" />");
                    } else {
                        tag_desc_ids.put(String.valueOf(lastMID + entcount), "<"+instanceInfo.get("class")+" m_id=\""+
                                String.valueOf(lastMID + entcount) + "\" RELATED_TO=\"\" TAG_DESCRIPTOR=\""+
                                instanceInfo.get("name")+"\" instance_id=\""+entityid+"\" />");
                    }
                    String[] tids = entityID2tokenIDs.get(entityid).split(",");
                    for (String tid : tids) {
                        corefSourceIDs.add(String.valueOf(markable.get(tid)));
                    }
                }

                inMarkable = false;
                relations.append("<Relations>\n");

                //break;
            } else if (line.contains("<token t_id=")) {
                if (current_mid != 0)
                    xml.append("  ");
                if (current_mid == 0 || checkedMarkable.contains(current_mid))
                    xml.append(line.trim()).append("\n");
            } else if (line.contains("<Markables>")) {
                inMarkable = true;
                xml.append(line).append("\n");
            } else if (inMarkable) {
                if (line.contains(" RELATED_TO=") || line.contains(" TAG_DESCRIPTOR=")) {
                    matcher = relatedPattern.matcher(line);
                    if (matcher.find()) {
                        //logger.debug("RELATED_TO PRESENT: " +matcher.group(2) + " "+ line);
                        relatedToMapping.put(matcher.group(2), matcher.group(1));

                    } //else {
                    //logger.error("TAG_DESCRIPTOR PARSER ERROR: " +docurl + " " + line);
                    //}
                    matcher = mPattern.matcher(line);
                    if (matcher.find()) {
                        String tagid = matcher.group(2);
                        if (!tag_desc_ids.containsKey(tagid))
                            tag_desc_ids.put(tagid, line);
                    }
                } else {
                    if (checkedMarkable.contains(current_mid)) {
                        if (line.contains("<token_anchor")) {
                            xml.append("  ");
                        }
                        xml.append(line.trim()).append("\n");
                        if (!alreadyMarked.contains(current_mid))
                            alreadyMarked.add(current_mid);
                    }
                }

            } else if (line.contains("</Relations>") ||  line.contains("<Relations />")) {
                int entcount = 0;
                for (String entityid : entityID2tokenIDs.keySet()) {
                    lastRID++;
                    relations.append("<COREFERENCE r_id=\""+lastRID+"\" note=\"").append(entityid).append("\" >\n");
                    //System.err.println("%% "+entityid+ " :: "+entityID2tokenIDs.get(entityid));
                    String[] tids = entityID2tokenIDs.get(entityid).split(",");
                    for (String tid : tids) {
                        if (markable.get(tid.trim()) != null) {
                            relations.append("  <source m_id=\"").append(markable.get(tid.trim())).append("\" />\n");
                        }
                    }
                    entcount++;
                    //if (lastMID_cromer != lastMID) {
                    //    logger.debug("LAST MID error on previuos export: " +docurl + " [r_id=" + lastRID + "] " +line);
                    //}
                    relations.append("  <target m_id=\"").append(lastMID_cromer+entcount).append("\" />\n");
                    targets.add(String.valueOf(lastMID_cromer+entcount));
                    //out.println(entityid +" -- " +tid+ " => " +markable.get(tid));
                    relations.append("</COREFERENCE>\n");
                }
                inRelation = false;
            } else if (line.contains("<Relations>")) {
                inRelation = true;
                inMarkable = false;
                current_mid=0;
                continue;
            } else if (inRelation) {
                //correggo errori di export incompleto da CAT come:
                // </DESCRIBES>
                // < r_id="37024" >
                // </>
                if (line.contains("< r_id") || line.contains("</>")) {
                    source_relation=0;
                    continue;
                }
                //System.err.println("LINE: "+ current_mid+", "+source_relation+ " -- " + line);

                if (line.matches("^\\s*</.+")) {
                    if (line.contains("</DESCRIBES>")) {
                        if (source_relation > 1 && relSection.contains("<target ")) {
                            if (tag_desc_ids.containsKey(String.valueOf(current_mid))) {
                                relations.append(relSection).append(line).append("\n");
                                targets.add(String.valueOf(current_mid));
                                relationFromCAT++;
                            } else {
                                logger.debug("FILE: "+docurl+" TARGET m_id="+ current_mid + " is NOT VALID");
                            }
                        }
                    } else {
                        if (relSection.contains("<target ")) {
                            relations.append(relSection).append(line).append("\n");
                            if (tag_desc_ids.containsKey(String.valueOf(current_mid))) {
                                targets.add(String.valueOf(current_mid));
                                relationFromCAT++;
                            } else
                                logger.debug("FILE: "+docurl+" TARGET m_id="+ current_mid + " is NOT VALID");
                        }
                    }

                    /*if (source_relation==0) {
                        logger.debug("NO SOURCE, ONE TARGET: " + docurl + " " +line);
                    } else {
                        logger.debug("MISSED TARGET (with tokens): " + docurl + " <source m_id=\""+current_mid+"\" />");
                    } */
                    relSection="";
                    source_relation=0;
                    current_mid=0;
                    last_source_mid=0;
                    continue;
                }
                if (tag_desc_ids.containsKey(String.valueOf(current_mid))) {
                    line = "<target m_id=\""+current_mid+"\" />";
                }
                //System.err.println("REL: [[["+ relSection+"]]]\n"+line);

                if (line.contains("<target m_id=")) {
                    if (source_relation == 0) {
                        //logger.debug("NO SOURCE, ONE TARGET or SOURCE < 1: " + docurl + " " +line);
                        continue;
                    }
                    matcher = mPattern.matcher(line);
                    if (matcher.find()) {
                        current_mid = Integer.valueOf(matcher.group(2));
                        if (last_source_mid == current_mid)
                            source_relation = source_relation -1;
                        if (relatedToMapping.containsKey(matcher.group(2))) {
                            logger.debug("UPDATED RELATED_TO: " + docurl + " " +line);
                            current_mid = Integer.valueOf(relatedToMapping.get(matcher.group(2)));

                            line = "<target m_id=\""+current_mid+"\" />";
                        }
                    }
                    //source_relation=0;
                } else if (line.contains("<source m_id=")) {
                    last_source_mid = current_mid;
                    if (!corefSourceIDs.contains(String.valueOf(current_mid)) && !tag_desc_ids.containsKey(String.valueOf(current_mid))) {
                        //corefSourceIDs.add(String.valueOf(current_mid));
                        if (!checkedMarkable.contains(current_mid))  {
                            logger.error(docurl+ " MISSED SOURCE "+current_mid);
                            continue;
                        }
                        source_relation++;

                    } else {
                        continue;
                    }
                }
                if (line.length() > 0)
                    relSection += line+"\n";
            }
        }

        //add tag_descriptors and relations
        for (String tagid : tag_desc_ids.keySet()) {
            if (targets.contains(tagid))
                xml.append(tag_desc_ids.get(tagid)).append("\n");
        }
        xml.append("</Markables>").append("\n\n");

        xml.append(relations.toString()).append("\n</Relations>\n").append("</Document>\n");

        //check if XML is well-formed
        try{
            new CATParser(xml.toString());
        } catch (Exception e) {
            logger.error("# XML parser error "+index+ " " + docid + " ("+e.getMessage()+")\n");
            e.printStackTrace();
        }

        return xml.toString();
    }

    static public LinkedHashMap<String, Hashtable<String, String>> getStats (String user) {
        return annotations.getStats(user);
    }

    static public String exportAnnotations (String user, String mode, String format) {
        return annotations.exportAnnotations(user, mode, format, repositories);
    }

    static public HashMap<String,String> getRelationOccurences (String index, String docid, String user) throws IOException {
        Document doc = repositories.getDocument(index, docid);
        LinkedHashMap<String,String> relations = new LinkedHashMap<String,String>();
        try {
            if (doc != null) {
                //String xml = getXML(index, docid, user);
                String xml = getOriginalDoc(index, docid);
                //String xml = doc.get("original");
                if (xml!=null) {
                    try {
                        CATParser cat = new CATParser(xml);
                        //for (String key : cat.getMarkables().keySet()) {
                        //    logger.info(">> " + key + " :: " +cat.getMarkables().get(key));
                        //}
                        String[] catlines = xml.split("\n");
                        String rid="", reltype="", relitem="", semrole="";
                        for (String line : catlines) {
                            if (line.contains("<HAS_PARTICIPANT ")) {
                                reltype="";
                                relitem="";
                                semrole=line.replaceFirst(".* sem_role=\"","").replaceFirst("\".*", "");
                                rid=line.replaceFirst(".* r_id=\"","").replaceFirst("\".*", "");
                            } else if (semrole.length() > 0) {
                                if (line.contains("<source ") || line.contains("<target ")) {
                                    Matcher matcher = mPattern.matcher(line);
                                    if (matcher.find()) {
                                        String tokenids = cat.getMarkableTokenIDs(Integer.valueOf(matcher.group(2)));
                                        if (line.contains("<target ")) {
                                            reltype += ","+ tokenids;
                                            //System.err.println("TARGET: "+Integer.valueOf(matcher.group(2))+ " ["+tokenids+ "]");
                                            relitem += " <button onclick=\"javascript:setRole(this,'"+rid+"','"+reltype+"');\">"+semrole+"</button> "+ cat.getTokens(tokenids.split(" "));
                                            relations.put(reltype, relitem);
                                            semrole = "";
                                            //logger.debug(reltype + " -> "+ relitem);
                                        } else {
                                            //System.err.println("SOURCE: "+Integer.valueOf(matcher.group(2)) + " ["+tokenids+"]");
                                            relitem = cat.getTokens(tokenids.split(" "));
                                            reltype = tokenids;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("# XML parser error "+index+ " " + docid + " ("+e.getMessage()+")\n");
                        e.printStackTrace();
                    }

                }
            }
        } catch (Exception e) {
            logger.error("Parsing problem in doc " +docid + " ("+index+"). " + e.getMessage());
            // e.printStackTrace();
        }

        return relations;
    }

    static public HashMap<String,String> getHasParticipant (String index, String docid, String user) throws IOException {
        Document doc = repositories.getDocument(index, docid);
        LinkedHashMap<String,String> relations = new LinkedHashMap<String,String>();
        try {
            if (doc != null) {
                //String xml = getXML(index, docid, user);
                String xml = getOriginalDoc(index, docid);
                //String xml = doc.get("original");
                if (xml!=null) {
                    try {
                        CATParser cat = new CATParser(xml);
                        //for (String key : cat.getMarkables().keySet()) {
                        //    logger.info(">> " + key + " :: " +cat.getMarkables().get(key));
                        //}
                        String[] catlines = xml.split("\n");
                        String rid="", reltype="", relitem="", semrole="";
                        for (String line : catlines) {
                            if (line.contains("<HAS_PARTICIPANT ")) {
                                reltype="";
                                relitem="";
                                semrole=line.replaceFirst(".* sem_role=\"","").replaceFirst("\".*", "");
                                rid=line.replaceFirst(".* r_id=\"","").replaceFirst("\".*", "");
                            } else if (semrole.length() > 0) {
                                if (line.contains("<source ") || line.contains("<target ")) {
                                    Matcher matcher = mPattern.matcher(line);
                                    if (matcher.find()) {
                                        String tokenids = cat.getMarkableTokenIDs(Integer.valueOf(matcher.group(2)));
                                        if (line.contains("<target ")) {
                                            reltype += ","+ tokenids;
                                            //System.err.println("TARGET: "+Integer.valueOf(matcher.group(2))+ " ["+tokenids+ "]");
                                            relitem += " <button>"+semrole+"</button> "+ cat.getTokens(tokenids.split(" "));
                                            relations.put(reltype, relitem);
                                            semrole = "";
                                            //logger.debug(reltype + " -> "+ relitem);
                                        } else {
                                            //System.err.println("SOURCE: "+Integer.valueOf(matcher.group(2)) + " ["+tokenids+"]");
                                            relitem = cat.getTokens(tokenids.split(" "));
                                            reltype = tokenids;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("# XML parser error "+index+ " " + docid + " ("+e.getMessage()+")\n");
                        e.printStackTrace();
                    }

                }
            }
        } catch (Exception e) {
            logger.error("Parsing problem in doc " +docid + " ("+index+"). " + e.getMessage());
            // e.printStackTrace();
        }

        return relations;
    }

    static public HashMap<String,String> getCATRelations(String index, String docid, String user) throws IOException {
        Document doc = repositories.getDocument(index, docid);
        HashMap<String,String> relations = new HashMap<String,String>();
        try {
            if (doc != null) {
                //String xml = getXML(index, docid, user);
                String xml = getOriginalDoc(index, docid);
                //String xml = doc.get("original");
                if (xml!=null) {
                    try {
                        CATParser cat = new CATParser(xml);
                        //for (String key : cat.getMarkables().keySet()) {
                        //    logger.info(">> " + key + " :: " +cat.getMarkables().get(key));
                        //}
                        String[] catlines = xml.split("\n");
                        String reltype="";
                        String relitem="";
                        for (String line : catlines) {
                            if (line.contains("</TLINK>")) {
                                reltype="";
                                relitem="";
                            } else if (line.contains("<TLINK ")) {
                                reltype=line.replaceFirst(".*reltype=\"","").replaceFirst("\".*", "");
                            } else if (reltype.length() > 0) {
                                if (line.contains("<source ") || line.contains("<target ")) {
                                    Matcher matcher = mPattern.matcher(line);
                                    if (matcher.find()) {
                                        String tokenids = cat.getMarkableTokenIDs(Integer.valueOf(matcher.group(2)));
                                        if (line.contains("<target ")) {
                                            reltype += ","+ tokenids;
                                            //System.err.println("TARGET: "+Integer.valueOf(matcher.group(2))+ " ["+tokenids+ "]");
                                            relitem += " "+ cat.getTokens(tokenids.split(" "));
                                            relations.put(reltype, relitem);
                                            //logger.debug(reltype + " -> "+ relitem);
                                        } else {
                                            //System.err.println("SOURCE: "+Integer.valueOf(matcher.group(2)) + " ["+tokenids+"]");
                                            relitem = cat.getTokens(tokenids.split(" ")) + " "+reltype;
                                            reltype = tokenids;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("# XML parser error "+index+ " " + docid + " ("+e.getMessage()+")\n");
                        e.printStackTrace();
                    }

                }
            }
        } catch (Exception e) {
            logger.error("Parsing problem in doc " +docid + " ("+index+"). " + e.getMessage());
            // e.printStackTrace();
        }

        return relations;
    }

    static public HashMap<String,String> getCATAnnotation(String index, String docid, String user) throws IOException {
        Document doc = repositories.getDocument(index, docid);
        HashMap<String,String> entities = new HashMap<String,String>();
        try {

            if (doc != null) {
                String news = getXML(index, docid, user);
                //String news = doc.get("original");
                if (news!=null) {
                    String[] catlines = news.split("\n");
                    HashMap<String,String> ids = new HashMap<String,String>();
                    List<String> markableInChain = new ArrayList<String>();
                    //int tokenid = 1;
                    Pattern tPattern = Pattern.compile(".* t_id=\"(\\d+)\"");
                    Pattern mPattern = Pattern.compile(".*<([^ ]+) m_id=\"(\\d+)\".*");

                    boolean foundMarkable = false;
                    boolean foundRelation = false;
                    String tokenIDs = "";
                    String lastentry = "";
                    String current_mID="", current_mTYPE="";
                    Matcher matcher;
                    //int tokenid= 1;
                    String mentionType = "";
                    for (String line : catlines) {
                        if (line.matches("<token t_id=.*</token>")) {
                            String token = line.replaceFirst("<\\/token>","").replaceFirst("<token t_id=.+>","");
                            ids.put("t"+line.replaceFirst(".*t_id=\"","").replaceFirst("\".*$",""), token);
                            //tokenid++;
                        } else if (line.contains("<Markables>")) {
                            foundMarkable = true;
                        } else if (line.contains("</Markables>") ||  line.contains("<Markables />")) {
                            foundMarkable = false;
                        } else if (line.contains("<Relations>")) {
                            //foundRelation = true;
                            mentionType = "";
                        } else if (line.contains("</Relations>") ||  line.contains("<Relations />")) {
                            //foundRelation = false;
                        } else if (line.contains("<DESCRIBES ") || line.contains("<REFERS_TO ")) {
                            foundRelation=true;
                        } else if (line.contains("</DESCRIBES>") || line.contains("</REFERS_TO>")) {
                            foundRelation=false;
                            if (!tokenIDs.equals("")) {
                                //logger.debug("["+ids.containsKey(current_mID) + "] " +current_mID + " -- "+ lastentry+ " => " + tokenIDs);
                                //CATMarkable catm = new CATMarkable(current_mID, current_mTYPE, lastentry);
                                entities.put(lastentry.replaceFirst("^[^ ]+ ",lastentry.replaceFirst(" .*","")+" ")+"#"+current_mID,tokenIDs);
                            }
                            tokenIDs = "";
                        } else if (foundMarkable) {
                            matcher = mPattern.matcher(line);
                            if (matcher.find()) {
                                //logger.debug("LINE1: " + line);

                                current_mID = "m"+matcher.group(2);
                                current_mTYPE = matcher.group(1);
                                if (line.contains("RELATED_TO=\"")) {
                                    //logger.debug(current_mID + " "+ current_mTYPE + " " + line.replaceFirst(".*TAG_DESCRIPTOR=\"","").replaceFirst("\".*",""));
                                    String mentionext = line.replaceFirst(".*TAG_DESCRIPTOR=\"", "").replaceFirst("\".*", "");
                                    if (!mentionext.equals("add_chain_name"))
                                        mentionext = mentionext.replaceAll("_"," ");
                                    if (line.contains(" ent_type=")) {
                                        current_mTYPE = line.replaceFirst(".*<","").replaceFirst(" .*","")
                                                +"/"+line.replaceFirst(".* ent_type=\"","").replaceFirst("\".*","");
                                    } else if (line.contains(" class=")) {
                                        current_mTYPE = line.replaceFirst(".*<","").replaceFirst(" .*","")
                                                +"/"+line.replaceFirst(".* class=\"","").replaceFirst("\".*","");
                                    }
                                    //logger.debug(current_mID+", "+current_mTYPE + " &#735;<u>" + mentionext +"</u>");

                                    ids.put(current_mID,current_mTYPE + " &#735;<u>" + mentionext +"</u>");

                                }
                            } else {
                                //logger.debug("LINE2: " + line);

                                matcher = tPattern.matcher(line);
                                if (matcher.find()) {
                                    if (ids.containsKey(current_mID)) {
                                        //logger.debug(current_mID + " -> " +ids.get(current_mID) + " " +matcher.group(1));
                                        ids.put(current_mID, ids.get(current_mID) + " " + matcher.group(1));
                                    } else {
                                        //logger.debug(current_mID + " -> " +current_mTYPE + " " +matcher.group(1));
                                        ids.put(current_mID, current_mTYPE + " " +matcher.group(1));
                                    }
                                }
                            }

                        } else if (foundRelation) {
                            matcher = mPattern.matcher(line);
                            if (matcher.find()) {
                                current_mID = "m"+matcher.group(2);
                                current_mTYPE = matcher.group(1);
                                if (ids.containsKey(current_mID)) {
                                    lastentry = ids.get(current_mID);
                                    //logger.debug("!"+current_mTYPE + " " +current_mID + " -- "+ lastentry);
                                    if (current_mTYPE.equals("source")) {
                                        mentionType = lastentry.replaceFirst(" .*","");
                                        if (tokenIDs.length() > 0) {
                                            tokenIDs += ",";
                                        }
                                        tokenIDs += lastentry.replaceFirst("^[^ ]+ ","");

                                    } else if (current_mTYPE.equals("target")) {
                                        if (!tokenIDs.equals("")) {
                                            //logger.debug("!"+lastentry.replaceFirst("^[^ ]+ ",mentionType+" ")+"#"+current_mID + " # " +current_mTYPE + " " +current_mID + " -- "+ lastentry+ " => " + tokenIDs);
                                            //if (lastentry.contains("<u>")) {
                                            if (entities.containsKey(lastentry+"#"+current_mID)) {
                                                entities.put(lastentry+"#"+current_mID,entities.get(lastentry + "#" + current_mID)+ ","+tokenIDs);
                                            } else {
                                                entities.put(lastentry+"#"+current_mID,tokenIDs);
                                            }
                                            //entities.put(lastentry.replaceFirst("^[^ ]+ ",mentionType+" ")+"#"+current_mID,tokenIDs);
                                            /*} else {
                                                String mention = "";
                                                String[] tids =  tokenIDs.split(" ");
                                                for (String tid : tids) {
                                                    if (ids.containsKey("t"+tid))
                                                        mention += ids.get("t"+tid) + " ";
                                                    else {
                                                        mention = null;
                                                        break;
                                                    }
                                                }
                                                logger.debug("!"+mentionType+" <i>"+mention.trim()+"</i>#"+current_mID+ " --- " + tokenIDs);
                                                if (mention != null && !entities.containsKey(mentionType+" <i>"+mention.trim()+"</i>#"+current_mID)) {
                                                    entities.put(mentionType+" <i>"+mention.trim()+"</i>#"+current_mID,tokenIDs);
                                                }
                                            } */

                                        }
                                        tokenIDs = "";
                                    }
                                    //ids.remove(current_mID);
                                    //logger.debug("markableInChain.add(" + current_mID+")");
                                    markableInChain.add(current_mID);
                                } else {
                                    logger.error("getCATAnnotation: " + docid + " the " + current_mID + " is missed");
                                }
                            }

                        }
                    }

                    //add all remained markable
                    for (String markable : ids.keySet()) {
                        if (markable.startsWith("m")) {
                            //select the rest of markables that are not involved in the chain
                            if (!markableInChain.contains(markable)) {
                                String mention = "";
                                String[] tids =  ids.get(markable).replaceFirst("^[^ ]+ ","").split(" ");
                                for (String tid : tids) {
                                    if (ids.containsKey("t"+tid))
                                        mention += ids.get("t"+tid) + " ";
                                    else {
                                        mention = null;
                                        break;
                                    }
                                }
                                //logger.debug("##" +markable + " -> " +ids.get(markable) + " " +mention);
                                //System.err.println("##" +markable + " -> " +ids.get(markable) + " " +mention + " -- " +ids.get(markable).replaceFirst("^[^ ]+ ",""));
                                if (mention != null) {
                                    entities.put(ids.get(markable).replaceAll(" .+","")+" <i>"+mention.trim()+"</i>#"+markable, ids.get(markable).replaceFirst("^[^ ]+ ",""));
                                }
                            } //else {
                            //  logger.debug("REMAIN: " + markable);
                            //}
                        }
                    }

                    Hashtable<String,String> keystodelete = new Hashtable<String,String>();
                    for (String k : entities.keySet()) {
                        //logger.debug("> "+k + ": " + entities.get(k));
                        if (k.contains("add_chain_name")) {
                            String mentionext = "";
                            //get tokens
                            String[] tokenranges = entities.get(k).split(",");
                            for (String tokenids : tokenranges) {
                                String mentiontmp = "";
                                String[] tids =  tokenids.split(" ");
                                for (String t_id : tids) {
                                    if (ids.get("t"+t_id) == null)
                                        logger.error("getCATAnnotation: The token t"+t_id+ " in "+index+"/"+docid + " is missed (" +k +")");
                                    else
                                        mentiontmp += ids.get("t"+t_id) + " ";
                                }
                                if (mentiontmp.length() > mentionext.length())
                                    mentionext = mentiontmp.trim();
                            }
                            String[] splitted_k = k.split("add_chain_name");
                            keystodelete.put(k, splitted_k[0] + mentionext + splitted_k[1]);
                        }
                    }
                    for (String k : keystodelete.keySet()) {
                        entities.put(keystodelete.get(k), entities.get(k));
                        entities.remove(k);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Parsing problem in doc " +docid + " ("+index+"). " + e.getMessage());
            // e.printStackTrace();
        }

        return entities;
    }

    static public String[] getInstanceDocSentences (String index, String docid, String instanceid, String user) throws IOException {
        String[] sentences = new String[0];
        String[] tokenids = annotations.getAnnotation(instanceid, index, docid, user);
        try {
            List<String> result = new ArrayList<String>();
            HashMap<Integer,Integer> highlightIds = new HashMap<Integer,Integer>();
            for (String item : tokenids) {
                String[] ids = item.split(" ");
                if (ids.length == 1) {
                    highlightIds.put(Integer.valueOf(ids[0]),0);
                } else {
                    for (int i=0; i<ids.length; i++) {
                        highlightIds.put(Integer.valueOf(ids[i]),0);
                        /*if (i==0) {
                            highlightIds.put(Integer.valueOf(ids[i]),1);
                        }
                        if (i==ids.length-1){
                            highlightIds.put(Integer.valueOf(ids[i]),-1);
                        } */
                    }
                }
            }
            if (tokenids != null) {
                Document doc = repositories.getDocument(index, docid);
                if (doc != null) {
                    String[] txplines = doc.get("txpfile").split("\n");

                    boolean found = false;
                    StringBuilder sentence = new StringBuilder();

                    int currentPosition = 0;
                    int currentTokenID = 0;
                    String space;
                    String[] items;
                    int sentenceIDs = 1;
                    sentence.append("<div class='sentcounter'><i>s"+sentenceIDs+"</i>.&nbsp;</div>&nbsp;<div style='display: inline'>");

                    for (String txpline : txplines) {
                        if (txpline.startsWith("# "))
                            continue;
                        else if (txpline.length() == 0) {
                            if (found) {
                                result.add(sentence.toString()+"</div>");
                            }
                            found = false;
                            sentence.setLength(0);
                            sentenceIDs++;
                            sentence.append("<div class='sentcounter'><i>s"+sentenceIDs+".&nbsp;</i></div><div style='display: inline'>");

                            continue;
                        }
                        currentTokenID++;
                        items = txpline.split("\t");
                        space = "";
                        if (currentPosition != Integer.valueOf(items[1])) {
                            space = " ";
                        }

                        Integer toHighlight = highlightIds.get(currentTokenID);
                        if (toHighlight != null) {
                            found = true;
                            if (toHighlight == 0)
                                sentence.append("<font class=highlgh>").append(space).append(items[0]).append("</font>");
                            /*else if (toHighlight == 1)
                                sentence.append("<font class=highlgh>").append(space).append(items[0]);
                            else if (toHighlight == -1)
                                sentence.append(space).append(items[0]).append("</font>");
                             */
                        } else {
                            sentence.append(space).append(items[0]);

                        }
                        currentPosition = Integer.valueOf(items[1]) + items[0].length();
                    }
                    //add last sentence if something is found
                    if (found) {
                        result.add(sentence.toString()+"</div>");
                    }
                }
            }

            sentences = new String[result.size()];
            for (int i=0; i<result.size(); i++) {
                sentences[i] = result.get(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sentences;
    }


    static public List<String> getDivTokens (String indexname, String docid, String query) throws IOException {
        return repositories.getDivTokens(indexname, docid, query);
    }

    static public String[] getIndexDocs () throws IOException {
        String[] names = getIndexNames();
        String[] docs = new String[names.length];
        int i=0;
        for (String name : names) {
            docs[i] = String.valueOf(repositories.getIndexDocs(name));
            i++;
        }
        return docs;

    }

    static public int getNumInstances() throws IOException {
        return entities.size();
    }

    static public String addInstance (String name, String classname, String type,
                                      String descr, String link, String comment,
                                      String time, String beginterval, String endinterval, String haspart) {
        try {
            if (classname != null) {
                if (classname.equals(classname.toLowerCase()))
                    classname = classname.toUpperCase();
            } else {
                return null;
            }

            String id = classname;
            if (id.length() > 3) {
                id = id.substring(0,3);
                id = id.toUpperCase();
            }
            id += String.valueOf(System.nanoTime());

            Document doc = new Document();
            Field f = new Field("id", id,
                    Field.Store.YES,
                    Field.Index.NOT_ANALYZED,
                    Field.TermVector.NO);
            doc.add(f);
            if (type != null) {
                if (type.equals(type.toLowerCase()))
                    type = type.toUpperCase();
                f= new Field("type", type,
                        Field.Store.YES,
                        Field.Index.NOT_ANALYZED,
                        Field.TermVector.NO);
                doc.add(f);
            } else
                return null;

            f= new Field("class", classname.toUpperCase(),
                    Field.Store.YES,
                    Field.Index.NOT_ANALYZED,
                    Field.TermVector.NO);
            doc.add(f);

            f= new Field("name", name,
                    Field.Store.YES,
                    Field.Index.NO,
                    Field.TermVector.NO);
            doc.add(f);
            f= new Field("descr", descr,
                    Field.Store.YES,
                    Field.Index.NO,
                    Field.TermVector.NO);
            doc.add(f);
            f= new Field("descnorm", "_"+NormalizedWhitespaceAnalyzer.normalize(normText.normalize(name+" "+descr)).replaceAll(" "," _"),
                    Field.Store.NO,
                    Field.Index.ANALYZED,
                    Field.TermVector.NO);
            doc.add(f);

            if (link == null)
                link = "";
            f= new Field("link", link,
                    Field.Store.YES,
                    Field.Index.NO,
                    Field.TermVector.NO);
            doc.add(f);
            if (comment == null)
                comment = "";
            f= new Field("comment", comment,
                    Field.Store.YES,
                    Field.Index.NO,
                    Field.TermVector.NO);
            doc.add(f);

            if (type.equals("EVENT")) {
                if (time == null)
                    time = "";
                f= new Field("time", time.trim(),
                        Field.Store.YES,
                        Field.Index.NO,
                        Field.TermVector.NO);
                doc.add(f);
                if (beginterval == null)
                    beginterval = "";
                f= new Field("beginterval", beginterval.trim(),
                        Field.Store.YES,
                        Field.Index.NO,
                        Field.TermVector.NO);
                doc.add(f);
                if (endinterval == null)
                    endinterval = "";
                f= new Field("endinterval", endinterval.trim(),
                        Field.Store.YES,
                        Field.Index.NO,
                        Field.TermVector.NO);
                doc.add(f);
                if (haspart == null)
                    haspart = "";
                if (haspart.trim().length() > 0) {
                    f= new Field("haspart", haspart.trim(),
                            Field.Store.YES,
                            Field.Index.NO,
                            Field.TermVector.NO);
                    doc.add(f);
                }
            }
            f= new Field("numdocs", "0",
                    Field.Store.YES,
                    Field.Index.NO,
                    Field.TermVector.NO);
            doc.add(f);

            f = new Field("lastmodified",
                    sdf.format(new Date()),
                    Field.Store.YES,
                    Field.Index.NOT_ANALYZED,
                    Field.TermVector.NO);
            doc.add(f);

            /*
            f= new Field("tokenids", "",
                    Field.Store.YES,
                    Field.Index.NO,
                    Field.TermVector.NO);
            doc.add(f);
             */

            logger.info("SAVED NEW INSTANCE >> " + type + "/" + classname + "/" + id + "\t" + link +"\t"+name+"\t"+descr);

            if (entities.addInstance(doc))
                return id;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static public boolean updateInstanceClass (String id, String classname) {
        try {
            Document doc = entities.getInstance(id);
            Field f;
            doc.removeField("class");
            f= new Field("class", classname.toUpperCase(),
                    Field.Store.YES,
                    Field.Index.NOT_ANALYZED,
                    Field.TermVector.NO);
            doc.add(f);
            entities.updateInstance(doc, id);
            logger.info("UPDATE INSTANCE CLASSNAME >> " + id + " " + classname.toUpperCase());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static public boolean updateInstance (String id, String name, String descr,
                                          String link, String comment,
                                          String time, String beginterval, String endinterval,
                                          String haspart) {
        try {
            Document doc = entities.getInstance(id);
            Field f;
            boolean updated = false;
            //TODO fix some lowercase type
            //System.err.println(doc.get("type").toUpperCase() + " == " +doc.get("type") + ", "+ doc.get("type").toUpperCase().equals(doc.get("type")));
            /*
            if (!doc.get("type").toUpperCase().equals(doc.get("type"))) {
                String type = doc.get("type").toUpperCase();
                doc.removeField("type");
                f= new Field("type", type,
                        Field.Store.YES,
                        Field.Index.NOT_ANALYZED,
                        Field.TermVector.NO);
                doc.add(f);
            }
              */

            if (name != null && !name.equals(doc.get("name"))) {
                doc.removeField("name");
                f= new Field("name", name,
                        Field.Store.YES,
                        Field.Index.NO,
                        Field.TermVector.NO);
                doc.add(f);
                updated = true;
            }
            if (descr != null && !descr.equals(doc.get("descr"))) {
                doc.removeField("descr");
                f= new Field("descr", descr,
                        Field.Store.YES,
                        Field.Index.NO,
                        Field.TermVector.NO);
                doc.add(f);
                updated = true;
            }
            /*if (updated) {
            doc.removeField("descnorm");
            f= new Field("descnorm", "_"+NormalizedWhitespaceAnalyzer.normalize(normText.normalize(name+" "+descr)).replaceAll(" "," _"),
                    Field.Store.NO,
                    Field.Index.ANALYZED,
                    Field.TermVector.NO);
            doc.add(f);
            } */
            if (link != null && !link.equals(doc.get("link"))) {
                doc.removeField("link");
                f= new Field("link", link,
                        Field.Store.YES,
                        Field.Index.NO,
                        Field.TermVector.NO);
                doc.add(f);
            }

            if (comment != null && !comment.equals(doc.get("comment"))) {
                doc.removeField("comment");
                f= new Field("comment", comment,
                        Field.Store.YES,
                        Field.Index.NO,
                        Field.TermVector.NO);
                doc.add(f);
            }
            if (doc.get("type").equals("EVENT")) {
                //TIME
                if (time != null) {
                    doc.removeField("time");
                    f= new Field("time", time,
                            Field.Store.YES,
                            Field.Index.NO,
                            Field.TermVector.NO);
                    doc.add(f);
                }
                //BEGIN INTERVAL
                if (beginterval != null) {
                    doc.removeField("beginterval");
                    f= new Field("beginterval", beginterval,
                            Field.Store.YES,
                            Field.Index.NO,
                            Field.TermVector.NO);
                    doc.add(f);
                }
                //END INTERVAL
                if (endinterval != null) {
                    doc.removeField("endinterval");
                    f= new Field("endinterval", endinterval,
                            Field.Store.YES,
                            Field.Index.NO,
                            Field.TermVector.NO);
                    doc.add(f);
                }
                doc.removeField("haspart");
                if (haspart != null && haspart.trim().length() >0) {
                    f= new Field("haspart", haspart.trim(),
                            Field.Store.YES,
                            Field.Index.NO,
                            Field.TermVector.NO);
                    doc.add(f);
                }
            }

            f = new Field("lastmodified",
                    sdf.format(new Date()),
                    Field.Store.YES,
                    Field.Index.NOT_ANALYZED,
                    Field.TermVector.NO);
            doc.add(f);
            entities.updateInstance(doc, id);
            logger.info("UPDATE INSTANCE >> " + id + " " + name + " - " + descr);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static public int removeInstance (String id, String user) {
        if (id != null) {
            String[] users= annotations.getDistinctAnnotators(id);
            for (String u : users) {
                //System.err.println("removeInstance: " +id + "==" +u + " equals " +user + " ("+(u != null && !u.equals(user))+")");
                if (u != null && !u.equals(user)) {
                    return 1;
                }
            }
            // remove both the instance and its annotations
            if (annotations.getNumDocs(id,user) > 0) {
                if (!annotations.removeAnnotation(id,user)) {
                    return -1;
                }
            }
            if (entities.removeInstance(id)) {
                return 0;
            }
        }

        return -1;
    }

    static public Hashtable<String, String> getInstance (String id) {
        return entities.getInfoDocument(id);
    }

    static public String getInstanceTokenIDs (String instanceid, String index, String docid, String user) {
        //logger.debug("getInstanceTokenIDs id: " + instanceid + ", docid: "+index + " " +docid);
        String[] tokenids = annotations.getAnnotation(instanceid,index,docid,user);
        if (tokenids != null) {
            //logger.debug("getInstanceTokenIDs docs " + doc.get("docs"));
            //logger.debug("getInstanceTokenIDs tokenids " + doc.get("tokenids"));
            String tokids = "";
            for (String tid : tokenids) {
                tokids += ","+tid;
            }
            //logger.debug("TOKENIDS: " + tokids.trim());
            return tokids.replaceFirst(",","").trim();
        }

        return null;
    }


    static public String getRelationTokenIDs (String rid, String user) {
        //logger.debug("getInstanceTokenIDs id: " + instanceid + ", docid: "+index + " " +docid);
        String[] tokenids = annotations.getRelationSourceTokenIDs(rid,user);
        if (tokenids != null) {
            //logger.debug("getInstanceTokenIDs docs " + doc.get("docs"));
            //logger.debug("getInstanceTokenIDs tokenids " + doc.get("tokenids"));
            String tokids = "";
            for (String tid : tokenids) {
                tokids += ","+tid;
            }
            //logger.debug("TOKENIDS: " + tokids.trim());
            return tokids.replaceFirst(",","").trim();
        }

        return null;
    }


    static public Map<String, String> getAnnotatedTokensByInstances  (String index, String docid, String user) {
        return annotations.getAnnotatedTokensByInstances(index,docid,user);
    }

    static public List<String> getAllInstanceTokenIDs2  (String index, String docid, String user) {
        List<String> entityIDandTokenID = new ArrayList<String>();

        List<String> entids = annotations.getListAnnotatedInstances(index, docid, user);
        for (String entityID : entids) {
            Document doc = entities.getInstance(entityID);
            if (doc != null) {
                String descr = doc.get("descr");
                if (descr == null) {
                    descr = "";
                } else {
                    if (descr.length() > 0)
                        descr = " - " + descr;
                }
                entityIDandTokenID.add(doc.get("id")+ " " +doc.get("type")+"/"+doc.get("class")+"//"+doc.get("name") + descr);
            }
        }
        return entityIDandTokenID;
    }


    static public Hashtable<String, String> getAllInstanceTokenIDs  (String index, String docid, String user) {
        Hashtable<String, String> entityIDandTokenID = new Hashtable<String, String>();

        List<String> entids = annotations.getListAnnotatedInstances(index, docid, user);
        for (String entityID : entids) {
            Document doc = entities.getInstance(entityID);
            if (doc != null) {
                String descr = doc.get("descr");
                if (descr == null) {
                    descr = "";
                } else {
                    if (descr.length() > 0)
                        descr = " - " + descr;
                }
                entityIDandTokenID.put(doc.get("id")+ " " +doc.get("name") + descr, "");
            }
        }

        //Hashtable<String, String> entityIDandTokenID = new Hashtable<String, String>();
        /*
        Hashtable<String, String> entityIDandTokenID = annotations.getInstanceAnnotations(index, docid, user);

        for (int i=0; i<docids.length; i=i+2) {
            //logger.debug("> " +docids[i] + " "+ docids[i+1]);
            if (docids[i].equals(index) && docids[i+1].equals(docid)) {
                //logger.debug("PUT " + doc.get("id") + " "+ tokenids[i/2]);
                String[] tokenids = doc.get("tokenids").split(",");

                descr = doc.get("descr");
                if (descr == null) {
                    descr = "";
                } else {
                    if (descr.length() > 0)
                        descr = " - " + descr;
                }

                result.put(doc.get("id")+ " " +doc.get("name") + descr, tokenids[i/2]);
            }
        }
         */

        return entityIDandTokenID;
    }

    static public Hashtable<String, String> listInstances (String filter) {
        if (filter == null || filter.replaceAll("\\*","").length() < 1)
            return entities.listInstances();

        filter = filter.trim().replaceFirst("^\\*","").replaceFirst("\\*$","");
        //logger.info("# FILTER: " +"_*"+filter.trim().replaceAll(" ", "* _*")+"*" + " " + entities.listInstances("_*"+filter.trim().replaceAll(" ", "* _*")+"*"));
        return entities.listInstances("_*"+filter.trim().replaceAll(" ", "* _*")+"*");
    }

    static public Hashtable<String, String> listLastInstances(String mode) {
        Date now = new Date();

        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        String end = sdf.format(cal.getTime());

        //cal.add( Calendar.DATE, -1 );
        Date before;
        if (mode.equals("hour")) {
            before = new Date(now.getTime() - (1000 * 60 * 60));
            cal.setTime(before);
            return entities.listLastInstances(sdf.format(cal.getTime()), end);
        } else if (mode.equals("day")) {
            before = new Date(now.getTime() - (1000 * 60 * 60 * 24));
            cal.setTime(before);
            return entities.listLastInstances(sdf.format(cal.getTime()), end);
        } else if (mode.matches("\\d+")) {
            return entities.listLastInstances(Integer.valueOf(mode));
        }
        return null;
    }

    static public boolean addIndex (String name, String group) {
        //normalize index name
        if (name != null) {
            name = name.trim().replaceFirst("^_","").replaceAll("\\s","_");
        }
        //normalize group of indexes
        if (group != null) {
            group = group.trim().replaceFirst("^_","").replaceAll("\\s","_");
        }
        return repositories.addIndex(group,name,INDEXPATH);
    }



    static private void deleteDirectory(File path) {
        if (path == null)
            return;
        if (path.exists()) {
            for (File f : path.listFiles()) {
                if(f.isDirectory()) {
                    deleteDirectory(f);
                    f.delete();
                } else {
                    f.delete();
                }
            }
            path.delete();
        }
    }

    static public List<String> getListAnnotatedRelations (String index, String docid, String user) {
        return annotations.getListAnnotatedRelations(index, docid, user);

    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        //String url = request.getRequestURI();
        //url = url.substring(request.getContextPath().length());
        String data = request.getParameter("data");
        String action = request.getParameter("action");
        String name = request.getParameter("name");
        String group = request.getParameter("group");

        //logger.debug("Controller request: " + url);
        //logger.debug(request.getRemoteAddr() + " UPDATE - Data: " + data + ", Action: " + action + ", Name: " + name + ", Group: " + group);
        try {
            String page = null;
            if (data != null) {
                String sessionid = request.getSession().toString().replaceFirst(".*@","");
                String user = (String) request.getSession().getAttribute("user");
                if (data.equals("repository")) {
                    page = "repositories.jsp?fail=1";
                    if (action.equals("add")) {
                        if (WebController.addIndex(name, group)) {
                            page = "repositories.jsp?fail=0";
                        }
                    } else if (action.equals("remove")) {
                        if (repositories.removeIndex(group+File.separator+name)) {
                            page = "repositories.jsp?fail=0";
                        }
                    }
                } else if (data.equals("instance")) {
                    page = "instances.jsp?fail=1";
                    if (action.endsWith("checkref")) {
                        WebPage webpage = crawler.getPage(new URL(name), true);
                        //logger.debug(webpage.toString());
                        PrintWriter writer = response.getWriter();
                        response.setContentType("text/plain");
                        if (webpage == null) {
                            writer.write("failed");
                        } else if(webpage.toString().contains(" 200 OK]")) {
                            writer.write("ok");
                        } else {
                            writer.write("error");
                        }
                        writer.close();
                        return;
                    } else if (action.equals("upload")) {
                        String errorlines = "";
                        int added = 0;
                        ServletFileUpload uploadHandler = new ServletFileUpload(new DiskFileItemFactory());
                        List<FileItem> items = uploadHandler.parseRequest(request);
                        int icounter = 0;
                        for (FileItem item : items) {
                            if (!item.isFormField() && item.getName().length() > 0) {
                                File file = File.createTempFile(item.getName(), "");
                                item.write(file);
                                if (file.length() > 0)
                                    logger.info("UPLOAD FILE >> " + file.getCanonicalPath() + ">>> " + item.getSize() + ", tmpname:" + file.getName());

                                InputStreamReader in = new InputStreamReader(new FileInputStream(file), Charset.forName("UTF8"));
                                BufferedReader bufffile = new BufferedReader(in);
                                String line;
                                int linecounter = 0;
                                while ((line = bufffile.readLine()) != null) {
                                    linecounter++;
                                    if (!line.startsWith("#") && line.trim().length() > 0) {
                                        icounter++;
                                        String[] values = (line+"#").split("\t");
                                        if (values.length == 6) {
                                            if (addInstance(values[2],values[1],values[0],values[3],values[4],values[5].replaceFirst("#$",""),"","","",null) != null)
                                                added++;
                                        } else if (values.length > 6) {
                                            if (addInstance(values[2],values[1],values[0],values[3],values[4],values[5].replaceFirst("#$",""),values[6],values[7],values[8],null) != null)
                                                added++;
                                        } else {
                                            errorlines += linecounter + " ";
                                        }
                                    }

                                }
                                in.close();
                                file.delete();
                            }
                        }
                        logger.info("UPLOAD FILE FINISHED >> "+added+"/"+icounter + " (errorlines="+errorlines+")");
                        page = "utilities.jsp?fail=0&added="+added+"/"+icounter+"&errorlines="+errorlines;
                    } else {
                        String id =request.getParameter("id");
                        if (id != null && id.trim().length()>0) {
                            if (action.equals("remove")) {
                                int fail=WebController.removeInstance(id, user);
                                logger.info("REMOVE: "+id+ " fail="+fail);
                                page = "instance_card.jsp?id="+id+"&action=remove&fail="+fail;
                            } else if (action.equals("removeannonly")) {
                                //System.err.println(action + " " +id);
                                page = "instance_card.jsp?id="+id+"&fail="+annotations.removeAnnotation(id, user);
                            } else {
                                //update an instance
                                if (WebController.updateInstance(id,
                                        name,
                                        request.getParameter("descr"),
                                        request.getParameter("link"),
                                        request.getParameter("comment"),
                                        request.getParameter("time"),
                                        request.getParameter("beginterval"),
                                        request.getParameter("endinterval"),
                                        request.getParameter("haspart"))) {
                                    page = "instance_card.jsp?fail=0&action="+action+"&id="+id;
                                }
                            }
                        } else {
                            //add a new instance
                            id = WebController.addInstance(name,
                                    request.getParameter("classname"),
                                    request.getParameter("type"),
                                    request.getParameter("descr"),
                                    request.getParameter("link"),
                                    request.getParameter("comment"),
                                    request.getParameter("time"),
                                    request.getParameter("beginterval"),
                                    request.getParameter("endinterval"),
                                    request.getParameter("haspart"));
                            if (id != null) {
                                page = "instance_card.jsp?fail=0&action="+action+"&id="+id;
                            }
                        }
                    }
                } else if (data.equals("document")) {
                    page = "documents.jsp?fail=1";
                    if (action.equals("add")) {
                        //logger.debug("> DOC INDEXING " + name+ "  --  " + sessionid);

                        String[] urls = uploadarchives.getUrls(sessionid);
                        Document doc;
                        Field f;
                        for (String u : urls) {
                            doc = new Document();
                            f = new Field("id", "DOC"+ String.valueOf(System.nanoTime()),
                                    Field.Store.YES,
                                    Field.Index.NOT_ANALYZED,
                                    Field.TermVector.NO);
                            doc.add(f);
                            f= new Field("url", u,
                                    Field.Store.YES,
                                    Field.Index.NOT_ANALYZED,
                                    Field.TermVector.NO);
                            doc.add(f);
                            f= new Field("index", name,
                                    Field.Store.YES,
                                    Field.Index.NO,
                                    Field.TermVector.NO);
                            doc.add(f);
                            f= new Field("lang", group,
                                    Field.Store.YES,
                                    Field.Index.NOT_ANALYZED,
                                    Field.TermVector.NO);
                            doc.add(f);

                            String inputText = new String(uploadarchives.getBytes(sessionid, u), "UTF8");
                            String txpAnnotation = "";
                            String tokens = "";
                            if (inputText.startsWith("<Document doc_name=")) {
                                CATWrapper tw = new CATWrapper(sessionid);

                                tw.analyze(inputText, group);
                                txpAnnotation =tw.getTxpFile();
                                tokens = tw.getTokens();
                                //logger.debug("TOKENS: " + tokens);
                            } else {
                                TokenProWrapper tw = new TokenProWrapper(sessionid);
                                tw.analyze(inputText, group);
                                txpAnnotation =tw.getTxpFile();
                                tokens = tw.getTokens();
                            }


                            f= new Field("token", NormalizedWhitespaceAnalyzer.normalize(normText.normalize(tokens)),
                                    Field.Store.NO,
                                    Field.Index.ANALYZED,
                                    Field.TermVector.NO);
                            doc.add(f);
                            f= new Field("txpfile", txpAnnotation,
                                    Field.Store.YES,
                                    Field.Index.NO,
                                    Field.TermVector.NO);
                            doc.add(f);
                            f= new Field("original", inputText,
                                    Field.Store.YES,
                                    Field.Index.NO,
                                    Field.TermVector.NO);
                            doc.add(f);
                            repositories.addDocument(doc, name);
                            page = "documents.jsp?fail=0";
                        }
                        if (!uploadarchives.removeArchive(sessionid)) {
                            page = "documents.jsp?fail=1";
                        }
                    } else if (action.equals("remove")) {
                        String docid = request.getParameter("id");
                        if (repositories.removeDocument(name, docid)) {
                            //remove all reference in the instances
                            annotations.removeAnnotation(null, name, docid, null, null);
                            //logger.debug("> remove all reference in the instances "+docid + " "+docs.length);
                        }
                        page = "documents.jsp?fail=0";
                    }

                } else if (data.equals("annotation")) {
                    String mesg ="error";
                    PrintWriter writer = response.getWriter();
                    response.setContentType("text/plain");
                    if (name.equals("instance")) {
                        //logger.debug("> DATA:" + data + ", USER: " + user);
                        //check if an other annotator is updating the same resource
                        // todo DOESN'T WORK, questo
                        // todo meccanismo serve solo se il task prevede la condivisioe delle annotazioni degli altri utenti
                        //if (!isLockedResource(docid+" "+instanceid, sessionid)) {
                        if (action.equals("add")) {
                            if (annotations.addAnnotation(request.getParameter("id"),
                                    request.getParameter("index"),
                                    request.getParameter("docid"),
                                    user,
                                    request.getParameter("tokenids"))) {
                                mesg= "ok";
                            }
                        } else if (action.equals("remove")) {
                            String erasetokids = annotations.removeAnnotation(request.getParameter("id"),
                                    request.getParameter("index"),
                                    request.getParameter("docid"),
                                    user,
                                    request.getParameter("tokenids"));
                            if (erasetokids != null) {
                                mesg= erasetokids;
                            }
                        }
                        //}

                    } else if (name.equals("relation")) {
                        if (action.equals("remove")) {
                            String erasetokids = annotations.removeRelation(request.getParameter("id"),
                                    user,
                                    request.getParameter("tokenids"));
                            if (erasetokids != null) {
                                mesg= erasetokids;
                            }
                        } else if (action.equals("add")) {
                            logger.debug("> DATA:" + data + ", NAME: " +name+ ", ACTION: " + action + ", USER: " + user +
                                    ", =>> "+request.getParameter("sourceID") + " ("+request.getParameter("relattribute")+") "+request.getParameter("targetID"));
                            String relID = annotations.addRelation(request.getParameter("index"),
                                    request.getParameter("docid"),
                                    request.getParameter("sourceID"),
                                    request.getParameter("sourceTokenIDs"),
                                    request.getParameter("targetID"),
                                    request.getParameter("targetTokenIDs"),
                                    request.getParameter("reltype"),
                                    request.getParameter("relattribute"),
                                    user);
                            if (relID != null) {
                                mesg=relID;
                            }
                        }
                    }
                    writer.write(mesg);
                    writer.flush();
                    writer.close();
                    return;
                }
            } else {
                page = ERRORPAGE;
            }

            if (page != null) {
                //logger.debug("Controller forward: " + page);
                //System.err.println("Controller forward: " + page);
                RequestDispatcher rd = request.getRequestDispatcher(page);
                rd.forward(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }


    public static int getAverageAnnotationTime(String user) {
        return annotations.getAverageAnnotationTime(user);
    }


    public static Hashtable<String, String> getReposToExport(String user) {
        return annotations.getReposToExport(user);
    }
}
