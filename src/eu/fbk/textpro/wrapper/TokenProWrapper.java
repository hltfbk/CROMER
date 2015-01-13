package eu.fbk.textpro.wrapper;

import eu.fbk.textpro.modules.tokenpro.TokenPro;

import javax.xml.bind.JAXBException;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 2-set-2013
 * Time: 8.01.52
 */
public class TokenProWrapper {
    private String sessionid;
    private String tokens;
    private byte[] page;

    public TokenProWrapper(String sessionid) {
        this.sessionid = sessionid;
    }

    public void analyze(String text, String lang) {
        StringBuffer tokens = new StringBuffer();
        File rawtextfile = new File("/tmp/" + sessionid + ".txt");
        OutputStreamWriter filein = null;
        try {
            filein = new OutputStreamWriter(new FileOutputStream(rawtextfile), "UTF8");
            filein.write(text);
            filein.close();

            TokenPro tokenpro = new TokenPro();
            String[] tokpara = {"-l",lang.toLowerCase().substring(0,3),"-c","token+tokenstart","-d","sentence"};
            tokenpro.init(tokpara);
            tokenpro.analyze(rawtextfile.getCanonicalPath(), rawtextfile.getCanonicalPath() + ".tok");

            File tokfile = new File(rawtextfile.getCanonicalPath() + ".tok");
            FileData filedatain = new FileData();
            filedatain.readData(tokfile.getCanonicalPath(), "UTF8");

            FileInputStream in = new FileInputStream(tokfile);
            page = new byte[in.available()];
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF8"));
            String line;
            String token = "";
            while((line=reader.readLine())!=null){
                if (line.length() > 0) {
                    if (!line.startsWith("# FIELDS:")) {
                        //System.out.println("TOKEN "+filedatain.getColumnValue(line,"tokenstart")+": " + filedatain.getColumnValue(line,"token"));
                        token = filedatain.getColumnValue(line,"token");
                        if (token.length() > 1 ||
                                (token.length() == 1 && Character.isLetterOrDigit(token.charAt(0)))) {
                            tokens.append(" ").append(token.toLowerCase());
                        } else {
                            tokens.append(" #");
                        }
                    }
                }
            }
            in.close();
            reader.close();

            DataInputStream datain = new DataInputStream (new FileInputStream(tokfile));
            datain.readFully (page);
            datain.close ();

            rawtextfile.delete();
            tokfile.delete();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        this.tokens = tokens.toString().replaceAll("( #)+"," #").trim();
    }

    public String getTokens() {
        return tokens;
    }

    public String getTxpFile() throws UnsupportedEncodingException {
        return new String(page, "UTF8");
    }


}
