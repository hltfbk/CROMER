package servlet;

import eu.fbk.textpro.modules.tokenpro.NormalizeText;
import eu.fbk.textpro.wrapper.NormalizedWhitespaceAnalyzer;
import fbk.hlt.utility.archive.LuceneIndex;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 29-ago-2013
 * Time: 23.41.33
 */
public class SingleIndexWriter {
    private List<String> lockIndexes = new ArrayList<String>();

    private File indexFile = null;
    private boolean indexLock = false;
    private IndexWriter indexwriter = null;

    public int indexLoader(String path, Analyzer analyzer) {
        indexFile = new File(path);
        indexwriter = LuceneIndex.createIndex(indexFile, analyzer);

        return size();
    }

    public int size() {
        try {
            return indexwriter.numDocs();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Hashtable<String, String> getInfoDocument (String id) {
        Hashtable<String, String> result = null;
        try {
            QueryParser qp = new QueryParser("id", new WhitespaceAnalyzer());
            Query q = qp.parse("id: " + id);
            Document doc =getDocument(q);
            if (doc != null) {
                result = new Hashtable<String, String>();
                List<Field> fields = doc.getFields();
                for (Field field : fields) {
                    result.put(field.name(), field.stringValue());
                }
                /*result.put("type", doc.get("type"));
                result.put("class", doc.get("class"));
                result.put("name", doc.get("name"));
                result.put("descr", doc.get("descr"));
                result.put("link", doc.get("link"));
                result.put("comment", doc.get("comment"));
                */
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return result;
    }

    public Document getDocument(Query query) throws IOException {
        Document doc = null;
        IndexSearcher searcher = new IndexSearcher(indexwriter.getReader());
        TopDocs topDocs = searcher.search(query,1);

        if (topDocs.totalHits > 0) {
            ScoreDoc[] scoreDosArray = topDocs.scoreDocs;
            doc = searcher.doc(scoreDosArray[0].doc);
        }
        searcher.close();

        return doc;
    }

    public synchronized boolean removeDocument (Query query) {
        //System.err.println("removeDocument " + query);
        boolean done = false;
        while (indexLock) {
            System.out.println("Index " + indexFile + " is locked, waiting...");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        indexLock = true;
        try {
            indexwriter.deleteDocuments(query);
            indexwriter.commit();
            if (indexwriter.hasDeletions()) {
                indexwriter.expungeDeletes();
                indexwriter.optimize();
                done = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        indexLock = false;
        return done;
    }

    public synchronized boolean addDocument (Document doc) throws IOException {
        while (indexLock) {
            System.out.println("Index " + indexFile + " is locked, waiting...");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        indexLock = true;
        indexwriter.addDocument(doc);
        indexwriter.commit();
        indexwriter.maybeMerge();
        indexwriter.optimize();
        indexLock = false;
        return true;
    }

    public synchronized String updateDocument (Document doc, String id) throws IOException {
        while (indexLock) {
            System.out.println("updateInstance to index waiting...");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        indexLock = true;
        //System.err.println(id + " " + doc.get("descnorm"));
        indexwriter.updateDocument(new Term("id", id), doc);
        indexwriter.maybeMerge();
        indexwriter.optimize();
        indexLock = false;
        return id;
    }

    protected  IndexWriter getIndexWriter () {
        return  indexwriter;
    }

    public Document[] searchDocuments (Query query) {
        Document[] result = new Document[0];

        try {
            // search
            //int hitsPerPage = 10;
            IndexSearcher searcher = new IndexSearcher(indexwriter.getReader());

            TopScoreDocCollector collector =
                    TopScoreDocCollector.create(searcher.maxDoc(), true);
            searcher.search(query, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            // display results
            //System.out.println(hits.length + " results.");
            result = new Document[hits.length];
            Document doc;
            for(int i=0; i < hits.length; i++) {
                doc = searcher.doc(hits[i].doc);
                result[i] = doc;
            }

            // close searcher
            searcher.close();
        } catch (CorruptIndexException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

}

