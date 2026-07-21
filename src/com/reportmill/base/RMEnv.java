package com.reportmill.base;
import com.reportmill.graphics.*;
import snap.gfx.Font;
import snap.text.TextLineStyle;
import snap.text.TextModel;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

/**
 * A class to allow certain functionality to be pluggable depending on platform (desktop/web).
 */
public class RMEnv {

    // The shared instance
    private static RMEnv _shared = new RMEnv();

    /**
     * Returns a TextModel for the given html string and a default font.
     */
    public TextModel parseHTML(String html, Font baseFont, TextLineStyle textLineStyle)
    {
        return RMHTMLParser.parse(html, baseFont, textLineStyle);
    }

    /**
     * Returns a TextModel from the given rtf string and default font.
     */
    public TextModel parseRTF(String rtf, Font baseFont)
    {
        return RMRTFParser.parse(rtf, baseFont);
    }

    /**
     * Returns a list of maps for a given ResultSet.
     */
    public List<Map<String,Object>> getResultSetAsMaps(Object aResultSet, int aLimit)
    {
        ResultSet rs = (ResultSet) aResultSet;
        return RMSQLUtils.getMaps(rs, aLimit);
    }

    /**
     * Returns the method for given class and name that best matches given parameter types.
     */
    public Method getMethodBest(Class<?> aClass, String aName, Class<?>... theClasses)
    {
        return GetBestMethod.getBestMethod(aClass, aName, theClasses);
    }

    /**
     * Returns the shared instance.
     */
    public static RMEnv getEnv()  { return _shared; }
}
