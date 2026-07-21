/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.*;
import com.reportmill.graphics.*;
import snap.geom.*;
import snap.gfx.*;
import snap.text.*;
import snap.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for some esoteric text functionality.
 */
public class RMTextShapeUtils {

    /**
     * Returns an RMPolygon shape with the glyph path for the chars in this text. Assumes all runs have same visual attrs.
     */
    public static RMPolygonShape getTextPathShape(RMTextShape aText)
    {
        // Create polygon for text path with attributes from text shape
        Shape charsShape = getTextOutlineShape(aText);
        RMPolygonShape polygon = new RMPolygonShape(charsShape);
        polygon.copyShape(aText);

        // Set polygon color to run or outline color and stroke and return
        polygon.setColor(aText.getTextColor());
        Border brdr = aText.getTextBorder();
        polygon.setStroke(brdr != null ? new RMStroke(brdr.getColor(), brdr.getWidth()) : null);
        return polygon;
    }

    /**
     * Returns a path for all text chars.
     */
    public static Shape getTextOutlineShape(RMTextShape aText)
    {
        // Create path and establish bounds of text
        Path2D outlineShape = new Path2D();
        outlineShape.moveTo(0, 0);
        outlineShape.moveTo(aText.getWidth(), aText.getHeight());

        // Iterate over text runs
        TextLayout textLayout = aText.getTextLayout();
        for (TextLine line : textLayout.getLines()) {
            for (TextRun run : line.getRuns()) { //if(run.length()==0 || run.isTab()) continue;
                String str = run.getString();
                Font font = run.getFont();
                double charSpacing = run.getTextStyle().getCharSpacing();
                Shape runOutlineShape = font.getOutline(str, run.getX(), line.getBaseline(), charSpacing);
                outlineShape.appendShape(runOutlineShape);
            }
        }

        // Return
        return outlineShape;
    }

    /**
     * Returns a group shape with a text shape for each individual character in this text shape.
     */
    public static RMShape getTextCharsShape(RMTextShape aText)
    {
        // Get shape for chars
        RMParentShape textCharsShape = new RMSpringShape();
        textCharsShape.copyShape(aText);

        // Iterate over runs
        TextLayout textLayout = aText.getTextLayout();
        for (TextLine line : textLayout.getLines())
            for (TextRun run : line.getRuns()) { //if(run.length()==0 || run.isTab()) continue;

                // Get run font and run bounds
                Font font = run.getFont();
                Rect runBounds = new Rect(run.getX(), line.getY(), 0, line.getHeight()); // run y/height instead?

                // Iterate over run chars
                for (int i = 0, iMax = run.length(); i < iMax; i++) {
                    char c = run.charAt(i);

                    // Get char advance (just continue if zero)
                    double advance = font.charAdvance(c);
                    if (advance <= 0)
                        continue;

                    // If non-space character, create glyph shape
                    if (c != ' ') {
                        Rect glyphBounds = font.getCharBounds(c);
                        TextModel glyphTextModel = aText.getTextModel().copyForRange(run.getStartCharIndex() + i, run.getStartCharIndex() + i + 1);
                        RMTextShape glyphShape = new RMTextShape(glyphTextModel);
                        glyphShape.setAutosizing("~-~,~-~");

                        textCharsShape.addChild(glyphShape);
                        runBounds.width = Math.ceil(Math.max(advance, glyphBounds.getMaxX()));
                        Rect glyphFrame = getBoundsFromTextBounds(aText, runBounds);
                        glyphShape.setFrame(glyphFrame);
                    }

                    // Increase bounds by advance
                    runBounds.x += advance;
                }
            }

        // Return
        return textCharsShape;
    }

    /**
     * Returns bounds from given text bounds, adjusted to account for text margins.
     */
    private static Rect getBoundsFromTextBounds(RMTextShape aText, Rect aRect)
    {
        Insets textMargin = aText.getMargin();
        double rectX = aRect.x - textMargin.left;
        double rectY = aRect.y - textMargin.top;
        double rectW = aRect.width + textMargin.getWidth();
        double rectH = aRect.height + textMargin.getHeight();
        return new Rect(rectX, rectY, rectW, rectH);
    }

    /**
     * Replaces any occurrence of consecutive newlines with a single newline in given text model.
     */
    public static void coalesceNewlines(TextModel textModel)
    {
        // Iterate over occurrences of adjacent newlines (from back to font) and remove redundant newline chars
        String string = textModel.getString();
        for (int start = string.lastIndexOf("\n\n"); start >= 0; start = string.lastIndexOf("\n\n", start)) {
            int end = start + 1;
            while (start > 0 && string.charAt(start - 1) == '\n')
                start--;
            textModel.removeChars(start, end);
            string = textModel.getString();
        }

        // Also remove leading newline if present
        if (!textModel.isEmpty() && textModel.charAt(0) == '\n')
            textModel.removeChars(0, 1);
    }


    /**
     * Performs @key@ substitution on an xstring.
     */
    public static TextModel rpgClone(TextModel textModel, ReportOwner rptOwner, Object userInfo, RMShape aShape, boolean doCopy)
    {
        // Declare local variable for resulting out-xstring and for whether something requested a recursive RPG run
        TextModel outString = textModel;
        boolean redo = false;

        // If userInfo provided, plug it into ReportMill
        if (userInfo != null && rptOwner != null)
            rptOwner.pushDataStack(userInfo);

        // Get range for first key found in string
        Range totalKeyRange = nextKeyRangeAfterIndex(outString, 0, new Range());

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
                    nextKeyRangeAfterIndex(outString, totalKeyRange.end, totalKeyRange);
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
                            valString = outString.copyForRange(start, start + string.length());
                            redo = redo || string.contains("@");
                        }
                    }
                }

                // If we found an TextModel, then we'll just use it for key substitution
                else if (val instanceof TextModel)
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
                    if (style != null) {
                        TextModel valTextModel = TextModel.createDefaultTextModel(false);
                        valTextModel.addCharsWithStyle((String) valString, TextStyle.DEFAULT.copyForStyleValue(style.getColor()));
                        valString = valTextModel;
                    }
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
                if (outString == textModel && doCopy)
                    outString = textModel.copyForRange(0, textModel.length());

                // If substitution string was raw string, perform replace (and possible rtf/html evaluation)
                if (valString instanceof String string) {

                    // If string is HTML formatted text, parse into TextModel
                    if (StringUtils.startsWithIC(string, "<html"))
                        valString = RMEnv.getEnv().parseHTML(string, keyRun.getFont(), keyRun.getLine().getLineStyle());

                    // If string is RTF formatted text, parse into TextModel
                    else if (string.startsWith("{\\rtf"))
                        valString = RMEnv.getEnv().parseRTF(string, keyRun.getFont());

                    // If string is normal string, just perform replace and update key range
                    else {
                        outString.replaceChars(string, totalKeyRange.start, totalKeyRange.end);
                        totalKeyRange.setLength(string.length());
                    }
                }

                // If substitution string is xstring, just do xstring replace
                if (valString instanceof TextModel valTextModel) {
                    outString.removeChars(totalKeyRange.start, totalKeyRange.end);
                    outString.addCharsForTextModel(valTextModel, totalKeyRange.start);
                    totalKeyRange.setLength(valTextModel.length());
                }
            }

            // Get next totalKeyRange
            nextKeyRangeAfterIndex(outString, totalKeyRange.end, totalKeyRange);
        }

        // If userInfo was provided, remove it from ReportMill
        if (userInfo != null)
            rptOwner.popDataStack();

        // If something requested a recursive RPG run, do it
        if (redo)
            outString = rpgClone(outString, rptOwner, userInfo, aShape, false);

        // Return RPG text
        return outString;
    }

    /**
     * Returns the range of the next occurrence of @delimited@ text.
     */
    private static Range nextKeyRangeAfterIndex(TextModel textModel, int anIndex, Range aRange)
    {
        // Get length of string (return bogus range if null)
        int length = textModel.length();
        if (length < 2)
            return aRange.set(-1, -1);

        // Get start of key (return if it is the last char)
        int startIndex = textModel.indexOf("@", anIndex);
        if (startIndex == length - 1) return aRange.set(startIndex, startIndex + 1);

        // If startRange of key was found, look for end
        if (startIndex >= 0) {
            int nextIndex = startIndex;
            while (++nextIndex < length) {
                char c = textModel.charAt(nextIndex);
                if (c == '"')
                    while ((++nextIndex < length) && (textModel.charAt(nextIndex) != '"')) ;
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