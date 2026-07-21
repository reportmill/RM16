/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import com.reportmill.base.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.geom.HPos;
import snap.gfx.Font;
import snap.text.*;
import snap.util.*;

/**
 * An RMXString is like a String that lets you apply attributes, like fonts and colors, to character ranges. These
 * character ranges with common attributes are represented internally as the inner class Run.
 * <p>
 * You might use it like this:
 * <p><blockquote><pre>
 *    RMXString xstring = new RMXString("Hello World", Color.RED);
 *    xstring.addAttribute(RMFont.getFont("Arial Bold", 12), 0, 5);
 *    xstring.addAttribute(RMFont.getFont("Arial BoldItalic", 12), 6, xstring.length());
 * </pre></blockquote><p>
 */
public class RMXString implements Cloneable, CharSequence {

    // The TextModel
    private TextModel _textModel;

    /**
     * Constructor.
     */
    public RMXString()
    {
        _textModel = TextModel.createDefaultTextModel(true);
    }

    /**
     * Constructor.
     */
    public RMXString(TextModel textModel)
    {
        _textModel = textModel;
    }

    /**
     * Constructor.
     */
    public RMXString(CharSequence theChars)
    {
        this();
        addChars(theChars);
    }

    /**
     * Constructor.
     */
    public RMXString(CharSequence theChars, Object... theAttrs)
    {
        this();
        addChars(theChars, theAttrs);
    }

    /**
     * Returns the TextModel.
     */
    public TextModel getTextModel()  { return _textModel; }

    /**
     * Returns the simple String.
     */
    public String getText()  { return _textModel.getString(); }

    /**
     * The length.
     */
    public int length()  { return _textModel.length(); }

    /**
     * Returns the char at given index.
     */
    public char charAt(int anIndex)  { return _textModel.charAt(anIndex); }

    /**
     * Returns a subsequence.
     */
    public CharSequence subSequence(int aStart, int anEnd)  { return _textModel.subSequence(aStart, anEnd); }

    /**
     * Sets the simple String represented by this RMXString.
     */
    public void setText(String aString)
    {
        replaceChars(aString, 0, length());
    }

    /**
     * Returns the index within this string of the first occurrence of the given substring.
     */
    public int indexOf(String aString)
    {
        return toString().indexOf(aString);
    }

    /**
     * Returns the index within this string of first occurrence of given substring, starting at given index.
     */
    public int indexOf(String aString, int aStart)
    {
        return toString().indexOf(aString, aStart);
    }

    /**
     * Appends the given String to the end of this XString.
     */
    public void addChars(CharSequence theChars)
    {
        addChars(theChars, length());
    }

    /**
     * Adds chars at index.
     */
    public void addChars(CharSequence theChars, int anIndex)
    {
        addChars(theChars, null, anIndex);
    }

    /**
     * Appends the given string to this XString, with the given attributes, at the given index.
     */
    public void addChars(CharSequence theChars, TextStyle aStyle, int anIndex)
    {
        if (theChars.isEmpty()) return;
        _textModel.addCharsWithStyle(theChars, aStyle, anIndex);
    }

    /**
     * Appends the given chars with the given attribute(s).
     */
    public void addChars(CharSequence theChars, Object... theAttrs)
    {
        TextStyle textStyle = _textModel.getTextStyleForCharIndex(length());
        Object attr0 = theAttrs != null && theAttrs.length > 0 ? theAttrs[0] : null;
        if (attr0 instanceof TextStyle) textStyle = (TextStyle) attr0;
        else if (attr0 != null) textStyle = textStyle.copyForStyleValues(theAttrs);
        addChars(theChars, textStyle, length());
    }

    /**
     * Removes characters in given range.
     */
    public void removeChars(int aStart, int anEnd)
    {
        _textModel.removeChars(aStart, anEnd);
    }

    /**
     * Replaces chars in given range, with given String.
     */
    public void replaceChars(CharSequence theChars, int aStart, int anEnd)
    {
        _textModel.replaceCharsWithStyle(theChars, null, aStart, anEnd);
    }

    /**
     * Adds an XString to this string at given index.
     */
    public void addString(RMXString xStr, int anIndex)
    {
        _textModel.addCharsForTextModel(xStr._textModel, anIndex);
    }

    /**
     * Replaces the chars in given range, with given XString.
     */
    public void replaceString(RMXString xStr, int aStart, int aEnd)
    {
        _textModel.removeChars(aStart, aEnd);
        _textModel.addCharsForTextModel(xStr._textModel, aStart);
    }

    /**
     * Returns the XString run that contains or ends at given index.
     */
    public TextRun getRunForCharRange(int startIndex, int endIndex)
    {
        return _textModel.getRunForCharRange(startIndex, endIndex);
    }

    /**
     * Applies the given attribute to whole xstring, assuming it's a basic attr types (font, color, etc.).
     */
    public void setAttribute(Object anAttr)
    {
        setAttribute(anAttr, 0, length());
    }

    /**
     * Applies the given attribute to the given character range, assuming it's a basic attr type (font, color, etc.).
     */
    public void setAttribute(Object anAttr, int aStart, int anEnd)
    {
        String key = TextStyle.getStyleKey(anAttr);
        if (key != null) setAttribute(key, anAttr, aStart, anEnd);
    }

    /**
     * Adds a given attribute of given type to the whole string.
     */
    public void setAttribute(String aKey, Object anAttr)
    {
        setAttribute(aKey, anAttr, 0, length());
    }

    /**
     * Sets a given attribute to a given value for a given range.
     */
    public void setAttribute(String aKey, Object aVal, int aStart, int aEnd)
    {
        _textModel.setTextStyleValue(aKey, aVal, aStart, aEnd);
    }

    /**
     * Returns the current font at the given character index.
     */
    public Font getFontAt(int anIndex)
    {
        return _textModel.getRunForCharIndex(anIndex).getFont();
    }

    /**
     * Returns the text line style at the given character index.
     */
    public TextLineStyle getLineStyleForCharIndex(int anIndex)
    {
        TextLine line = _textModel.getLineForCharIndex(anIndex);
        return line.getLineStyle();
    }

    /**
     * Returns the text line style at the given character index.
     */
    public void setLineStyle(TextLineStyle lineStyle, int aStart, int anEnd)
    {
        _textModel.setLineStyle(lineStyle, aStart, anEnd);
    }

    /**
     * Sets the xstring to be underlined.
     */
    public void setUnderlined(boolean aFlag)
    {
        setAttribute(TextStyle.Underline_Prop, aFlag ? 1 : null, 0, length());
    }

    /**
     * Returns an XString for given char range.
     */
    public RMXString substring(int aStart, int aEnd)
    {
        TextModel richTextCopy = _textModel.copyForRange(aStart, aEnd);
        return new RMXString(richTextCopy);
    }

    /**
     * Standard Object equals implementation.
     */
    public boolean equals(Object anObj)
    {
        if (anObj == this) return true;
        RMXString other = anObj instanceof RMXString ? (RMXString) anObj : null;
        if (other == null) return false;
        return other._textModel.equals(_textModel);
    }

    /**
     * Returns a clone of this RMXString.
     */
    public RMXString clone()
    {
        RMXString clone;
        try { clone = (RMXString) super.clone(); }
        catch (Exception e) { throw new RuntimeException(e); }
        clone._textModel = _textModel.copyForRange(0, _textModel.length());
        return clone;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        return _textModel.getString();
    }

    /**
     * Performs @key@ substitution on an xstring.
     */
    public RMXString rpgClone(ReportOwner rptOwner, Object userInfo, RMShape aShape, boolean doCopy)
    {
        // Declare local variable for resulting out-xstring and for whether something requested a recursive RPG run
        RMXString outString = this;
        boolean redo = false;

        // If userInfo provided, plug it into ReportMill
        if (userInfo != null && rptOwner != null)
            rptOwner.pushDataStack(userInfo);

        // Get range for first key found in string
        Range totalKeyRange = outString.nextKeyRangeAfterIndex(0, new Range());

        // While the inString still contains @key@ constructs, do substitution
        while (totalKeyRange.length() > 0) {

            // Get key start location (after @-sign) and length
            int keyStart = totalKeyRange.start + 1;
            int keyEnd = totalKeyRange.end - 1;
            Object valString = null;

            // Get the run at the given location
            TextRun keyRun = outString.getRunForCharRange(keyStart, keyEnd);

            // If there is a key between the @-signs, evaluate it for substitution string
            if (keyEnd > keyStart) {

                // Get actual key string
                String keyString = outString.subSequence(keyStart, keyEnd).toString();

                // Get key string as key chain
                RMKeyChain keyChain = RMKeyChain.getKeyChain(keyString);

                // If keyChain hasPageReference, tell reportMill and skip this key
                if (aShape != null && keyChain.hasPageReference()) {
                    rptOwner.addPageReferenceShape(aShape);
                    outString.nextKeyRangeAfterIndex(totalKeyRange.end, totalKeyRange);
                    continue;
                }

                // Get keyChain value
                Object val = RMKeyChain.getValue(rptOwner, keyChain);

                // If val is list, replace with first value (or null)
                if (val instanceof List<?> list)
                    val = !list.isEmpty() ? list.get(0) : null;

                // If we found a String, then we'll just use it for key sub (although we to see if it's a KeyChain literal)
                if (val instanceof String) {

                    // Set string value to be substitution string
                    valString = val;

                    // If keyChain has a string literal, check to see if val is that string literal
                    if (keyChain.hasOp(RMKeyChain.Op.Literal) && !StringUtils.startsWithIC((String) val, "<html")) {
                        String string = val.toString();
                        int index = keyString.indexOf(string);

                        // If val is that string literal, get original xstring substring (with attributes)
                        if (index > 0 && keyString.charAt(index - 1) == '"' && keyString.charAt(index + string.length()) == '"') {
                            int start = index + keyStart;
                            valString = outString.substring(start, start + string.length());
                            redo = redo || string.contains("@");
                        }
                    }
                }

                // If we found an xstring, then we'll just use it for key substitution
                else if (val instanceof RMXString)
                    valString = val;

                    // If we found a keyChain, add @ signs and redo (this feature lets developers return an RMKeyChain)
                else if (val instanceof RMKeyChain) {
                    valString = "@" + val + "@";
                    redo = true;
                }

                // If val is Number, get format and change val to string (verify format type)
                else if (val instanceof Number) {
                    TextFormat format = keyRun.getFormat();
                    if (!(format instanceof RMNumberFormat))
                        format = RMNumberFormat.PLAIN;
                    valString = format.format(val);
                    TextStyle style = format.formatStyle(val);
                    if (style != null)
                        valString = new RMXString((String) valString, style.getColor());
                }

                // If val is Date, get format and change val to string (verify format type)
                else if (val instanceof Date) {
                    TextFormat format = keyRun.getFormat();
                    if (!(format instanceof RMDateFormat))
                        format = RMDateFormat.defaultFormat;
                    valString = format.format(val);
                }

                // If value is null, either use current format's or Document's NullString
                else if (val == null) {
                    TextFormat fmt = keyRun.getFormat();
                    if (fmt != null)
                        valString = fmt.format(val);
                }

                // If object is none of standard types (Str, Num, Date, XStr or null), see if it will provide bytes
                else {

                    // Ask object for "bytes" method or attribute
                    Object bytes = RMKey.getValue(val, "bytes");

                    // If bytes is byte array, just set it
                    if (bytes instanceof byte[])
                        valString = new String((byte[]) bytes);

                        // If value is List, reset it so we don't get potential hang in toString
                    else if (val instanceof List)
                        valString = "<List>";

                        // If value is Map, reset to "Map" so we don't get potential hang in toString
                    else if (val instanceof Map)
                        valString = "<Map>";

                        // Set substitution value to string representation of provided object
                    else valString = val.toString();
                }

                // If substitution string is still null, replace it with document null-string
                if (valString == null)
                    valString = rptOwner.getNullString() != null ? rptOwner.getNullString() : "";
            }

            // If there wasn't a key between '@' signs, assume they wanted '@'
            else valString = "@";

            // If substitution string was found, perform substitution
            if (valString != null) {

                // If this is the first substitution, get a copy of outString
                if (outString == this && doCopy)
                    outString = clone();

                // If substitution string was raw string, perform replace (and possible rtf/html evaluation)
                if (valString instanceof String string) {

                    // If string is HTML formatted text, parse into RMXString
                    if (StringUtils.startsWithIC(string, "<html")) {
                        TextModel textModel = RMEnv.getEnv().parseHTML(string, keyRun.getFont(), keyRun.getLine().getLineStyle());
                        valString = new RMXString(textModel);
                    }

                    // If string is RTF formatted text, parse into RMXString
                    else if (string.startsWith("{\\rtf")) {
                        TextModel textModel = RMEnv.getEnv().parseRTF(string, keyRun.getFont());
                        valString = new RMXString(textModel);
                    }

                    // If string is normal string, just perform replace and update key range
                    else {
                        outString.replaceChars(string, totalKeyRange.start, totalKeyRange.end);
                        totalKeyRange.setLength(string.length());
                    }
                }

                // If substitution string is xstring, just do xstring replace
                if (valString instanceof RMXString xstring) {
                    outString.replaceString(xstring, totalKeyRange.start, totalKeyRange.end);
                    totalKeyRange.setLength(xstring.length());
                }
            }

            // Get next totalKeyRange
            outString.nextKeyRangeAfterIndex(totalKeyRange.end, totalKeyRange);
        }

        // If userInfo was provided, remove it from ReportMill
        if (userInfo != null)
            rptOwner.popDataStack();

        // If something requested a recursive RPG run, do it
        if (redo)
            outString = outString.rpgClone(rptOwner, userInfo, aShape, false);

        // Return RPG string
        return outString;
    }

    /**
     * Returns the range of the next occurrence of @delimited@ text.
     */
    private Range nextKeyRangeAfterIndex(int anIndex, Range aRange)
    {
        // Get length of string (return bogus range if null)
        int length = length();
        if (length < 2)
            return aRange.set(-1, -1);

        // Get start of key (return if it is the last char)
        int startIndex = indexOf("@", anIndex);
        if (startIndex == length - 1) return aRange.set(startIndex, startIndex + 1);

        // If startRange of key was found, look for end
        if (startIndex >= 0) {
            int nextIndex = startIndex;
            while (++nextIndex < length) {
                char c = charAt(nextIndex);
                if (c == '"')
                    while ((++nextIndex < length) && (charAt(nextIndex) != '"')) ;
                else if (c == '@')
                    return aRange.set(startIndex, nextIndex + 1);
            }
        }

        // Set bogus range and return
        return aRange.set(-1, -1);
    }

    /**
     * A range class.
     */
    private static class Range {

        // Start/end
        int start, end;

        /** Constructor. */
        public Range()  { }

        /** Returns the range length */
        public int length()  { return end - start; }

        /** Sets the range length. */
        public void setLength(int aLength)  { end = start + aLength; }

        public Range set(int aStart, int anEnd)
        {
            start = aStart;
            end = anEnd;
            return this;
        }
    }
}