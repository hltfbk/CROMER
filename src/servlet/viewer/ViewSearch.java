package servlet.viewer;

import eu.fbk.textpro.modules.tokenpro.NormalizeText;
import eu.fbk.textpro.wrapper.NormalizedWhitespaceAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 4-set-2013
 * Time: 7.52.23
 */
public class ViewSearch {
    private static int exactquerynumterms = 1;
    private static Analyzer analyzer = new NormalizedWhitespaceAnalyzer();
    private static NormalizeText normText = new NormalizeText();
    
    public static Vector getDocSnippet (String[] tokens, String[] tokenstarts, String[] sentencestarts, int num_snippet, String query) throws IOException {
        Vector snippets = new Vector();
        //System.err.println(fileposition + ":LISTSTART " + liststart);

        //if (pattern != null) {
        //Field descr = doc.getField("descr");
        Pattern[] pattern = null;
        if (query != null && query.length() > 0) {
            pattern = createPattern(query);
        }
        StringBuffer currentPhrase = new StringBuffer();
        boolean showPhrase = false;
        //String[] eos = (doc.getField("eos").stringValue()).split(" ");
        //int endEosId = Integer.parseInt(eos[eosNum]);
        int textposition=0;
        int startposition = 0;
        List currentendposition = Arrays.asList(sentencestarts);
        Queue termqueue = new Queue();
        Queue tokenqueue = new Queue();
        String space;


        boolean foundentity = false;
        int i = 0;
        for (String term : tokens) {
            //System.err.println(":TERM " + term);

            space = "";

            try {
                startposition = Integer.valueOf(tokenstarts[i]);
                if (textposition != 0 && textposition != startposition) {
                    space= " ";
                }
                textposition = startposition+term.length();
            } catch (NumberFormatException e) {
                space = " ";
            }


            //System.out.println("liststart " + liststart);
            if (pattern != null) {
                //ricerca testuale
                //System.out.println("#"+splitline[0]+"#"+splitline[5]+"#"+splitline[4]);

                //termqueue.enqueue("#"+splitline[0]+"#"+splitline[6]+"#"+splitline[5]);
                termqueue.enqueue("#"+term+"#");
                tokenqueue.enqueue(space+term);

                if (matcher(pattern,termqueue.toString())) {
                    showPhrase = true;
                    while (!termqueue.isEmpty()) {
                        termqueue.dequeue();
                        if (matcher(pattern,termqueue.toString())) {
                            currentPhrase.append(tokenqueue.dequeue());
                        } else {
                            currentPhrase.append("<font color=red><b>").append(tokenqueue.toString()).append("</b></font>");
                            termqueue.clear();
                        }
                    }
                    tokenqueue.clear();
                }
                if (tokenqueue.size() >= exactquerynumterms) {
                    currentPhrase.append(tokenqueue.dequeue());
                    termqueue.dequeue();
                }

            }
            if (currentendposition.contains(startposition)) {
                if (showPhrase) {
                    if (!tokenqueue.isEmpty())
                        currentPhrase.append(tokenqueue);
                    snippets.add(currentPhrase.toString());
                    if (snippets.size() == num_snippet) {
                        return snippets;
                    }
                }
                showPhrase = false;
                currentPhrase.setLength(0);
                termqueue.clear();
                tokenqueue.clear();
                //eosNum++;
            }
            i++;
        }
        if (foundentity) {
            currentPhrase.append("</b></font>");
        }

        //}
        return snippets;
    }

    private static Pattern[] createPattern (String queryStr) {
        Vector patterns = new Vector();

        String patternstr = "";

        //System.err.println("News doQuery: " + queryStr );
        try {
            QueryParser qp = new QueryParser(queryStr, analyzer);
            Query iquery = qp.parse(NormalizedWhitespaceAnalyzer.normalize(normText.normalize(queryStr)));

            BooleanQuery query = new BooleanQuery();
            query.add(iquery, BooleanClause.Occur.SHOULD);

            //extract the right patterns
            int exactquery = 0;
            String field = "";
            String[] terms = queryStr.split("[\\s|\\+]+");

            //System.out.println("QUERY PARSE: " + qtv.size() + " -- " + terms[1].substring(terms[1].indexOf(":") + 1));
            int countterm = 0;
            String term;
            for (int t=0; t<terms.length; t++) {
                if (terms[t].equalsIgnoreCase("&&") || terms[t].equalsIgnoreCase("||")) {
                    continue;
                }
                if (terms[t].indexOf(":") > 0) {
                    field = terms[t].substring(0,terms[t].indexOf(":"));
                    term = terms[t].substring(terms[t].indexOf(":") + 1);
                } else {
                    term = terms[t];
                }

                //System.out.println("QUERY PARSE: " + terms.length + " "  +  terms[t]);
                if (term.indexOf("\"") == 0 && exactquery == 0) {
                    exactquery = 1;
                } else if (term.endsWith("\"") && exactquery == 1) {
                    exactquery = 2;
                }
                term = term.replaceAll("\"","");
                term = term.replaceAll("\\*","\\.\\*");
                if (term.length() > 0) {
                    //Term term = new Term(field,term.substring(term.indexOf(":") + 1).replaceAll("\\*","\\.\\*"));
                    countterm++;
                    if (field.equalsIgnoreCase("token")) {
                        patternstr += "|#" + term + "#.*";
                    } else if (field.equalsIgnoreCase("tokenpos")) {
                        patternstr += "|" + term.substring(0,term.indexOf("#",1)) + "#.*" + term.substring(term.lastIndexOf("#")) + "#.*";
                    } else if (field.equalsIgnoreCase("lemmapos")) {
                        patternstr += "|#.*" + term;
                    } else if (field.equalsIgnoreCase("entity")) {
                        patternstr += "|.*#" + term + "#.*";
                    } else {
                        patternstr += "|.*#" + terms[t] + "#.*";
                    }
                }
                if (exactquery == 2) {
                    patternstr = patternstr.replaceAll("\\|","");
                    patterns.add(".*" + patternstr);
                    patternstr = "";
                    if (countterm > exactquerynumterms)
                        exactquerynumterms = countterm;
                    countterm = 0;
                }
                //System.out.println("# " + term + " ->"  + patternstr);
            }



        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        if (patternstr.length() > 0) {
            patterns.add(patternstr.substring(1));
        }


        Pattern[] pattern = new Pattern[patterns.size()];
        //System.err.println("PATTERNS: " + patterns);
        for (int i=0; i<patterns.size();i++) {
            pattern[i] = Pattern.compile((String) patterns.get(i) ,Pattern.CASE_INSENSITIVE);
        }
        return pattern;
    }

    private static boolean matcher (Pattern[] pattern, String str) {
        if (pattern != null) {
            for (int i=0; i<pattern.length;i++) {
                //System.out.println(pattern[i] + " , " + str);
                //Pattern.matches(pattern[i], str)
                if (pattern[i].matcher(str).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

}
