package eu.fbk.textpro.wrapper;

import org.apache.lucene.analysis.*;

import java.io.Reader;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 5-set-2013
 * Time: 0.57.44
 */
public class NormalizedWhitespaceAnalyzer extends Analyzer {
    // private static final Logger LOG = Logger.getLogger(IO.class);
    private final static Pattern pattern = Pattern
            .compile("\\p{InCombiningDiacriticalMarks}+");

    @Override
    public TokenStream tokenStream(String s, Reader reader) {
        TokenStream result = new WhitespaceTokenizer(reader);
        result = new LowerCaseFilter(result);
        result = new ISOLatin1AccentFilter(result);
        return result;
    }

    /**
     * lowercases and removes diacritics, see http://stackoverflow.com/questions/1008802/converting-symbols-accent-letters-to-english-alphabet
     * @param str
     * @return
     */
    public static String normalize(final String str) {
        String norm = Normalizer.normalize(str.toLowerCase(), Normalizer.Form.NFD);
        return pattern.matcher(norm).replaceAll("");
    }
}
