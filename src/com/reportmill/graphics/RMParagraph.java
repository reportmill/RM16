/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.geom.HPos;
import snap.text.TextLineStyle;

/**
 * This class represents attributes of a paragraph in an RMXString.
 */
public class RMParagraph {

    // The line style
    TextLineStyle _lineStyle = TextLineStyle.DEFAULT;

    // Default paragraph
    public static final RMParagraph DEFAULT = new RMParagraph();
    public static final RMParagraph CENTERED = DEFAULT.copyForAlign(HPos.CENTER);

    // Constants for tab types
    public static final char TAB_LEFT = TextLineStyle.TAB_LEFT;
    public static final char TAB_RIGHT = TextLineStyle.TAB_RIGHT;
    public static final char TAB_CENTER = TextLineStyle.TAB_CENTER;
    public static final char TAB_DECIMAL = TextLineStyle.TAB_DECIMAL;

    /**
     * Constructor.
     */
    public RMParagraph()
    {
    }

    /**
     * Constructor.
     */
    public RMParagraph(TextLineStyle aLineStyle)
    {
        _lineStyle = aLineStyle;
    }

    /**
     * Returns the line style.
     */
    public TextLineStyle getLineStyle()  { return _lineStyle; }

    /**
     * Returns whether is justified.
     */
    public boolean isJustify()  { return _lineStyle.isJustify(); }

    /**
     * Returns the alignment.
     */
    public HPos getAlign()  { return _lineStyle.getAlign(); }

    /**
     * Returns the right side indentation of this paragraph.
     */
    public double getRightIndent()  { return _lineStyle.getRightIndent(); }

    /**
     * Returns the spacing of lines expressed as a factor of a given line's height.
     */
    public float getLineSpacing()  { return (float) _lineStyle.getSpacingFactor(); }

    /**
     * Returns additional line spacing expressed as a constant amount in points.
     */
    public float getLineGap()  { return (float) _lineStyle.getSpacing(); }

    /**
     * Returns the minimum line height in printer points associated with this paragraph.
     */
    public float getLineHeightMin()  { return (float) _lineStyle.getMinHeight(); }

    /**
     * Returns the maximum line height in printer points associated with this paragraph.
     */
    public float getLineHeightMax()  { return (float) _lineStyle.getMaxHeight(); }

    /**
     * Returns the spacing between paragraphs in printer points associated with this paragraph.
     */
    public float getParagraphSpacing()  { return (float) _lineStyle.getNewlineSpacing(); }

    /**
     * Returns the number of tabs associated with this paragraph.
     */
    public int getTabCount()  { return _lineStyle.getTabCount(); }

    /**
     * Returns the specific tab value for the given index in printer points.
     */
    public float getTab(int anIndex)  { return (float) _lineStyle.getTab(anIndex); }

    /**
     * Returns the type of tab at the given index.
     */
    public char getTabType(int anIndex)  { return _lineStyle.getTabType(anIndex); }

    /**
     * Returns the raw tab array
     */
    public float[] getTabs()
    {
        double[] tabs = _lineStyle.getTabs();
        float[] ftabs = new float[tabs.length];
        for (int i = 0; i < tabs.length; i++) ftabs[i] = (float) tabs[i];
        return ftabs;
    }

    /**
     * Returns the raw tab type array
     */
    public char[] getTabTypes()  { return _lineStyle.getTabTypes(); }

    /**
     * Returns a paragraph identical to the receiver, but with the given alignment.
     */
    public RMParagraph copyForAlign(HPos alignX)  { return new RMParagraph(_lineStyle.copyForAlign(alignX)); }

    /**
     * Returns a paragraph identical to the receiver, but with the given indentation values.
     */
    public RMParagraph copyForIndents(double firstIndent, double leftIndent, double rightIndent)
    {
        return new RMParagraph(_lineStyle.copyForIndents(firstIndent, leftIndent, rightIndent));
    }

    /**
     * Returns a paragraph identical to the receiver, but with the given line spacing.
     */
    public RMParagraph deriveLineSpacing(float aHeight)
    {
        return new RMParagraph(_lineStyle.copyForPropKeyValue(TextLineStyle.SpacingFactor_Prop, aHeight));
    }

    /**
     * Returns a paragraph identical to the receiver, but with the given line gap.
     */
    public RMParagraph deriveLineGap(float aHeight)
    {
        return new RMParagraph(_lineStyle.copyForPropKeyValue(TextLineStyle.Spacing_Prop, aHeight));
    }

    /**
     * Returns a paragraph identical to the receiver, but with the given min line height.
     */
    public RMParagraph deriveLineHeightMin(float aHeight)
    {
        return new RMParagraph(_lineStyle.copyForPropKeyValue(TextLineStyle.MinHeight_Prop, aHeight));
    }

    /**
     * Returns a paragraph identical to the receiver, but with the given max line height.
     */
    public RMParagraph deriveLineHeightMax(float aHeight)
    {
        return new RMParagraph(_lineStyle.copyForPropKeyValue(TextLineStyle.MaxHeight_Prop, aHeight));
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        if (this == anObj) return true;
        if (!(anObj instanceof RMParagraph other)) return false;
        if (!other._lineStyle.equals(_lineStyle)) return false;
        return true;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()  { return "RMParagraph: " + _lineStyle; }
}