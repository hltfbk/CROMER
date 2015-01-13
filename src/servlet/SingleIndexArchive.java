package servlet;

import fbk.hlt.utility.archive.CatalogOfGzippedTexts;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 30-ago-2013
 * Time: 11.23.52
 */
 class SingleIndexArchive {
    private static SingleIndexArchive singletonArchive;

    private static String ARCHIVENAME = "archive.cgt";

    private List<String> lockArchives = new ArrayList();
    static private Hashtable<String, CatalogOfGzippedTexts> newsarchives = new Hashtable<String,CatalogOfGzippedTexts>();


    static public int indexLoader (String path) {
        int num = 0;
        File repoDir = new File(path);
        if (!repoDir.exists()) {
            repoDir.mkdirs();
        }

        //News archive
        //loop on 2 levels directory
        if (repoDir.isDirectory()) {
            String[] subDirs = repoDir.list();
            for (int i=0; i<subDirs.length; i++) {
                File sectionDir = new File(repoDir, subDirs[i] + File.separator);
                String[] listRepos = sectionDir.list();
                for (int l=0; l<listRepos.length; l++) {
                    File indexDir = new File(sectionDir+File.separator+listRepos[l]);
                    if (indexDir.isDirectory()) {
                        CatalogOfGzippedTexts cgt = null;
                        try {
                            cgt = new CatalogOfGzippedTexts(indexDir+ File.separator+ARCHIVENAME,"rw");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (cgt != null) {
                            newsarchives.put(subDirs[i]+File.separator+listRepos[l], cgt);
                            System.out.println("LOAD ARCHIVE " + cgt.getFilepath());
                            num++;
                        }
                    }
                }
            }
        }

        return num;
    }

}
