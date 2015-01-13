package eu.fbk.textpro.wrapper;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class FileData {
    public static boolean isRawDataFile = true;
    public static LinkedHashMap<String, Integer> tokensIndex = new LinkedHashMap<String, Integer>();
    public static LinkedHashMap<String, String> headerList = new LinkedHashMap<String, String>();
    String headerStartPrefix = "# FILE:";
    static String headerEndPrefix = "# FIELDS:";

    public static void resetFileData(){
        tokensIndex.clear();
        headerList.clear();
        isRawDataFile = true;

    }
    public void readData(String file, String encoding) {
        try {
            String line;
            Reader reader = new InputStreamReader(new FileInputStream(file),
                    encoding);
            BufferedReader br = new BufferedReader(reader);
            boolean headerStartFound = false, headerEndFound = false;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(headerStartPrefix)) {
                    updateHeaderList(line);
                    headerStartFound = true;
                } else if (line.startsWith(headerEndPrefix)) {
                    if (line.length() > headerEndPrefix.length() + 1) {
                        isRawDataFile = false;
                        setTokensIndex(line);
                    }
                    updateHeaderList(line);
                    headerEndFound = true;
                } else if (line.startsWith("# ") && line.length() > 5) {
                    updateHeaderList(line);
                } else {
                    break;
                }

                // line;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // get field value givin a line
    public String getColumnValue(String line, String fieldname) {
        Integer index = -1;
        if(tokensIndex.containsKey(fieldname))
            index = tokensIndex.get(fieldname);
        else{
            System.err.println( "Column named "+fieldname+ " not found!");
            System.exit(-1);
        }
        String[] lineToks = line.split("\t");
        if(lineToks.length>= index&& index >-1)
            return lineToks[index];
        else{
            System.err.println( "The input line values is not aligned with the number of columns which we have!.\nWe couldn't provide a value of "+fieldname+" column");
            System.exit(0);
            return null;
        }
    }

    public String getHeaderValue(String header) {
        if (headerList.containsKey(header)) {
            return headerList.get(header);
        } else {
            System.err.println("We couldn't find a value to the " + header
                    + " header");
        }
        return "";
    }

    private void setTokensIndex(String line) throws IOException {
        tokensIndex.clear();
        updateHeaderList(line);
        if(line.startsWith(headerEndPrefix)&&line.length()>headerEndPrefix.length()){
            line = line.replaceFirst(headerEndPrefix + "\\s*", "");
            String[] cols = line.split("\t");
            for (int i = 0; i < cols.length; i++) {
                // System.err.println(cols[i]+","+ i);
                if(!tokensIndex.containsKey(cols[i]))
                    tokensIndex.put(cols[i], i);
            }
        }
    }

    public static void printTokenIndexs() {
        Iterator<String> ti = tokensIndex.keySet().iterator();
        while (ti.hasNext()) {
            String titmp = ti.next();
            System.out.println(titmp + "=>" + tokensIndex.get(titmp));
        }
    }

    public static void printHeaderList() {
        Iterator<String> ti = headerList.keySet().iterator();
        while (ti.hasNext()) {
            String titmp = ti.next();
            System.out.println(titmp + "=>" + headerList.get(titmp));
        }
    }

    public static void updateHeaderList(String headerLine) {
        // we are sure here that the headerLine is a from the header we don't
        // need to check that again.
        String key = "";
        String minimumHeader ="# : " ;
        if(headerLine.length() > minimumHeader.length()+2){
            key = headerLine.substring(0, headerLine.indexOf(":") + 1);
            headerList.put(key, headerLine);
        }else{
            System.err.println("We couldn't update the header of this line:\n"+headerLine);
        }
    }

}
