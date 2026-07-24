/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
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
        snap.text.SpellCheck.setSharedClass(RMSpellCheck.class);
    }

    /**
     * Sets the text format for given text adapter such that it covers key range.
     */
    public void setTextFormatSmart(TextFormat textFormat)
    {
        // Get full range of key found in current text selection
        TextSel textSel = smartFindFormatRange(getTextModel(), getSelStart(), getSelEnd());
        if (textSel != null)
            setSel(textSel.getStart(), textSel.getEnd());

        // If at end of text, just return (should never happen)
        if (getSelStart() >= length())
            return;

        // If there is a format, add it to current attributes and set for selected text
        setSelTextStyleValue(TextStyle.Format_Prop, textFormat);
    }

    /**
     * This method returns the range of the @-sign delinated key closest to the current selection (or null if not found).
     */
    private static TextSel smartFindFormatRange(TextLayout textLayout, int selStart, int selEnd)
    {
        String string = textLayout.getString();
        int prevAtSignIndex = -1;
        int nextAtSignIndex = -1;

        // See if selection contains an '@'
        if (selEnd > selStart)
            prevAtSignIndex = string.indexOf("@", selStart);
        if (prevAtSignIndex >= selEnd)
            prevAtSignIndex = -1;

        // If there wasn't an '@' in selection, see if there is one before the selected range
        if (prevAtSignIndex < 0)
            prevAtSignIndex = string.lastIndexOf("@", selStart - 1);

        // If there wasn't an '@' in or before selection, see if there is one after the selected range
        if (prevAtSignIndex < 0)
            prevAtSignIndex = string.indexOf("@", selEnd);

        // If there is a '@' in, before or after selection, see if there is another after it
        if (prevAtSignIndex >= 0)
            nextAtSignIndex = string.indexOf("@", prevAtSignIndex + 1);

        // If there is a '@' in, before or after selection, but not one after it, see if there is one before that
        if (prevAtSignIndex >= 0 && nextAtSignIndex < 0)
            nextAtSignIndex = string.lastIndexOf("@", prevAtSignIndex - 1);

        // If both a previous and next '@', select the chars inbetween
        if (prevAtSignIndex >= 0 && nextAtSignIndex >= 0 && prevAtSignIndex != nextAtSignIndex) {
            int start = Math.min(prevAtSignIndex, nextAtSignIndex);
            int end = Math.max(prevAtSignIndex, nextAtSignIndex);
            return new TextSel(textLayout, start, end + 1);
        }

        // Return null since range not found
        return null;
    }

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
        Prefs.getDefaultPrefs().setValue("SpellChecking", _spellChecking = aValue);
    }

    /**
     * Returns whether layout tries to hyphenate wrapped words.
     */
    public static boolean isHyphenating()  { return _hyphenating; }

    /**
     * Sets whether layout tries to hyphenate wrapped words.
     */
    public static void setHyphenating(boolean aValue)
    {
        Prefs.getDefaultPrefs().setValue("Hyphenating", _hyphenating = aValue);
    }
}