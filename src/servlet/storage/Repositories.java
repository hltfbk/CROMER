package servlet.storage;

import eu.fbk.textpro.modules.tokenpro.NormalizeText;
import eu.fbk.textpro.wrapper.NormalizedWhitespaceAnalyzer;
import fbk.hlt.utility.archive.LuceneIndex;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;
import servlet.WebController;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 21/01/14
 * Time: 12.23
 */
public class Repositories {
    private static Repositories singleton = null;

    private static String NEWSDIRNAME = "news"+File.separator;
    private List<String> lockIndexes = new ArrayList<String>();
    private Hashtable<String,IndexWriter> docsIndexes = new Hashtable<String,IndexWriter>();
    private static Analyzer analyzer = new NormalizedWhitespaceAnalyzer();
    private static NormalizeText normText = new NormalizeText();

    public static Repositories init() {
        if (singleton == null) {
            singleton = new Repositories();
        }

        return singleton;
    }

    public int indexLoader (String path) {
        int num = 0;

        File repoDir = new File(path);
        if (!repoDir.exists()) {
            repoDir.mkdirs();
        }

        //News indexes
        //loop on 2 levels directory
        if (repoDir.isDirectory()) {
            String[] subDirs = repoDir.list();
            for (String subDir : subDirs) {
                File sectionDir = new File(repoDir, subDir);
                if (sectionDir.isDirectory()) {
                    String[] listRepos = sectionDir.list();
                    for (String listRepo : listRepos) {
                        File rDir = new File(sectionDir, listRepo);
                        if (rDir.isDirectory()) {
                            IndexWriter indexwriter = LuceneIndex.createIndex(new File(rDir, NEWSDIRNAME), analyzer);
                            if (indexwriter != null) {
                                //Directory directory = FSDirectory.getDirectory(INDEX_DIRECTORY);
                                //IndexSearcher indexSearcher = new IndexSearcher(directory);

                                docsIndexes.put(subDir + File.separator + listRepo, indexwriter);
                                int numdocs = 0;
                                try {
                                    numdocs = indexwriter.numDocs();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                //System.out.println("LOAD INDEX " + sectionDir + File.separator + listRepo + File.separator + NEWSDIRNAME + " (" + numdocs + " docs)");
                                num++;
                            }
                        }
                    }
                }
            }
        }

        return num;
    }

    //News methods
    // create a new index
    public boolean addIndex (String dir, String name, String indexpath) {
        if (!docsIndexes.containsKey(dir+ File.separator+name)) {
            IndexWriter indexwriter = LuceneIndex.createIndex(new File(indexpath+File.separator+dir + File.separator + name, NEWSDIRNAME), analyzer);
            if (indexwriter != null) {
                //System.out.println("Add a new index: " + indexwriter.getDirectory());
                docsIndexes.put(dir+File.separator+name, indexwriter);
                return true;
            }
        }
        return false;
    }


    // remove an existing index of docs
    public boolean removeIndex (String indexname) throws IOException {
        if (docsIndexes.containsKey(indexname)) {
            File indexpath = this.getIndexPath(indexname);
            docsIndexes.remove(indexname);
            if (indexpath.exists()) {
                //WebController.logger.debug("REMOVE DIR " + indexpath.getParentFile());
                FileUtils.deleteDirectory(indexpath.getParentFile());

                if (!indexpath.exists())
                    return true;
            }
        }
        return false;
    }

    public String[] getNames() {
        List<String> sortedKeys=new ArrayList<String>(docsIndexes.keySet());

        Collections.sort(sortedKeys);
        String[] array = new String[sortedKeys.size()];
        int i = 0;
        for (String key : sortedKeys) {
            array[i] = key;
            i++;
        }
        return array;
    }

    public int getIndexDocs(String indexname) throws IOException {
        if (docsIndexes.containsKey(indexname)) {
            IndexWriter i = docsIndexes.get(indexname);
            //WebController.logger.debug("-- " +indexname+ " " +i.getDirectory());
            return i.numDocs();
        }
        return 0;
    }

    public File getIndexPath(String indexname) {
        if (docsIndexes.containsKey(indexname)) {
            return new File(docsIndexes.get(indexname).getDirectory().toString().replaceFirst(".*@",""));
        }
        return null;
    }

    public synchronized boolean addDocument(Document doc, String indexname) throws IOException {
        if (docsIndexes.containsKey(indexname)) {
            while (lockIndexes.contains(indexname)) {
                //System.out.println("addDocument to "+indexname+" index waiting...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            lockIndexes.add(indexname);
            IndexWriter index = docsIndexes.get(indexname);
            index.addDocument(doc);
            index.commit();
            lockIndexes.remove(indexname);

            return true;
        }

        return false;
    }

    public synchronized boolean removeDocument (String indexname, String docid) {
        if (docsIndexes.containsKey(indexname)) {
            while (lockIndexes.contains(indexname)) {
                //System.out.println("removeDocument from "+indexname+" index waiting...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            lockIndexes.add(indexname);
            IndexWriter index = docsIndexes.get(indexname);

            try {
                index.deleteDocuments(new Term("id", docid));
                //System.out.println("removeDocument " + docid+" to index waiting...");
                index.commit();
                index.expungeDeletes();
                index.optimize();
            } catch (IOException e) {
                e.printStackTrace();
            }
            lockIndexes.remove(indexname);
            return true;
        }
        return false;
    }

    public synchronized Document getDocument(String index, String docid) throws IOException {
        Document doc = null;
        if (docsIndexes.containsKey(index)) {
            IndexReader ir = docsIndexes.get(index).getReader();
            IndexSearcher indexSearcher = new IndexSearcher(ir);

            Query termQuery = new TermQuery(new Term("id", docid));
            TopDocs topDocs = indexSearcher.search(termQuery,1);

            if (topDocs.totalHits > 0) {
                ScoreDoc[] scoreDosArray = topDocs.scoreDocs;
                doc = indexSearcher.doc(scoreDosArray[0].doc);
            }
            indexSearcher.close();
            ir.close();
        }
        return doc;
    }

    public String getDocUrl (String index, String docid) throws IOException {
        //WebController.logger.debug("getDocUrl " + index + " " +docid + " ("+docsIndexes.get(index)+")");
        Document doc = getDocument(index, docid);
        if (doc != null) {
            return doc.get("url");
        }
        return null;
    }

    // return and hash with id and url doc info
    public LinkedHashMap<String, String> getInfoDocs (String index) throws IOException {
        LinkedHashMap<String, String> docinfo = new LinkedHashMap<String, String>();

        IndexReader ir = docsIndexes.get(index).getReader();
        Document doc;
        for (int i=0; i<ir.numDocs(); i++) {
            doc = ir.document(i);
            docinfo.put(doc.get("id"), doc.get("url"));
        }
        ir.close();
        return docinfo;
    }

    public String[] getDocSentences (String index, String docid, String query) throws IOException {
        //WebController.logger.debug("QUERY ORIG: "+query);
        if (query != null)
            query = "#"+NormalizedWhitespaceAnalyzer.normalize(normText.normalize(query.trim())).replaceAll("[\\s|\"]+","#")+"#";
            //WebController.logger.debug("QUERY NORM: "+query);
        else
            return new String[0];
        String querymaching = ".*"+query.replaceAll("\\*",".*")+".*";

        List<String> result = new ArrayList<String>();

        Document doc = getDocument(index, docid);
        String[] txplines = doc.get("txpfile").split("\n");

        boolean found = false;
        StringBuilder sentence = new StringBuilder();

        int currentPosition = 0;
        String space;
        String[] items;
        int sentenceIDs = 1;
        sentence.append("<div class='sentcounter'><i>s"+sentenceIDs+"</i>.&nbsp;</div>&nbsp;<div style='display: inline'>");

        try{
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
                items = txpline.split("\t");
                space = "";
                if (items[1].length() == 0) {
                    WebController.logger.error(" on index " + index + " docid:" + docid + "(position: " + currentPosition + ")\n" + doc.get("txpfile"));
                } else {
                    if (currentPosition != Integer.valueOf(items[1])) {
                        space = " ";
                    }

                    //WebController.logger.debug(querymaching + " @@ " +NormalizedWhitespaceAnalyzer.normalize(normText.normalize(items[0])));
                    if (("#"+NormalizedWhitespaceAnalyzer.normalize(normText.normalize(items[0]))+"#").matches(querymaching)) {
                        found = true;
                        sentence.append("<font class=highlgh>").append(space).append(items[0]).append("</font>");
                    } else {
                        sentence.append(space).append(items[0]);

                    }
                    currentPosition = Integer.valueOf(items[1]) + items[0].length();
                }
            }
            //add last sentence if something is found
            if (found) {
                result.add(sentence.toString()+"</div>");
            }
        } catch (Exception e) {
            WebController.logger.error("MATCH FAILED: " +querymaching + " @@ index "+index + " docid:" +docid+"(position: " + currentPosition + ")");
        }
        String[] sentences = new String[result.size()];
        for (int i=0; i<result.size(); i++) {
            sentences[i] = result.get(i);
        }
        return sentences;
    }


    public String[] searchIndex (String[] indexnames, String field, String content) {
        String[] result = new String[0];

        // query string
        //String querystr = NormalizedWhitespaceAnalyzer.normalize(content);
        String querystr = NormalizedWhitespaceAnalyzer.normalize(normText.normalize(content));
        //WebController.logger.debug("searchIndex: "+querystr);

        // query, with default field
        try {
            Query q = new QueryParser(Version.LUCENE_29, field, analyzer).parse(querystr);

            // search
            //int hitsPerPage = 10;
            IndexSearcher[] searchers = new IndexSearcher[indexnames.length];
            for (int i=0; i<indexnames.length; i++) {
                searchers[i] = new IndexSearcher(docsIndexes.get(indexnames[i]).getReader());
            }
            MultiSearcher searcher = new MultiSearcher(searchers);

            TopScoreDocCollector collector =
                    TopScoreDocCollector.create(searcher.maxDoc(), true);
            searcher.search(q, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            // display results
            //System.out.println(hits.length + " results.");
            result = new String[hits.length];
            Document doc;
            for(int i=0; i < hits.length; i++) {
                doc = searcher.doc(hits[i].doc);
                result[i] = doc.get("index")+","+doc.get("id"); //+","+doc.get("url");
            }

            // close searcher
            searcher.close();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (CorruptIndexException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static String getISODate () {
        TimeZone tz = TimeZone.getTimeZone("CET");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'CET'");
        df.setTimeZone(tz);
        return df.format(new Date());
    }

    public List<String> getDivTokens (String index, String docid, String query) throws IOException {
        List<String> html = new ArrayList<String>();

        Document doc = getDocument(index, docid);
        try {
            if (doc != null) {

                String[] txplines = doc.get("txpfile").split("\n");
                //WebController.logger.debug("getDivTokens: " + index + "/" +docid + " " + query);
                int i = 0;
                int prev_token_lenght=0;
                int prev_pos=0;
                String space = "";
                String[] items;
                for (String txpline : txplines) {
                    if (txpline.startsWith("# "))
                        continue;
                    else if (txpline.trim().length() == 0) {
                        //html.append("<div id='"+i+"-" +(i+1)+"'><br></div><br>");
                        html.add("<br>");
                        space = "";
                        continue;
                    }

                    items = txpline.split("\t");
                    if (i > 0) {
                        if (items[1].equals("")) {
                            WebController.logger.error("getDivTokens: " + index + "/" +docid + " " + txpline +" (line: "+i+")");
                        } else {
                            if ((prev_pos + prev_token_lenght) != Integer.valueOf(items[1])) {
                                html.add("<div id='"+i+"-"+(i+1)+"' onmouseup=\"mymouseup(this);\" onmousedown=\"mymousedown(this);\">" + space + "</div>");
                            } //else {
                            //html.append("<div id='"+i+"-" +(i+1)+"'></div>");
                            //}
                        }
                    }
                    //prev_token_lenght = items[0].length();
                    //prev_pos = Integer.valueOf(items[1]);

                    String highclass = "";
                    if (query.equals(items[0])) {
                        highclass = " class=highlgh";
                    }

                    html.add("<div id='"+(i+1)+"'"+highclass+" onmouseup=\"mymouseup(this);\" onmousedown=\"mymousedown(this);\">"+items[0]+"</div>");
                    space = "&nbsp;";
                    i++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return html;
    }

}
