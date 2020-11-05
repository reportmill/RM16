package reportmill.util;
import reportmill.shape.RMDoc;
import snap.gfx.Font;
import snap.text.RichText;
import snap.text.TextLineStyle;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Map;

/**
 * A class to allow certain functionality to be pluggable depending on platform (desktop/web).
 */
public class RMEnv {

    // The shared instance
    private static RMEnv  _shared;

    /**
     * Returns a RichText for the given html string and a default font.
     */
    public RichText parseHTML(String html, Font baseFont, TextLineStyle aLineStyle)
    {
        System.err.println("RMEnv.parseHTML: Not implemented");
        return null;
    }

    /**
     * Returns an xstring from the given rtf string and default font.
     */
    public RichText parseRTF(String rtf, Font baseFont)
    {
        System.err.println("RMEnv.parseRTF: Not implemented");
        return null;
    }

    /**
     * Returns the document as byte array of an Excel file.
     */
    public byte[] getBytesExcel(RMDoc aDoc)
    {
        System.err.println("RMEnv.getBytesExcel: Not implemented");
        return null;
    }

    /**
     * Returns a list of maps for a given ResultSet.
     */
    public List<Map> getResultSetAsMaps(Object aResultSet, int aLimit)
    {
        System.err.println("RMEnv.getResultSetAsMaps: Not implemented");
        return null;
    }

    /**
      * Returns the method for given class and name that best matches given parameter types.
      */
    public Method getMethodBest(Class aClass, String aName, Class ... theClasses)
    {
        System.err.println("RMEnv.getMethodBest: Not implemented");
        return null;
    }

    /**
     * This is here because TeaVM doesn't have this method yet.
     */
    public void setDecimalFormatSymbols(DecimalFormat aFomat, DecimalFormatSymbols newSymbols)  { }

    /**
     * Returns the shared instance.
     */
    public static RMEnv getEnv()
    {
        if (_shared!=null) return _shared;
        return _shared = new RMEnv();
    }
}
