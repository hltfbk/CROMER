package servlet.storage;

import servlet.WebController;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 21/01/14
 * Time: 11.50
 *
 */
public class CrossDocAnnotations {
    private static final String ANNOTATIONTABLENAME = "annotation";
    private static final String RELATIONTABLENAME = "relation";
    private DBConnection db = new DBConnection();

    public CrossDocAnnotations() {
        try {
            if (db.getDBConnection() == null) {
                WebController.logger.error("Connection to MySQL server failed!");
            } else {
                WebController.logger.info("MySQL connection established!");

                String query = "CREATE TABLE IF NOT EXISTS `annotation` (\n" +
                        " `entityID` varchar(40) NOT NULL,\n" +
                        " `repository` varchar(100) NOT NULL,\n" +
                        " `docID` varchar(40) NOT NULL,\n" +
                        " `user` varchar(30) NOT NULL,\n" +
                        " `tokenids` text,\n" +
                        " `lasttime` datetime NOT NULL,\n" +
                        " PRIMARY KEY (`entityID`,`repository`,`docID`,`user`),\n" +
                        " KEY `entityID` (`entityID`),\n" +
                        " KEY `repository` (`repository`),\n" +
                        " KEY `docID` (`docID`),\n" +
                        " KEY `user` (`user`)\n" +
                        ");";

                db.executeUpdate(query, null);

                query = "CREATE TABLE IF NOT EXISTS `relation` (\n" +
                        "  `id` int(100) NOT NULL auto_increment PRIMARY KEY,\n" +
                        "  `repository` varchar(100) NOT NULL,\n" +
                        "  `docID` varchar(40) NOT NULL,\n" +
                        "  `sourceID` varchar(40) NOT NULL,\n" +
                        "  `sourceTokenIDs` text,\n" +
                        "  `targetID` varchar(40) NOT NULL,\n" +
                        "  `targetTokenIDs` text,\n" +
                        "  `reltype` varchar(40) NOT NULL,\n" +
                        "  `relattribute` varchar(40) NOT NULL,\n" +
                        "  `user` varchar(30) NOT NULL,\n" +
                        "  `lasttime` datetime NOT NULL,\n" +
                        //"  PRIMARY KEY (`repository`,`docID`,`sourceID`,`targetID`,`reltype`,`user`),\n" +
                        "  KEY `sourceID` (`sourceID`),\n" +
                        "  KEY `targetID` (`targetID`),\n" +
                        "  KEY `reltype` (`reltype`),\n" +
                        "  KEY `repository` (`repository`),\n" +
                        "  KEY `docID` (`docID`),\n" +
                        "  KEY `user` (`user`)\n" +
                        ");";
                db.executeUpdate(query, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean checkDBConnection () {
        try {
            return db.checkDBConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //document methods
    public String[] getAnnotation (String entityid, String repository, String docid, String user) {
        try {
            String query = "";
            /*if (user == null) {
                query = "SELECT tokenids FROM "+TABLENAME+" WHERE entityID='"+entityid+"'"
                        +" && repository='"+repository+"'"
                        +" && docID='"+docid+"'";
            } else {
            */
            query = "SELECT tokenids FROM "+ANNOTATIONTABLENAME+" WHERE entityID='"+entityid+"'"
                    +" && repository='"+repository+"'"
                    +" && docID='"+docid+"'"
                    +" && user='"+user+"'";
            //}
            String ids = "";
            ResultSet rs = db.executeQuery(query, user);
            if (rs != null) {
                while (rs.next()) {
                    ids = rs.getString(1);
                    break;
                }
            }

            //WebController.logger.debug("getAnnotation: " + entityid + ", REPO: " + repository + ", DOCID: " +
            //        docid + ",USER: " +user + " ids.length():" +ids.length()+" -- " +ids);
            if (ids.length()==0)
                return null;

            return ids.split(",");
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    public synchronized boolean removeAnnotation (String entityID, String user) {
        String query = "DELETE FROM " +ANNOTATIONTABLENAME+ " WHERE entityID='"+entityID+"' AND user='"+user+"'";
        //System.err.println(query);

        return db.executeUpdate(query, user);
    }

    public synchronized String removeAnnotation (String entityid, String repository, String docid, String user, String tokids) {
        String erasetokids = "";
        if (tokids != null) {
            tokids = " "+tokids+" ";
            String[] storedTokenIDs = getAnnotation(entityid, repository, docid, user);

            if (storedTokenIDs != null) {
                String tokenids="";
                for (String tid : storedTokenIDs) {
                    if ((" "+tid+" ").contains(tokids)) {
                        erasetokids = tid;
                    } else {
                        tokenids +=","+tid;
                    }
                }

                tokenids = tokenids.replaceFirst("^,","").replaceAll("\\s+"," ").trim();
                String query;
                if (tokenids.length() >0) {
                    query = "UPDATE "+ANNOTATIONTABLENAME+" SET tokenids='"+tokenids+"',lasttime=now() ";
                } else {
                    query = "DELETE from "+ANNOTATIONTABLENAME;
                }
                query += " WHERE entityID='"+entityid+"'"
                        +" AND repository='"+repository+"'"
                        +" AND docID='"+docid+"'"
                        +" AND user='"+user+"'";
                db.executeUpdate(query,user);
            }
        } else {
            String query = "";
            if (repository != null && docid != null)
                query += "repository='"+repository+"' AND docID='"+docid+"'";
            if (entityid != null)
                if (query.length() > 0)
                    query += " AND";
            query += " entityID='"+entityid+"'";
            if (user != null)
                if (query.length() > 0)
                    query += " AND";
            query += " user='"+user+"'";
            db.executeUpdate("DELETE FROM " +ANNOTATIONTABLENAME+ " WHERE " + query,user);
        }

        return erasetokids;
    }

    public synchronized boolean addAnnotation (String entityid, String repository, String docid, String user, String tokids) {
        if (tokids != null) {
            String[] storedTokenIDs = getAnnotation(entityid, repository, docid, user);
            String query = null;
            if (storedTokenIDs != null && storedTokenIDs.length > 0) {
                String tokenids=tokids;
                for (String tid : storedTokenIDs) {
                    if (tid.equalsIgnoreCase(tokids))
                        continue;
                    tokenids +=","+tid;
                }
                tokenids = tokenids.replaceFirst("^,","").replaceAll("\\s+"," ").trim();

                if (tokenids.length() == 0) {
                    query = "DELETE FROM "+ANNOTATIONTABLENAME+" WHERE entityID='"+entityid+"'"
                            +" && repository='"+repository+"'"
                            +" && docID='"+docid+"'"
                            +" && user='"+user+"'";
                } else {
                    query = "UPDATE "+ANNOTATIONTABLENAME+" SET tokenids='"+tokenids+"',lasttime=now()  WHERE entityID='"+entityid+"'"
                            +" && repository='"+repository+"'"
                            +" && docID='"+docid+"'"
                            +" && user='"+user+"'";
                }
            } else {
                query = "INSERT INTO "+ANNOTATIONTABLENAME+" VALUES ('"+entityid+"','"+repository+"','"+docid+"','"+user+"','"+tokids+"',now())";
            }

            return db.executeUpdate(query,user);
        }
        return false;
    }

    public synchronized int getNumDocs (String entityid, String user) {
        try {
            String query = "SELECT count(*) FROM " + ANNOTATIONTABLENAME + " WHERE entityID='" + entityid + "'";
            if (user != null)
                query += " AND user='" + user + "'";
            ResultSet rs = db.executeQuery(query, user);
            if (rs != null) {
                rs.next();
                return Integer.valueOf(rs.getString(1));
            } else {
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public String[] getDocumentsByInstance (String[] repositories, String entityid, String user) {
        try {
            StringBuilder sb = new StringBuilder();
            for (String repo : repositories) {
                String query = "SELECT DISTINCT docID FROM "+
                        ANNOTATIONTABLENAME+" WHERE repository='"+repo+"' AND entityID='"+entityid+"'"
                        +" AND user='"+user+"'";
                //WebController.logger.debug("getDocumentsByInstance " + entityid + " -- " + query);

                ResultSet rs = db.executeQuery(query,user);
                while (rs.next()) {
                    if (sb.length() > 0)
                        sb.append(" ");
                    sb.append(repo).append(" ").append(rs.getString("docID"));
                }
            }
            //WebController.logger.debug("getDocumentsByInstance> "+sb.toString());
            return sb.toString().split(" ");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new String[0];
    }


    public String[] getDistinctAnnotators (String entityID) {
        String[] users = new String[0];
        try {
            ResultSet rs = db.executeQuery("SELECT DISTINCT user FROM " + ANNOTATIONTABLENAME + " WHERE entityID='" + entityID + "'","");
            if (rs != null) {
                rs.last();
                users = new String[rs.getRow()+1];
                rs.beforeFirst();
                while (rs.next()) {
                    users[rs.getRow()] = rs.getString("user");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    public int getLinkedInstances(String docid, String user) {
        try {
            String query = "SELECT DISTINCT entityID FROM " + ANNOTATIONTABLENAME + " WHERE docid='" + docid + "' AND user='"+user+"' AND tokenids!=''";
            ResultSet rs = db.executeQuery(query,user);
            if (rs != null) {
                rs.last();
                return rs.getRow();
            } else {
                return -1;
            }
        }  catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List getLinkedInstances(String user) {
        ArrayList<String> ids = new ArrayList<String>();
        try {
            String query = "SELECT DISTINCT entityID FROM " + ANNOTATIONTABLENAME + " WHERE tokenids!=''";
            if (user != null && user.trim().length() > 0) {
                query +=" AND user='"+user+"'";
            }
            ResultSet rs = db.executeQuery(query, user);
            while (rs.next()) {
                ids.add(rs.getString("entityID"));
            }
        }  catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public HashMap<String,String> getAnnotatedTokensByInstances(String repository, String docid, String user) {
        HashMap<String,String> result = new HashMap<String,String>();
        try {
            ResultSet rs = db.executeQuery("SELECT DISTINCT entityid, tokenids FROM " + ANNOTATIONTABLENAME + " WHERE repository='" + repository + "'"
                    + " AND docid='" + docid + "' AND user='" + user + "'", user);
            while (rs.next()) {
                String[] ids = rs.getString("tokenids").split(",");
                for (String id: ids) {
                    if (id.trim().length() > 0) {
                        //System.err.println("---"+rs.getString("entityid") + " :: "+id);
                        if (result.containsKey(id) && !result.get(id).contains(rs.getString("entityid"))) {
                            result.put(id,result.get(id) + " " +rs.getString("entityid"));
                        } else
                            result.put(id,rs.getString("entityid"));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<String> getListAnnotatedInstances(String repository, String docid, String user) {
        ArrayList<String> result = new ArrayList<String>();
        try {
            ResultSet rs = db.executeQuery("SELECT DISTINCT entityid FROM " + ANNOTATIONTABLENAME + " WHERE repository='" + repository + "'"
                    + " AND docid='" + docid + "' AND user='" + user + "'", user);
            while (rs.next()) {
                result.add(rs.getString("entityID"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public LinkedHashMap<String, Hashtable<String, String>> getStats(String user) {
        LinkedHashMap<String, Hashtable<String, String>> hash = new LinkedHashMap<String, Hashtable<String, String>>();
        try {
            if (user != null && !user.equals("admin")) {
                //number of annotated mentions
                ResultSet rs = db.executeQuery("SELECT DISTINCT SUBSTR(lasttime,1,10) as day, sum(1+ LENGTH(tokenids) - LENGTH(REPLACE(tokenids, ',', ''))) AS mentions FROM " + ANNOTATIONTABLENAME + " where user='"+user+"' GROUP BY day ORDER BY day DESC;", user);
                while (rs.next()) {
                    Hashtable<String, String> h = new Hashtable<String, String>();
                    h.put("mnt", rs.getString("mentions"));
                    hash.put(rs.getString("day"), h);
                }

                //number of annotated documents
                rs = db.executeQuery("SELECT DISTINCT SUBSTR(lasttime,1,10) AS day, COUNT(distinct docid) as docs FROM " + ANNOTATIONTABLENAME + " WHERE user='"+user+"' GROUP BY day ORDER BY day DESC", user);
                while (rs.next()) {
                    Hashtable<String, String> h = new Hashtable<String, String>();
                    if (hash.containsKey(rs.getString("day"))) {
                        h = hash.get(rs.getString("day"));
                    }
                    h.put("doc",rs.getString("docs"));
                    hash.put(rs.getString("day"), h);
                }

                //number of annotated documents
                rs = db.executeQuery("SELECT DISTINCT SUBSTR(lasttime,1,10) AS day, COUNT(distinct entityid) as instances FROM " + ANNOTATIONTABLENAME + " WHERE user='"+user+"' GROUP BY day ORDER BY day DESC", user);
                while (rs.next()) {
                    Hashtable<String, String> h = new Hashtable<String, String>();
                    if (hash.containsKey(rs.getString("day"))) {
                        h = hash.get(rs.getString("day"));
                    }
                    h.put("ins",rs.getString("instances"));
                    hash.put(rs.getString("day"), h);
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return hash;
    }


    public List<String> getListAnnotatedRelations (String repository, String docid, String user) {
        ArrayList<String> result = new ArrayList<String>();
        try {
            ResultSet rs = db.executeQuery("SELECT DISTINCT id,sourceID,sourceTokenIDs,targetID,targetTokenIDs,reltype,relattribute FROM " + RELATIONTABLENAME + " WHERE repository='" + repository + "'"
                    + " AND docid='" + docid + "' AND user='" + user + "' order by reltype", user);
            while (rs.next()) {
                result.add(rs.getString("id")+"\t"+
                        rs.getString("reltype")+"\t"+
                        rs.getString("sourceID")+"\t"+
                        rs.getString("sourceTokenIDs")+"\t"+
                        rs.getString("targetID")+"\t"+
                        rs.getString("targetTokenIDs")+"\t"+
                        rs.getString("relattribute"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getRelationID (String repository, String docid, String sourceID, String targetID, String relattribute, String user) {
        try {
            String query = "SELECT id FROM "+RELATIONTABLENAME+" WHERE " +
                    "repository='"+repository+"' AND docid='"+docid+"' AND sourceID='"+sourceID+
                    "' AND targetID='"+targetID+"' AND relattribute='"+relattribute+"' AND user='"+user+"'";
            System.err.println(query);
            ResultSet rs = db.executeQuery(query, user);
            if (rs != null) {
                rs.next();
                return rs.getString(1);
            }
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    public String[] getRelationSourceTokenIDs (String id, String user) {
        try {
            String query = "SELECT sourceTokenIDs FROM "+RELATIONTABLENAME+" WHERE id=" +id+ " AND user='"+user+"'";
            String ids = "";
            ResultSet rs = db.executeQuery(query, user);
            if (rs != null) {
                while (rs.next()) {
                    ids = rs.getString(1);
                    break;
                }
            }

            if (ids.length()==0)
                return null;

            return ids.split(",");
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        return null;
    }


    public synchronized String removeRelation (String id, String user, String tokenids) {
        String erasetokids = "";
        if (tokenids != null) {
            String[] storedTokenIDs = getRelationSourceTokenIDs(id, user);

            if (storedTokenIDs != null) {
                String newids="";
                tokenids= " "+tokenids+" ";
                for (String tid : storedTokenIDs) {
                    if ((" "+tid+" ").contains(tokenids)) {
                        erasetokids = tid;
                    } else {
                        newids +=","+tid;
                    }
                }

                newids = newids.replaceFirst("^,","").replaceAll("\\s+"," ").trim();
                String query;
                if (newids.length() >0) {
                    query = "UPDATE "+RELATIONTABLENAME+" SET sourceTokenIDs='"+newids+"',lasttime=now() ";
                } else {
                    query = "DELETE from "+RELATIONTABLENAME;
                }
                query += " WHERE id=" + id
                        +" AND user='"+user+"'";
                db.executeUpdate(query,user);
            }
        } else {
            db.executeUpdate("DELETE FROM " +RELATIONTABLENAME+ " WHERE id="+id+" AND user='"+user+"'",user);
        }
        return erasetokids;
    }

    public synchronized String addRelation (String repository, String docid,
                                            String sourceID, String sourceTokenIDs,
                                            String targetID, String targetTokenIDs,
                                            String reltype, String relattribute,
                                            String user) {
        if (sourceTokenIDs != null) {
            String query = null;
            String rid = getRelationID(repository, docid, sourceID, targetID, relattribute, user);
            if (rid != null) {
                String[] storedTokenIDs = getRelationSourceTokenIDs(rid, user);
                String tokenids=sourceTokenIDs;
                if (storedTokenIDs != null && storedTokenIDs.length > 0) {
                    for (String tid : storedTokenIDs) {
                        if (tid.equalsIgnoreCase(sourceTokenIDs))
                            continue;
                        tokenids +=","+tid;
                    }
                    tokenids = tokenids.replaceFirst("^,","").replaceAll("\\s+"," ").trim();

                    query = "UPDATE "+RELATIONTABLENAME+" SET sourceTokenIDs='"+tokenids+"', relattribute='"+relattribute+"',lasttime=now() WHERE "+
                            "repository='"+repository+"' AND docid='"+docid+"' AND sourceID='"+sourceID+
                            "' AND targetID='"+targetID+"' AND user='"+user+"'";

                }
            } else {
                query = "INSERT INTO "+RELATIONTABLENAME+" VALUES (null,'"+repository+"','"+docid+"','"+sourceID+"','"+sourceTokenIDs+"','"+targetID+"','"+targetTokenIDs+"','"+reltype+"','"+relattribute+"','"+user+"',now())";
            }

            if (query != null && db.executeUpdate(query,user)) {
                if (rid == null) {
                    rid=getRelationID(repository, docid, sourceID, targetID, relattribute, user);
                }
                return rid;
            }
        }
        return null;
    }

    public Hashtable<String, String> getReposToExport (String user) {
        ResultSet rs = db.executeQuery("SELECT distinct docid, repository FROM " + ANNOTATIONTABLENAME + " WHERE user='"+user+"' ORDER BY repository, docid", user);
        try {
            Hashtable<String, String> repos = new Hashtable<String, String>();
            while (rs.next()) {
                if (repos.containsKey(rs.getString("repository"))) {
                    repos.put(rs.getString("repository"),repos.get(rs.getString("repository")) + " " + rs.getString("docid"));
                } else {
                    repos.put(rs.getString("repository"),rs.getString("docid"));
                }
            }
            return repos;
        } catch (SQLException e) {
            WebController.logger.error("Error on XML documents export (user: "+user +")");
            e.printStackTrace();
        }
        return null;
    }

    public String exportAnnotations(String user, String mode, String format, Repositories repositories) {
        StringBuilder export = new StringBuilder();
        ResultSet rs = db.executeQuery("SELECT docid,repository,entityid,tokenids,lasttime FROM " + ANNOTATIONTABLENAME + " WHERE user='"+user+"' ORDER BY docid", user);
        try {
            if (rs != null) {
                while (rs.next()) {
                    String[] tokids = rs.getString("tokenids").split(",");
                    for (String tids : tokids) {
                        if (format.equals("csv")) {
                            String url = "";
                            try {
                                url = repositories.getDocUrl(rs.getString("repository"), rs.getString("docid"));
                            } catch (IOException e) {
                                WebController.logger.error("url of "+rs.getString("repository")+"/"+ rs.getString("docid") +" is missed");
                                e.printStackTrace();
                            }
                            export.append(rs.getString("docid")).append("\t").append(rs.getString("repository")).append("/").append(url).append("\t").append(rs.getString("entityid")).append("\t").append(tids).append("\t").append(rs.getDate("lasttime")).append(" ").append(rs.getTime("lasttime")).append("\n");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return export.toString();
    }

    public int getAverageAnnotationTime(String user) {
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        long prevdate = 0;
        int datecounter = 0;
        int sumseconds = 0;
        long intervalseconds = 0;
        ResultSet rs = db.executeQuery("SELECT lasttime FROM " + ANNOTATIONTABLENAME + " WHERE user='"+user+"' ORDER BY lasttime", user);
        try {
            if (rs != null) {
                while (rs.next()) {
                    if (!rs.getString("lasttime").endsWith(" 00:00:00.0")) {
                        if (prevdate > 0) {
                            intervalseconds = (rs.getTimestamp("lasttime").getTime() - prevdate)/1000;
                            //System.err.println ("time: " + sdf.format(rs.getTimestamp("lasttime")) + " // " +rs.getTimestamp("lasttime").getTime() + " - " + prevdate +" = " + intervalseconds);
                            if (intervalseconds < 600) {
                                datecounter++;
                                sumseconds += intervalseconds;
                            } else {
                                prevdate = 0;
                                continue;
                            }
                        }
                        //System.err.println(">> " +sdf.format(rs.getTimestamp("lasttime")));
                        prevdate = rs.getTimestamp("lasttime").getTime();

                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (datecounter > 0) {
            //System.err.println ("getAverageAnnotationTime for "+user+": " +sumseconds+"/"+datecounter+"="+sumseconds/datecounter);

            return sumseconds/datecounter;
        }
        return 0;
    }
}
