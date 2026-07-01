/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import com.reportmill.shape.RMDocument;
import java.util.*;
import java.text.*;
import snap.util.*;

/**
 * This is just a SimpleDateFormat subclass to support RM archiving and legacy formats.
 */
public class RMDateFormat implements RMFormat, Cloneable {

    // The format
    SimpleDateFormat _fmt = new SimpleDateFormat();

    // The string to be substituted if asked to format null
    String _nullString = "<N/A>";

    // The local of the format
    Locale _locale;

    // Shared common formats
    public static RMDateFormat BASIC = new RMDateFormat("MM/dd/yyyy");
    public static RMDateFormat DEFAULT = new RMDateFormat("MMM dd, yyyy");
    public static RMDateFormat defaultFormat = DEFAULT;

    /**
     * Constructor.
     */
    public RMDateFormat()
    {
    }

    /**
     * Constructor for the given string format.
     */
    public RMDateFormat(String aFormat)
    {
        setPattern(aFormat);
    }

    /**
     * Returns the String that is substituted when this format is asked to provide stringForObjectValue(null).
     */
    public String getNullString()  { return _nullString; }

    /**
     * Sets the String that is substituted when this format is asked to provide stringForObjectValue(null).
     */
    public void setNullString(String aString)  { _nullString = aString; }

    /**
     * Returns the date format string.
     */
    public String getPattern()  { return _fmt.toPattern(); }

    /**
     * Sets the date format string. Has support for legacy RM formats and Java style.
     */
    public void setPattern(String aFormat)  { _fmt.applyPattern(aFormat); }

    /**
     * Formats the given object.
     */
    public String format(Object anObj)
    {
        // If locale hasn't been set, get it from RMDocument locale
        if (_locale != RMDocument._locale) {
            _locale = RMDocument._locale;
            _fmt.setDateFormatSymbols(new DateFormatSymbols(RMDocument._locale));
        }

        // If object is date, return date format
        if (anObj instanceof Date)
            return _fmt.format(anObj);

        // If object isn't date, just return null string
        return _nullString;
    }

    /**
     * Returns the time zone.
     */
    public TimeZone getTimeZone()  { return _fmt.getTimeZone(); }

    /**
     * Sets the time zone.
     */
    public void setTimeZone(TimeZone aTZ)
    {
        _fmt.setTimeZone(aTZ);
    }

    /**
     * Tries to parse a number from given string using this format.
     */
    public Date parse(String aStr)
    {
        try { return _fmt.parse(aStr); }
        catch (Exception e) { return null; }
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        if (anObj == this) return true;
        if (!getClass().isInstance(anObj)) return false;
        RMDateFormat other = (RMDateFormat) anObj;
        if (!Objects.equals(other._nullString, _nullString)) return false;
        return super.equals(anObj);
    }

    /**
     * Standard clone implementation.
     */
    public RMDateFormat clone()
    {
        RMDateFormat clone;
        try { clone = (RMDateFormat) super.clone(); }
        catch (CloneNotSupportedException e) { throw new RuntimeException(e); }
        clone._fmt = (SimpleDateFormat) _fmt.clone();
        return clone;
    }

    /**
     * XML archival.
     */
    @Override
    public XMLElement toXML(XMLArchiver anArchiver)
    {
        // Get new element named format
        XMLElement e = new XMLElement("format");
        e.add("type", "date");
        e.add("pattern", _fmt.toPattern());
        if (_nullString != null && !_nullString.isEmpty())
            e.add("null-string", _nullString);
        return e;
    }

    /**
     * XML unarchival.
     */
    @Override
    public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
    {
        _fmt.applyPattern(anElement.getAttributeValue("pattern"));
        _nullString = anElement.getAttributeValue("null-string");
        return this;
    }

    /**
     * Returns string representation of this format.
     */
    public String toString()  { return getPattern(); }
}