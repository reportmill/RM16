/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import java.util.*;
import com.reportmill.shape.RMArchiverHpr;
import snap.gfx.*;
import snap.gfx.GradientPaint.Stop;
import snap.util.*;

/**
 * This class represents a fill that draws a linear gradient between an arbitrary list of colors.
 */
public class RMGradientFill extends RMFill {

    // The snap gradient fill
    private GradientPaint _snap;

    /**
     * Constructor.
     */
    public RMGradientFill()
    {
        _snap = new GradientPaint();
    }

    /**
     * Constructor.
     */
    public RMGradientFill(GradientPaint aGradientPaint)
    {
        _snap = aGradientPaint;
    }

    /**
     * Returns the number of color stops in the gradient
     */
    public int getStopCount()  { return _snap.getStopCount(); }

    /**
     * Returns the individual color stop at given index.
     */
    public Stop getStop(int anIndex)  { return _snap.getStop(anIndex); }

    /**
     * Returns the color of the stop at the given index.
     */
    public RMColor getStopColor(int index)  { return RMColor.get(getStop(index).color()); }

    /**
     * Returns the position (in the range {0-1}) for the given stop index.
     */
    public double getStopOffset(int index)  { return getStop(index).offset(); }

    /**
     * Returns the list of color stops.
     */
    public Stop[] getStops()  { return _snap.getStops(); }

    /**
     * Returns whether gradient is radial.
     */
    public boolean isRadial()  { return _snap.isRadial(); }

    /**
     * Returns the gradient's rotation.
     */
    public double getRoll()  { return _snap.getRoll(); }

    /**
     * Returns the color associated with this fill.
     */
    public RMColor getColor()  { return getStopColor(0); }

    /**
     * Returns the snap version of this fill.
     */
    public GradientPaint snap()  { return _snap; }

    /**
     * Derives an instance of this class from another fill.
     */
    public RMGradientFill copyForColor(Color aColor)
    {
        RMGradientFill clone = clone();
        clone._color = aColor != null ? RMColor.get(aColor) : _color;
        GradientPaint.Stop[] stops = Arrays.copyOf(getStops(), getStopCount());
        stops[0] = new Stop(getStopOffset(0), aColor);
        clone._snap = _snap.copyForStops(stops);
        return clone;
    }

    /**
     * Standard clone implementation.
     */
    public RMGradientFill clone()
    {
        RMGradientFill clone = (RMGradientFill) super.clone(); // Do normal clone
        clone._snap = _snap.clone();
        return clone;
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        if (anObj == this) return true;
        RMGradientFill other = anObj instanceof RMGradientFill ? (RMGradientFill) anObj : null;
        if (other == null) return false;
        return _snap.equals(other._snap);
    }

    /**
     * XML archival.
     */
    public XMLElement toXML(XMLArchiver anArchiver)
    {
        return RMArchiverHpr.gradientPaintToXML(_snap);
    }

    /**
     * XML unarchival.
     */
    public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
    {
        _snap = RMArchiverHpr.gradientPaintFromXML(anElement);
        return this;
    }

    /**
     * Standard to string implementation.
     */
    public String toString()  { return _snap.toString(); }
}