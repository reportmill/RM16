/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import com.reportmill.shape.RMArchiverHpr;
import snap.text.TextLineStyle;
import snap.util.*;

/**
 * This class represents attributes of a paragraph in an RMXString (all of the characters up to and including each
 * newline in an RMXString make up a paragraph). Paragraphs can have their own alignment, indentation, min/max line
 * height, etc. You might use this class like this:
 * <p><blockquote><pre>
 *   RMParagraph pgraph = RMParagraph.defaultParagraph.deriveAligned(RMParagraph.ALIGN_RIGHT);
 *   RMXString xstring = new RMXString("Hello World", pgraph);
 */
public class RMParagraph implements Cloneable, RMTypes, XMLArchiver.Archivable {

    // The line style
    TextLineStyle _lineStyle = TextLineStyle.DEFAULT;

    // Default paragraph
    public static final RMParagraph DEFAULT = new RMParagraph();
    public static final RMParagraph CENTERED = DEFAULT.deriveAligned(RMTypes.AlignX.Center);

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
     * Returns the alignment associated with this paragraph.
     */
    public AlignX getAlignmentX()
    {
        if (_lineStyle.isJustify()) return AlignX.Full;
        return AlignX.get(_lineStyle.getAlign());
    }

    /**
     * Returns indentation of first line in paragraph (this can be set different than successive lines).
     */
    public double getFirstIndent()  { return _lineStyle.getFirstIndent(); }

    /**
     * Returns the left side indentation of this paragraph.
     */
    public double getLeftIndent()  { return _lineStyle.getLeftIndent(); }

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
    public RMParagraph deriveAligned(AlignX anAlign)
    {
        RMParagraph ps = clone();
        if (anAlign == AlignX.Full) ps._lineStyle = _lineStyle.copyForPropKeyValue(TextLineStyle.Justify_Prop, true);
        else ps._lineStyle = _lineStyle.copyForAlign(anAlign.hpos());
        return ps;
    }

    /**
     * Returns a paragraph identical to the receiver, but with the given indentation values.
     */
    public RMParagraph deriveIndent(double firstIndent, double leftIndent, double rightIndent)
    {
        RMParagraph ps = clone();
        ps._lineStyle = ps._lineStyle.copyForPropKeyValue(TextLineStyle.FirstIndent_Prop, firstIndent);
        ps._lineStyle = ps._lineStyle.copyForPropKeyValue(TextLineStyle.LeftIndent_Prop, leftIndent);
        ps._lineStyle = ps._lineStyle.copyForPropKeyValue(TextLineStyle.RightIndent_Prop, rightIndent);
        return ps;
    }

    /**
     * Returns a paragraph identical to the receiver, but with the given line spacing.
     */
    public RMParagraph deriveLineSpacing(float aHeight)
    {
        RMParagraph ps = clone();
        ps._lineStyle = _lineStyle.copyForPropKeyValue(TextLineStyle.SpacingFactor_Prop, aHeight);
        return ps;
    }

    /**
     * Returns a paragraph identical to the receiver, but with the given line gap.
     */
    public RMParagraph deriveLineGap(float aHeight)
    {
        RMParagraph ps = clone();
        ps._lineStyle = _lineStyle.copyForPropKeyValue(TextLineStyle.Spacing_Prop, aHeight);
        return ps;
    }

    /**
     * Returns a paragraph identical to the receiver, but with the given min line height.
     */
    public RMParagraph deriveLineHeightMin(float aHeight)
    {
        RMParagraph ps = clone();
        ps._lineStyle = _lineStyle.copyForPropKeyValue(TextLineStyle.MinHeight_Prop, aHeight);
        return ps;
    }

    /**
     * Returns a paragraph identical to the receiver, but with the given max line height.
     */
    public RMParagraph deriveLineHeightMax(float aHeight)
    {
        RMParagraph ps = clone();
        ps._lineStyle = _lineStyle.copyForPropKeyValue(TextLineStyle.MaxHeight_Prop, aHeight);
        return ps;
    }

    /**
     * Standard clone of this object.
     */
    public RMParagraph clone()
    {
        try { return (RMParagraph) super.clone(); }
        catch (CloneNotSupportedException e) { throw new RuntimeException(e); }
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
     * XML archival.
     */
    public XMLElement toXML(XMLArchiver anArchiver)
    {
        return RMArchiverHpr.lineStyleToXML(_lineStyle);
    }

    /**
     * XML unarchival.
     */
    public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
    {
        _lineStyle = RMArchiverHpr.lineStyleFromXML(anElement);
        return this;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()  { return "RMParagraph: " + _lineStyle; }
}