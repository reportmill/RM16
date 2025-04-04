package com.reportmill.base;

import com.reportmill.graphics.RMColor;
import com.reportmill.graphics.RMFont;
import com.reportmill.graphics.RMXString;
import com.reportmill.shape.RMDocument;
import com.reportmill.shape.RMParentShape;
import com.reportmill.shape.RMTextShape;
import snap.util.*;

/**
 * Perform checks.
 */
public class Voucher {

    // The current license string
    private static String  _license;

    // Whether RM found a valid license
    private static Boolean  _licensed;

    /**
     * Returns whether ReportMill has a valid license for the current user.
     */
    public static boolean isLicensed()
    {
        if (_licensed != null) return _licensed;
        return _licensed = checkString(getLicense(), ReportMill.isApp);
    }

    /**
     * Returns the ReportMill license string for the current user.
     */
    public static String getLicense()
    {
        // If already set, just return
        if (_license != null) return _license;

        // Get preferences for com.reportmill.Shell and prefs key (HostProperties1 for app, HostProperties2  for engine)
        try {
            String prefsKey = ReportMill.isApp ? "HostProperties1" : "HostProperties2";
            Prefs prefs = Prefs.getPrefsForName("/com/reportmill");
            _license = prefs.getString(prefsKey, null);
        }

        // Catch exceptions - in case security manager complains
        catch (Throwable t) {
            System.err.println("ReportMill.getLicense: Can't get license (" + t.getMessage() + ")");
        }
        return _license;
    }

    /**
     * Sets the ReportMill license string for the current user.
     */
    public static void setLicense(String aLicense)
    {
        setLicense(aLicense, false, ReportMill.isApp);
    }

    /**
     * Sets the ReportMill license string for the current user (with option to persist).
     */
    public static void setLicense(String aLicense, boolean isPersistent, boolean isApplication)
    {
        // If persistent, save license to preferences
        if (isPersistent) try {

            // Get preferences for com.reportmill.Shell and prefs key (HostProperties1 for app, HostProperties2  for engine)
            Prefs prefs = Prefs.getPrefsForName("/com/reportmill");
            String prefsKey = ReportMill.isApp ? "HostProperties1" : "HostProperties2";

            // Put license for prefs key (or remove if null) and flush preferences
            if (aLicense != null) prefs.setValue(prefsKey, aLicense);
            else prefs.remove(prefsKey);
            prefs.flush();
        }

        // Catch exceptions - in case security manager complains
        catch (Throwable t) {
            System.err.println("ReportMill.setLicense: Can't set license (" + t.getMessage() + ")");
        }

        // Set new license and determine if license is valid
        _license = aLicense;
        _licensed = checkString(getLicense(), ReportMill.isApp);
    }

    /**
     * Simple lc check.
     */
    public static void lc(RMDocument aDoc)
    {
        // If licenced, just return
        if (isLicensed() || SnapEnv.isTeaVM) return;

        // Add watermark/unlicensed message to each report page
        for (int i = 0; i < aDoc.getPages().size(); i++) addWatermark(aDoc.getPage(i));

        // If not app, print warning
        if (!ReportMill.isApp) {
            System.err.println("Warning: Unlicensed copy of ReportMill for host " + SnapUtils.getHostname() +
                    " and user " + System.getProperty("user.name") + " - call 214.513.1636 for license.");
            System.err.println("    Enter license with: \"java -cp /YourInstallDir/ReportMill.jar " +
                    "com.reportmill.Shell -license <license-string>\"");
            System.err.println("    Or call \"ReportMill.setLicense(\"<license_string>\") in app " +
                    "prior to report generation.");
            System.err.println("    This is only a warning (generated report will contain a watermark).");
        }
    }

    /**
     * This method adds a watermark to the given shape.
     */
    private static void addWatermark(RMParentShape aShape)
    {
        // Get attributed string with REPORTMILL in 72pt grey (with R & M in 100pt)
        RMFont font72 = RMFont.getFont("Arial Bold", 72), font100 = font72.copyForSize(100);
        RMXString xstring = new RMXString("REPORTMILL", font72, new RMColor(.9));
        xstring.setAttribute(font100, 0, 1);
        xstring.setAttribute(font100, 6, 7); // Set R & M in 100pt

        // Create evalShape watermark across background
        RMTextShape evalShape = new RMTextShape(xstring);
        evalShape.setFrame((aShape.getWidth() - 570) / 2, (aShape.getHeight() - 140) / 2, 570, 140);
        evalShape.setRoll(45);
        evalShape.setOpacity(.667f);
        aShape.addChild(evalShape, 0);

        // Get attributed string with bottom eval message in 12pt
        String msg = "ReportMill Evaluation - for more information go to reportmill.com.";
        xstring = new RMXString(msg, RMFont.Helvetica12);

        // Create evalShape license string in lower left corner
        evalShape = new RMTextShape(xstring);
        evalShape.setFrame(5, aShape.getHeight() - 20, 500, 18);
        evalShape.setURL("http://www.reportmill.com");
        aShape.addChild(evalShape);
    }

    /**
     * Checks a string to see if it's valid.
     */
    public static boolean checkString(String aString, boolean isApplication)
    {
        // If null string provided, just return
        if (aString == null) return false;

        // License is uppercase version of string - return if not long enough or has wrong prefix
        String license = aString.toUpperCase();

        // Get index of dash in license string
        int dash = license.indexOf("-");
        if (dash < 0) return false;

        // Get license prefix (substring before dash) and suffix (substring after dash)
        String prefix = license.substring(0, dash);
        String suffix = license.substring(dash + 1);

        // Decrypt suffix
        suffix = dString(suffix, "RMRules");

        // If prefix/suffix lengths not equal, return false
        if (prefix.length() != suffix.length()) return false;

        // Strip off first two characters of suffix
        suffix = suffix.substring(2);

        // If prefix doesn't start with suffix, return false
        if (!prefix.startsWith(suffix)) return false;

        // Decode processor count hex digit at second to last prefix char
        int procCount;
        try {
            String procCountStr = prefix.substring(prefix.length() - 2, prefix.length() - 1);
            procCount = Integer.parseInt(procCountStr, Character.MAX_RADIX);
        }
        catch (Exception e) {
            return false;
        }

        // If less than actual processor count, return false
        if ((isApplication && procCount != 0) || (!isApplication && procCount < SnapUtils.getProcessorCount())) {
            if (!isApplication) {
                System.err.println("Warning: License key not valid for CPU Count " + SnapUtils.getProcessorCount());
                System.err.println("Warning: License key CPU count is " + procCount);
            }
            return false;
        }

        // Decode version hex digit at last prefix char
        int version;
        try {
            String versionStr = prefix.substring(prefix.length() - 1);
            version = Integer.parseInt(versionStr, Character.MAX_RADIX);
        } catch (Exception e) {
            System.err.println("Warning: License key invalid format (2)");
            return false;
        }

        // If version is less than getVersion(), complain and return false
        if (version < 9) { //getVersion()) { Change back soon
            System.err.print("Warning: License not valid for RM " + ReportMill.getVersion());
            System.err.println(" (license valid for RM " + version + ")");
            return false;
        }

        // Return true since all checks passed
        return true;
    }

    /**
     * Decrypts aString with aPassword (takes hex string of form "AC5FDE" and returns human readable string).
     */
    private static String dString(String aString, String aPassword)
    {
        if (aString == null || aPassword == null) return null;

        // Get bytes for string and password
        byte[] string = ASCIICodec.decodeHex(aString);
        byte[] password = StringUtils.getBytes(aPassword);

        // XOR string bytes with password bytes
        for (int i = 0; i < string.length; i++)
            string[i] = (byte) (string[i] ^ password[i % password.length]);

        return StringUtils.getISOLatinString(string);
    }
}
