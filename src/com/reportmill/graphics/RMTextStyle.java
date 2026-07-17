/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.*;
import snap.text.TextFormat;
import snap.text.TextStyle;

/**
 * A class to hold style attributes for a text run.
 */
public class RMTextStyle implements Cloneable {

    // The style
    TextStyle _style;

    // Constants for style attribute keys
    public static final String FONT_KEY = TextStyle.Font_Prop;
    public static final String COLOR_KEY = TextStyle.Color_Prop;
    public static final String FORMAT_KEY = TextStyle.Format_Prop;
    public static final String UNDERLINE_KEY = TextStyle.Underline_Prop;
    public static final String BORDER_KEY = TextStyle.Border_Prop;
    public static final String SCRIPTING_KEY = TextStyle.Scripting_Prop;
    public static final String CHAR_SPACING_KEY = TextStyle.CharSpacing_Prop;

    /**
     * Constructor.
     */
    public RMTextStyle(TextStyle aStyle)
    {
        _style = aStyle;
    }

    /**
     * Returns the font for this run.
     */
    public RMFont getFont()  { return RMFont.get(_style.getFont()); }

    /**
     * Returns the color for this run.
     */
    public RMColor getColor()  { return RMColor.get(_style.getColor()); }

    /**
     * Returns whether this run is underlined.
     */
    public boolean isUnderlined()  { return _style.isUnderlined(); }

    /**
     * Returns the char spacing.
     */
    public double getCharSpacing()  { return _style.getCharSpacing(); }

    /**
     * Returns the format.
     */
    public TextFormat getFormat()  { return _style.getFormat(); }

    /**
     * Returns the text border.
     */
    public Border getBorder()  { return _style.getBorder(); }

    /**
     * Standard clone implementation.
     */
    public RMTextStyle clone()
    {
        try { return (RMTextStyle) super.clone(); }
        catch (CloneNotSupportedException e) { throw new RuntimeException(e); }
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        if (anObj == this) return true;
        RMTextStyle other = anObj instanceof RMTextStyle ? (RMTextStyle) anObj : null;
        if (other == null) return false;
        return other._style.equals(_style);
    }

    /**
     * Standard toString implementation.
     */
    public String toString()  { return _style.toString(); }

    /**
     * Returns the most likely key for a given style attribute.
     */
    public static String getStyleKey(Object anAttr)  { return TextStyle.getStyleKey(anAttr); }
}