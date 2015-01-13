package eu.fbk.textpro.wrapper;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 21/12/13
 * Time: 10.06
 */
public class CATWrapper {
    private String tokens;
    private String sessionid;
    private byte[] page;


    public CATWrapper(String sessionid) {
        this.sessionid = sessionid;
    }

    public void analyze(String text, String lang) {
        StringBuffer tokens = new StringBuffer();
        File rawtextfile = new File("/tmp/" + sessionid + ".txt");
        OutputStreamWriter tokfile = null;
        try {
            int tokenstart = 0;
            String[] lines = text.split("\n");
            tokfile = new OutputStreamWriter(new FileOutputStream(rawtextfile.getCanonicalPath() + ".tok"), "UTF8");
            tokfile.write("# FILE: \n# FIELDS: token\ttokenstart\n");
             int sentence = 1;
            for( String line : lines) {
                if (line.matches("<token t?_?id=.*</token>")) {
                    String token = line.replaceFirst("<\\/token>","").replaceFirst("<token t?_?id=.+>","");
                    //System.err.println(token+"\t"+tokenstart);
                    if (line.contains(" sentence=\""+ sentence +"\"") ) {
                        tokfile.write("\n");
                        sentence++;
                    }
                    tokfile.write(token+"\t"+tokenstart+"\n");

                    tokenstart = tokenstart + token.length()+1;
                    if (token.length() > 1 ||
                            (token.length() == 1 && Character.isLetterOrDigit(token.charAt(0)))) {
                        tokens.append(" ").append(token.toLowerCase());
                    } else {
                        tokens.append(" #");
                    }
                }
            }
            tokfile.close();

            FileInputStream in = new FileInputStream(new File(rawtextfile.getCanonicalPath() + ".tok"));
            page = new byte[in.available()];
            DataInputStream datain = new DataInputStream (in);
            datain.readFully (page);
            datain.close ();
            //System.err.println("PAGE: " + new String(page, "UTF8"));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.tokens = tokens.toString().replaceAll("( #)+"," #").trim();
    }

    public String getTokens() {
        return tokens;
    }

    public String getTxpFile() throws UnsupportedEncodingException {
        //System.err.println("PAGE: " + new String(page, "UTF8"));
        return new String(page, "UTF8");
    }
}
