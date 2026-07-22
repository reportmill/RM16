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