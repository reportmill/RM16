/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.graphics.*;
import snap.gfx.Font;
import snap.util.*;

/**
 * This shape is used by graph area to hold attributes of the value axis.
 */
public class RMGraphPartLabelAxis extends RMShape {

    // Whether to show axis labels
    private boolean _showAxisLabels = true;

    // Whether to show grid lines between label axis sections
    private boolean _showGridLines = true;

    // The label axis item key
    private String _itemKey = "@Row@";

    // The font
    private Font _font;

    /**
     * Constructor.
     */
    public RMGraphPartLabelAxis()
    {
        super();
    }

    /**
     * Returns whether the graph shows axis labels.
     */
    public boolean getShowAxisLabels()  { return _showAxisLabels; }

    /**
     * Sets whether the graph shows axis labels.
     */
    public void setShowAxisLabels(boolean aFlag)
    {
        _showAxisLabels = aFlag;
        relayoutParent();
    }

    /**
     * Returns whether the graph shows grid lines between label axis sections.
     */
    public boolean getShowGridLines()  { return _showGridLines; }

    /**
     * Sets whether the graph shows grid lines between label axis sections.
     */
    public void setShowGridLines(boolean aFlag)
    {
        _showGridLines = aFlag;
        relayoutParent();
    }

    /**
     * Returns the item key.
     */
    public String getItemKey()  { return _itemKey; }

    /**
     * Sets the item key.
     */
    public void setItemKey(String aKey)
    {
        _itemKey = aKey;
        relayoutParent();
    }

    /**
     * Returns whether font has been set.
     */
    public boolean isFontSet()  { return _font != null; }

    /**
     * Return current font.
     */
    public Font getFont()  { return _font != null ? _font : Font.Arial10; }

    /**
     * Set current font.
     */
    public void setFont(Font aFont)
    {
        _font = aFont;
        relayoutParent();
    }

    /**
     * Override to trigger layout.
     */
    public void setRoll(double aValue)
    {
        super.setRoll(aValue);
        relayoutParent();
    }

    /**
     * XML archival.
     */
    public XMLElement toXML(RMArchiver anArchiver)
    {
        // Archive basic shape attributes and reset element name
        XMLElement e = super.toXML(anArchiver);
        e.setName("label-axis");

        // Archive ShowAxisLabels, ShowGridLines, ItemKey
        if (!_showAxisLabels) e.add("show-labels", false);
        if (!_showGridLines) e.add("show-grid", false);
        if (_itemKey != null && !_itemKey.isEmpty()) e.add("item-key", _itemKey);

        // Return xml element
        return e;
    }

    /**
     * XML unarchival.
     */
    public Object fromXML(RMArchiver anArchiver, XMLElement anElement)
    {
        // Unarchive basic shape attributes
        super.fromXML(anArchiver, anElement);

        // Unarchive ShowAxisLabels, ShowGridLines, ItemKey
        setShowAxisLabels(anElement.getAttributeBoolValue("show-labels", true));
        setShowGridLines(anElement.getAttributeBoolValue("show-grid", true));
        setItemKey(anElement.getAttributeValue("item-key"));

        // Return this graph
        return this;
    }
}