/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.geom.HPos;
import snap.gfx.*;
import snap.text.*;
import snap.util.*;

/**
 * This TextAdapter subclass provides some wrappers for RM text.
 */
public class RMTextEditor extends TextAdapter {

    // Whether as-you-type spell checking is enabled
    private static Boolean _spellChecking = Prefs.getDefaultPrefs().getBoolean("SpellChecking", false);

    // Whether hyphenating is activated
    private static boolean _hyphenating = Prefs.getDefaultPrefs().getBoolean("Hyphenating", false);

    /**
     * Constructor.
     */
    public RMTextEditor(TextLayout textLayout)
    {
        super(textLayout);
        setEditable(true);
        setShowCaret(true);
        snap.text.SpellCheck.setSharedClass(RMSpellCheck.class);
    }

    /**
     * Sets the attributes that are applied to current selection or newly typed chars.
     */
    public void setInputAttribute(String aKey, Object aValue)  { setSelTextStyleValue(aKey, aValue); }

    /**
     * Returns the line style of the current selection or cursor position.
     */
    public TextLineStyle getInputLineStyle()
    {
        int selStart = getSelStart();
        return getLineForCharIndex(selStart).getLineStyle();
    }

    /**
     * Sets the paragraph of the current selection or cursor position.
     */
    public void setInputLineStyle(TextLineStyle lineStyle)  { _textModel.setLineStyle(lineStyle, getSelStart(), getSelEnd()); }

    /**
     * Returns the color of the current selection or cursor.
     */
    public Color getColor()  { return getSelTextStyle().getColor(); }

    /**
     * Sets the color of the current selection or cursor.
     */
    public void setColor(Color color)  { setInputAttribute(TextStyle.Color_Prop, color); }

    /**
     * Returns the font of the current selection or cursor.
     */
    public Font getFont()  { return getSelTextStyle().getFont(); }

    /**
     * Sets the font of the current selection or cursor.
     */
    public void setFont(Font font)  { setInputAttribute(TextStyle.Font_Prop, font); }

    /**
     * Returns the format of the current selection or cursor.
     */
    public TextFormat getFormat()  { return getSelTextStyle().getFormat(); }

    /**
     * Sets the format of the current selection or cursor, after trying to expand the selection to encompass currently
     * selected, @-sign delineated key.
     */
    public void setFormat(TextFormat aFormat)
    {
        // Get format selection range and select it (if non-null)
        TextSel sel = TextModelUtils.smartFindFormatRange(getTextModel(), getSelStart(), getSelEnd());
        if (sel != null)
            setSel(sel.getStart(), sel.getEnd());

        // Return if we are at end of string (this should never happen)
        if (getSelStart() >= length())
            return;

        // If there is a format, add it to current attributes and set for selected text
        setInputAttribute(TextStyle.Format_Prop, aFormat);
    }

    /**
     * Returns whether current selection is outlined.
     */
    public Border getTextBorder()  { return getSelTextStyle().getBorder(); }

    /**
     * Sets whether current selection is outlined.
     */
    public void setTextBorder(Border aBorder)  { setInputAttribute(TextStyle.Border_Prop, aBorder); }

    /**
     * Returns the character spacing of the current selection or cursor.
     */
    public float getCharSpacing()  { return (float) getSelTextStyle().getCharSpacing(); }

    /**
     * Returns the character spacing of the current selection or cursor.
     */
    public void setCharSpacing(float aValue)  { setInputAttribute(TextStyle.CharSpacing_Prop, aValue); }

    /**
     * Returns the alignment for current selection.
     */
    public HPos getAlignX()  { return getInputLineStyle().getAlign(); }

    /**
     * Sets the alignment for current selection.
     */
    public void setAlignX(HPos alignX)
    {
        TextLineStyle lineStyle = getInputLineStyle().copyForAlign(alignX);
        setInputLineStyle(lineStyle);
    }

    /**
     * Returns the line spacing for current selection.
     */
    public double getLineSpacingFactor()  { return getInputLineStyle().getSpacingFactor(); }

    /**
     * Sets the line spacing for current selection.
     */
    public void setLineSpacingFactor(double aHeight)
    {
        setInputLineStyle(getInputLineStyle().copyForPropKeyValue(TextLineStyle.SpacingFactor_Prop, aHeight));
    }

    /**
     * Returns the line gap for current selection.
     */
    public double getLineSpacing()  { return getInputLineStyle().getSpacing(); }

    /**
     * Sets the line gap for current selection.
     */
    public void setLineSpacing(double aHeight)
    {
        setInputLineStyle(getInputLineStyle().copyForPropKeyValue(TextLineStyle.Spacing_Prop, aHeight));
    }

    /**
     * Returns the min line height for current selection.
     */
    public double getLineMinHeight()  { return getInputLineStyle().getMinHeight(); }

    /**
     * Sets the min line height for current selection.
     */
    public void setLineMinHeight(double aHeight)
    {
        setInputLineStyle(getInputLineStyle().copyForPropKeyValue(TextLineStyle.MinHeight_Prop, aHeight));
    }

    /**
     * Returns the maximum line height for a line of text (even if font size would dictate higher).
     */
    public double getLineMaxHeight()  { return getInputLineStyle().getMaxHeight(); }

    /**
     * Sets the maximum line height for a line of text (even if font size would dictate higher).
     */
    public void setLineMaxHeight(double aHeight)
    {
        setInputLineStyle(getInputLineStyle().copyForPropKeyValue(TextLineStyle.MaxHeight_Prop, aHeight));
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
     * Override to always show caret.
     */
    @Override
    public boolean isShowCaret()  { return true; }

    /**
     * Override to use RMTextEditor global.
     */
    @Override
    public boolean isSpellChecking()  { return isSpellCheckingGlobal(); }

    /**
     * Returns whether editor is doing check-as-you-type spelling for all text editors.
     */
    public static boolean isSpellCheckingGlobal()
    {
        if (_spellChecking != null) return _spellChecking;
        return _spellChecking = Prefs.getDefaultPrefs().getBoolean("SpellChecking", false);
    }

    /**
     * Returns whether editor is doing check-as-you-type spelling for all text editors.
     */
    public static void setSpellCheckingGlobal(boolean aValue)
    {
        if (aValue == isSpellCheckingGlobal()) return;
        _spellChecking = aValue;
        Prefs.getDefaultPrefs().setValue("SpellChecking", aValue);
    }

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