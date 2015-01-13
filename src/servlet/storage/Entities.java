package servlet.storage;

import eu.fbk.textpro.modules.tokenpro.NormalizeText;
import eu.fbk.textpro.wrapper.NormalizedWhitespaceAnalyzer;
import fbk.hlt.utility.archive.LuceneIndex;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;
import servlet.SingleIndexWriter;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 21/01/14
 * Time: 12.22
 */
public class Entities extends SingleIndexWriter {
    private static Entities singleton;

    private static Analyzer analyzer = new NormalizedWhitespaceAnalyzer();
    private static NormalizeText normText = new NormalizeText();

    public static Entities init() {
        if (singleton == null) {
            singleton = new Entities();
        }
        return singleton;
    }

    //document methods
    public Document getInstance (String id) {
        try {
            return getDocument(new TermQuery(new Term("id", id)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized boolean addInstance (Document doc) throws IOException {
        return addDocument(doc);
    }

    public synchronized String updateInstance (Document doc, String id) throws IOException {
        doc.removeField("descnorm");
        Field f= new Field("descnorm", "_"+NormalizedWhitespaceAnalyzer.normalize(normText.normalize(doc.get("name")+" "+doc.get("descr"))).replaceAll(" "," _"),
                Field.Store.NO,
                Field.Index.ANALYZED,
                Field.TermVector.NO);
        doc.add(f);

        return updateDocument(doc, id);
    }

    public synchronized boolean removeInstance (String id) {
        return removeDocument(new TermQuery(new Term("id", id)));
    }


    public Document[] searchInstances (String field, String content) {
        String querystr = NormalizedWhitespaceAnalyzer.normalize(normText.normalize(content));
        //System.err.println("searchInstances: "+querystr);

        // query, with default field
        try {
            Query q = new QueryParser(Version.LUCENE_29, field, analyzer).parse(querystr);
            return searchDocuments(q);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    // the key is the concatenation of the annotated num docs (space), the name and " - descr" of an instance
    // es. 23 Christian Girardi - tecnico informatico
    // the value is the concatenation of the type/class/instanceid values
    public Hashtable listInstances () {
        Hashtable<String, String> result = new Hashtable<String, String>();
        try {
            IndexReader ir = getIndexWriter().getReader();

            Document doc;
            for (int i=0; i<ir.numDocs(); i++) {
                doc = ir.document(i);

                String descr = doc.get("descr");
                if (descr == null) {
                    descr = "";
                } else {
                    if (descr.length() > 0)
                        descr = " - " + descr;
                }
                //result.put(doc.get("type") +"/"+doc.get("class")+"/"+doc.get("id"), "0 " +doc.get("name") + descr);
                result.put(doc.get("type") +"/"+doc.get("class")+"/"+doc.get("name").replaceAll("[/|\\s]","_")+"/"+doc.get("id"), "0 " +doc.get("name") + descr);
                //System.err.println("##" +doc.get("name") +", "+doc.get("type") +"/"+doc.get("class") +"/"+doc.get("id"));
            }
            ir.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public Hashtable listLastInstances (int maxNumDocs) {
        Hashtable<String, String> result = new Hashtable<String, String>();
        try {
            IndexSearcher indexSearcher = new IndexSearcher(getIndexWriter().getReader());
            Query q=new MatchAllDocsQuery();
            SortField sortField=new SortField("lastmodified",SortField.STRING, true);
            Sort sort=new Sort(sortField);

            TopFieldDocs topDocs=indexSearcher.search(q,null,maxNumDocs,sort);
            Document doc;
            if (topDocs.totalHits > 0) {
                ScoreDoc[] scoreDosArray = topDocs.scoreDocs;
                for (int i=0; i < scoreDosArray.length; i++) {
                    doc = indexSearcher.doc(scoreDosArray[i].doc);
                    String descr = doc.get("descr");
                    if (descr == null) {
                        descr = "";
                    } else {
                        if (descr.length() > 0)
                            descr = " - " + descr;
                    }
                    result.put(doc.get("type") +"/"+doc.get("class") +"/"+doc.get("name").replaceAll("[/|\\s]","_")+"/"+doc.get("id"), doc.get("numdocs")+ " " +doc.get("name") + descr);
                }
            }
        }  catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public Hashtable listLastInstances (String start, String end) {
        Hashtable<String, String> result = new Hashtable<String, String>();
        try {
            IndexSearcher indexSearcher = new IndexSearcher(getIndexWriter().getReader());

            Term startTerm = new Term("lastmodified", start);
            Term endTerm = new Term("lastmodified", end);
            Query query = new RangeQuery(startTerm, endTerm, true);
            Hits hits = indexSearcher.search(query);
            Document doc;
            for (int docnum=0;docnum<hits.length(); docnum++ ) {
                doc = hits.doc(docnum);
                String descr = doc.get("descr");
                if (descr == null) {
                    descr = "";
                } else {
                    if (descr.length() > 0)
                        descr = " - " + descr;
                }
                result.put(doc.get("type") +"/"+doc.get("class") +"/"+doc.get("name").replaceAll("[/|\\s]","_")+"/"+doc.get("id"), doc.get("numdocs")+ " " +doc.get("name") + descr);
            }
        }  catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    // il sort non funziona se si inserisce uno spazio nel nome
    public Hashtable listInstances (String query) {
        Hashtable<String, String> result = new Hashtable<String, String>();
        try {

            QueryParser qp = new QueryParser("descnorm", analyzer);

            //Sort sort = new Sort("name", true);
            //Sort sort = new Sort(new SortField("name", SortField.STRING));
            Query q = qp.parse("descnorm: " + NormalizedWhitespaceAnalyzer.normalize(normText.normalize(query)));
            Document[] hits = searchDocuments(q);  //, sort);
            Document doc;
            System.out.println("listInstances " + query + " ("+hits.length+")");
            for (int docnum=0;docnum<hits.length; docnum++ ) {
                doc = hits[docnum];
                String descr = doc.get("descr");
                if (descr == null) {
                    descr = "";
                } else {
                    if (descr.length() > 0)
                        descr = " - " + descr;
                }
                result.put(doc.get("type") +"/"+doc.get("class") +"/"+doc.get("name").replaceAll("[/|\\s]","_")+"/"+doc.get("id"), doc.get("numdocs")+ " " +doc.get("name") + descr);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
