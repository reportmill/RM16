package reportmill.util;
import reportmill.out.RMExcelWriter;
import reportmill.shape.RMDoc;
import snap.gfx.Font;
import snap.text.RichText;
import snap.text.TextLineStyle;
import snap.util.ClassUtils;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Map;

/**
 * An RMEnv subclass that uses JVM desktop features when appropriate.
 */
public class RMEnvSwing extends RMEnv {

    /**
     * Returns a RichText for the given html string and a default font.
     */
    public RichText parseHTML(String html, Font baseFont, TextLineStyle aLineStyle)
    {
        return RMHTMLParser.parse(html, baseFont, aLineStyle);
    }

    /**
     * Returns an xstring from the given rtf string and default font.
     */
    public RichText parseRTF(String rtf, Font baseFont)
    {
        return RMRTFParser.parse(rtf, baseFont);
    }

    /**
     * Returns the document as byte array of an Excel file.
     */
    public byte[] getBytesExcel(RMDoc aDoc)
    {
        return new RMExcelWriter().getBytes(aDoc);
    }

    /**
     * Returns a list of maps for a given ResultSet.
     */
    public List<Map> getResultSetAsMaps(Object aResultSet, int aLimit)
    {
        ResultSet rs = (ResultSet) aResultSet;
        return RMSQLUtils.getMaps(rs, aLimit);
    }

    /**
     * Returns the method for given class and name that best matches given parameter types.
     */
    public Method getMethodBest(Class aClass, String aName, Class ... theClasses)
    {
        return ClassUtils.getMethodBest(aClass, aName, theClasses);
    }

    /**
     * This is here because TeaVM doesn't have this method yet.
     */
    public void setDecimalFormatSymbols(DecimalFormat aFormat, DecimalFormatSymbols newSymbols)
    {
        aFormat.setDecimalFormatSymbols(newSymbols);
    }
}
