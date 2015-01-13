package servlet.parser;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 23/05/14
 * Time: 19.39
 */
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CATParser {
    SAXHandler handler = new SAXHandler();

    public CATParser(String xml) throws Exception {
        SAXParserFactory parserFactor = SAXParserFactory.newInstance();
        SAXParser parser = parserFactor.newSAXParser();
        //parser.parse(ClassLoader.getSystemResourceAsStream("xml/cat.xml"),handler);
       try  {
           parser.parse(new InputSource(new StringReader(xml)),handler);
       } catch (Exception e) {
           System.err.println(xml + "\n\nERROR! " + e.getMessage());
       }

        //Printing the list of employees obtained from XML
        /*
        for ( Markable markable : handler.markableList){
          System.err.println(markable);
        }
        */
    }

    public int getLastMID() {
        return handler.lastMID;
    }

    public int getLastRID() {
        return handler.lastRID;
    }

    public List<String> getTokenList() {
        return handler.tokenList;
    }

    public List<String> getTargetList () {
        return handler.targetList;
    }

    public List<Markable> getMarkableList() {
        return handler.markableList;
    }

    public Hashtable<String, Integer> getMarkables() {
        return handler.markables;
    }

    public String getMarkableTokenIDs (Integer id) {
        for (String key : getMarkables().keySet()) {
            if (getMarkables().get(key) == id) {
                return key;
            }
        }
        return "";
    }

    public String getTokens (String[] ids) {
        String tokens = "";
        for (String id : ids) {
            if (id.matches("[0-9]+"))
                tokens += handler.tokenList.get(Integer.valueOf(id)-1) + " ";
            else
                System.err.println("ERROR! Token ID is empty.");
        }
        return tokens.trim();
    }
}
/**
 * The Handler for SAX Events.
 */
class SAXHandler extends DefaultHandler {
    int lastMID = 0;
    int lastRID = 0;
    int source_relation=0;
    List<String> tokenList = new ArrayList<String>();
    List<Markable> markableList = new ArrayList<Markable>();
    Hashtable<String, Integer> markables = new Hashtable<String, Integer>();

    List<String> targetList = new ArrayList<String>();
    List<String> tag_descriptors = new ArrayList<String>();
    boolean inMarkable = false;

    Markable mark = null;
    private String content = null;
    private String elname = null;
    @Override
    //Triggered when the start of tag is found.
    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
            throws SAXException {
        //System.err.println(">>" + qName + " " +attributes.getIndex("r_id"));
        if (qName.equalsIgnoreCase("markables")) {
            inMarkable=true;
            return;
        }
        if (inMarkable) {
            if (attributes.getIndex("m_id") >= 0) {
                elname = qName;
                mark = new Markable();
                mark.mid = attributes.getValue("m_id");
                if (Integer.valueOf(mark.mid) > lastMID) {
                    lastMID = Integer.valueOf(mark.mid);
                }
                //tag_descriptor
                if (attributes.getIndex("TAG_DESCRIPTOR") >= 0) {
                    tag_descriptors.add(mark.mid);
                }
            }
        } else {
            if (attributes.getIndex("r_id") >= 0) {
                if (Integer.valueOf(attributes.getValue("r_id")) > lastRID) {
                    lastRID = Integer.valueOf(attributes.getValue("r_id"));
                }
            }
        }

        if (mark !=null) {
            //System.err.println(mark.mid + "->" + qName +".equals(\"token_anchor\") " +attributes.getIndex("t_id")+ " "+ mark.tokens);
            if (qName.equals("token_anchor") && attributes.getIndex("t_id") >= 0) {
                mark.tokens += attributes.getValue("t_id") + " ";
            } else if (qName.equalsIgnoreCase("describes")) {
                source_relation=0;
            } else if (qName.equalsIgnoreCase("source")) {
                source_relation++;
            } else if (qName.equalsIgnoreCase("target") && source_relation > 1) {
                targetList.add(String.valueOf(attributes.getValue("m_id")));
            }
        }
    }

    @Override
    public void endElement(String uri, String localName,
                           String qName) throws SAXException {
        if (qName.equals("token")) {
            tokenList.add(content);
        } else if (qName.equalsIgnoreCase("markables")) {
            inMarkable=false;
        }
        if (qName.equalsIgnoreCase(elname) && mark.tokens.trim().length()>0) {
            markables.put(mark.tokens.trim(), Integer.valueOf(mark.mid));
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        content = String.copyValueOf(ch, start, length).trim();
    }

}

class Markable {
    String mid;
    String classname;
    String naming;
    String tokens = "";

    @Override
    public String toString() {
        return classname + " " + naming + "(" + mid + ")" + tokens;
    }
}