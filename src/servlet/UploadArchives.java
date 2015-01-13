package servlet;

import fbk.hlt.utility.archive.CatalogOfGzippedTexts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 20-ago-2013
 * Time: 11.23.52
 */
public class UploadArchives {
    private static UploadArchives singletonArchive;

    private static File ARCHIVEDIRNAME = null;

    private List<String> lockArchives = new ArrayList<String>();
    private Hashtable<String, CatalogOfGzippedTexts> newsArchives = new Hashtable<String, CatalogOfGzippedTexts>();


    private UploadArchives(String path) throws IOException {
        ARCHIVEDIRNAME = new File(path);
        if (!ARCHIVEDIRNAME.exists()) {
            if (!ARCHIVEDIRNAME.mkdirs()) {
                throw new IOException();
            }

        }
    }

    public static UploadArchives getInstance(String path) throws IOException {
        if (singletonArchive == null) {
            singletonArchive = new UploadArchives(path);
            singletonArchive.archiveLoader();
        }

        return singletonArchive;
    }

    public boolean addDocument (String name, String key, byte[] bytes) {
        while (lockArchives.contains(name)) {
            System.out.println("addDocument to "+name+" archive waiting...");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lockArchives.add(name);
        CatalogOfGzippedTexts cgt = getArchive(name);
        if (cgt == null) {
            newsArchives.remove(name);
        } else {
            cgt.add(key, bytes, null);
            System.out.println("> ADD ARCHIVE " +cgt.getFilepath() +" ["+cgt.size()+"]");
        }
        lockArchives.remove(name);

        return true;
    }

    public String[] getArchiveNames() {
        List<String> sortedKeys=new ArrayList<String>(newsArchives.keySet());

        Collections.sort(sortedKeys);
        String[] array = new String[sortedKeys.size()];
        int i = 0;
        for (String key : sortedKeys) {
            array[i] = key;
            i++;
        }

        return array;
    }


    public int getArchiveDocs (String name) {
        return getArchive(name).size();
    }

    private CatalogOfGzippedTexts getArchive (String name) {
        if (!newsArchives.containsKey(name)) {
            if (!addArchive(name, ARCHIVEDIRNAME+File.separator+name+".cgt")) {
                System.err.println("ERROR! Problems occured on archive " + ARCHIVEDIRNAME+File.separator+name+".cgt");
                return null;
            }
        }
        return newsArchives.get(name);
    }


    public boolean addArchive (String name, String path) {
        try {
            if (!newsArchives.containsKey(name)) {
                newsArchives.put(name, new CatalogOfGzippedTexts(path,"rw"));
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean removeArchive (String name) {
        if (newsArchives.containsKey(name)) {
            CatalogOfGzippedTexts cgt = newsArchives.get(name);
            cgt.close();
            newsArchives.remove(name);
            File path = new File(cgt.getFilepath());
            path.delete();
            path = new File(cgt.getFilepath().replaceFirst(".cgt$",".set"));
            path.delete();

            return true;
        }

        return false;
    }

    public String[] getUrls (String name) {
        CatalogOfGzippedTexts cgt = newsArchives.get(name);
        String[] urls;
        if (cgt != null) {
            urls = new String[cgt.size()];
            String key = cgt.firstKey();
            int i = 0;
            while(key != null) {
                urls[i] = key;
                i++;
                key = cgt.nextKey();
            }

        } else {
            urls = new String[0];
        }
        return urls;
    }

    public byte[] getBytes (String name, String url) {
        return newsArchives.get(name).getBytes(url);
    }

    public int archiveLoader () {
        newsArchives.clear();

        //News archive
        //loop on 1 levels directory
        String[] files = ARCHIVEDIRNAME.list();
        for (int i=0; i<files.length; i++) {
            if (files[i].endsWith(".cgt")) {
                if (addArchive(files[i].replaceFirst(".cgt$",""), ARCHIVEDIRNAME+File.separator+files[i])) {
                    System.out.println("LOAD UPLOAD ARCHIVE " + ARCHIVEDIRNAME +File.separator+ files[i]);
                }
            }
        }

        return newsArchives.size();
    }

}
