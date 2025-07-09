/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import com.reportmill.base.RMFormat;
import snap.geom.Shape;
import snap.gfx.*;
import snap.text.*;
import snap.util.*;

/**
 * This TextAdapter subclass provides some wrappers for RM text.
 */
public class RMTextEditor extends TextAdapter {

    // Whether hyphenating is activated
    static boolean _hyphenating = Prefs.getDefaultPrefs().getBoolean("Hyphenating", false);

    /**
     * Constructor.
     */
    public RMTextEditor(TextModel textModel)
    {
        super(textModel);
        setEditable(true);
        setShowCaret(true);
    }

    /**
     * Returns the text box used to layout text.
     */
    public TextModel getTextBox()  { return _textModel; }

    /**
     * Returns the text style applied to any input characters.
     */
    public RMTextStyle getSelStyle()
    {
        TextStyle textStyle = getSelTextStyle();
        return new RMTextStyle(textStyle);
    }

    /**
     * Sets the attributes that are applied to current selection or newly typed chars.
     */
    public void setInputAttribute(String aKey, Object aValue)
    {
        setSelTextStyleValue(aKey, aValue);
    }

    /**
     * Returns the paragraph of the current selection or cursor position.
     */
    public RMParagraph getInputParagraph()
    {
        int selStart = getSelStart();
        TextLine line = getLineForCharIndex(selStart);
        return new RMParagraph(line.getLineStyle());
    }

    /**
     * Sets the paragraph of the current selection or cursor position.
     */
    public void setInputParagraph(RMParagraph ps)
    {
        _textModel.setLineStyle(ps._lstyle, getSelStart(), getSelEnd());
    }

    /**
     * Returns the plain string of the xstring being edited.
     */
    public String getString()  { return getText(); }

    /**
     * Returns the color of the current selection or cursor.
     */
    public RMColor getColor()
    {
        return getSelStyle().getColor();
    }

    /**
     * Sets the color of the current selection or cursor.
     */
    public void setColor(RMColor color)
    {
        setInputAttribute(RMTextStyle.COLOR_KEY, color);
    }

    /**
     * Returns the font of the current selection or cursor.
     */
    public RMFont getFont()
    {
        return getSelStyle().getFont();
    }

    /**
     * Sets the font of the current selection or cursor.
     */
    public void setFont(RMFont font)
    {
        setInputAttribute(RMTextStyle.FONT_KEY, font);
    }

    /**
     * Returns the format of the current selection or cursor.
     */
    public RMFormat getFormat()
    {
        return getSelStyle().getFormat();
    }

    /**
     * Sets the format of the current selection or cursor, after trying to expand the selection to encompass currently
     * selected, @-sign delineated key.
     */
    public void setFormat(RMFormat aFormat)
    {
        // Get format selection range and select it (if non-null)
        TextSel sel = TextModelUtils.smartFindFormatRange(getTextModel(), getSelStart(), getSelEnd());
        if (sel != null)
            setSel(sel.getStart(), sel.getEnd());

        // Return if we are at end of string (this should never happen)
        if (getSelStart() >= length())
            return;

        // If there is a format, add it to current attributes and set for selected text
        setInputAttribute(RMTextStyle.FORMAT_KEY, aFormat);
    }

    /**
     * Returns whether current selection is outlined.
     */
    public Border getTextBorder()
    {
        return getSelStyle().getBorder();
    }

    /**
     * Sets whether current selection is outlined.
     */
    public void setTextBorder(Border aBorder)
    {
        setInputAttribute(RMTextStyle.BORDER_KEY, aBorder);
    }

    /**
     * Returns the character spacing of the current selection or cursor.
     */
    public float getCharSpacing()
    {
        return (float) getSelStyle().getCharSpacing();
    }

    /**
     * Returns the character spacing of the current selection or cursor.
     */
    public void setCharSpacing(float aValue)
    {
        setInputAttribute(RMTextStyle.CHAR_SPACING_KEY, aValue);
    }

    /**
     * Returns the alignment for current selection.
     */
    public RMTypes.AlignX getAlignX()
    {
        return getInputParagraph().getAlignmentX();
    }

    /**
     * Sets the alignment for current selection.
     */
    public void setAlignX(RMTypes.AlignX anAlignX)
    {
        RMParagraph pg = getInputParagraph().deriveAligned(anAlignX);
        setInputParagraph(pg);
    }

    /**
     * Returns the line spacing for current selection.
     */
    public float getLineSpacing()
    {
        return getInputParagraph().getLineSpacing();
    }

    /**
     * Sets the line spacing for current selection.
     */
    public void setLineSpacing(float aHeight)
    {
        RMParagraph pg = getInputParagraph().deriveLineSpacing(aHeight);
        setInputParagraph(pg);
    }

    /**
     * Returns the line gap for current selection.
     */
    public float getLineGap()
    {
        return getInputParagraph().getLineGap();
    }

    /**
     * Sets the line gap for current selection.
     */
    public void setLineGap(float aHeight)
    {
        RMParagraph pg = getInputParagraph().deriveLineGap(aHeight);
        setInputParagraph(pg);
    }

    /**
     * Returns the min line height for current selection.
     */
    public float getLineHeightMin()
    {
        return getInputParagraph().getLineHeightMin();
    }

    /**
     * Sets the min line height for current selection.
     */
    public void setLineHeightMin(float aHeight)
    {
        RMParagraph pg = getInputParagraph().deriveLineHeightMin(aHeight);
        setInputParagraph(pg);
    }

    /**
     * Returns the maximum line height for a line of text (even if font size would dictate higher).
     */
    public float getLineHeightMax()
    {
        return getInputParagraph().getLineHeightMax();
    }

    /**
     * Sets the maximum line height for a line of text (even if font size would dictate higher).
     */
    public void setLineHeightMax(float aHeight)
    {
        RMParagraph pg = getInputParagraph().deriveLineHeightMax(aHeight);
        setInputParagraph(pg);
    }

    /**
     * Replaces the current selection with the given string.
     */
    public void replace(String aString)  { replaceChars(aString); }

    /**
     * Returns the height needed to display all characters.
     */
    public double getPrefHeight()  { return getPrefHeight(-1); }

    /**
     * Override to support spell check paint.
     */
    @Override
    public void paintAll(Painter aPntr)
    {
        // Paint selection
        paintSel(aPntr);

        // Paint spell check
        if (isSpellChecking() && length() > 0)
            paintSpellCheck(aPntr);

        // Paint Text
        paintText(aPntr);
    }

    /**
     * Paints spell check.
     */
    private void paintSpellCheck(Painter aPntr)
    {
        // Set RM SpellCheck
        snap.text.SpellCheck.setSharedClass(RMSpellCheck.class);

        // Get spelling path
        Shape spellingPath = snap.text.SpellCheck.getSpellingPath(_textModel, getSelStart());

        // Paint spelling path
        aPntr.setColor(Color.RED);
        aPntr.setStroke(Stroke.StrokeDash1);
        aPntr.draw(spellingPath);
        aPntr.setColor(Color.BLACK);
        aPntr.setStroke(Stroke.Stroke1);
    }

    /**
     * Override to always show caret.
     */
    @Override
    public boolean isShowCaret()  { return true; }

    /**
     * Returns whether layout tries to hyphenate wrapped words.
     */
    public static boolean isHyphenating()
    {
        return _hyphenating;
    }

    /**
     * Sets whether layout tries to hyphenate wrapped words.
     */
    public static void setHyphenating(boolean aValue)
    {
        Prefs.getDefaultPrefs().setValue("Hyphenating", _hyphenating = aValue);
    }
}