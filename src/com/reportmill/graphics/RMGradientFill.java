/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import java.util.*;
import com.reportmill.shape.RMArchiver;
import snap.gfx.*;
import snap.gfx.GradientPaint.Stop;
import snap.util.*;

/**
 * This class represents a fill that draws a linear gradient between an arbitrary list of colors.
 */
public class RMGradientFill extends RMFill {

    // The snap gradient paint
    private GradientPaint _gradientPaint;

    /**
     * Constructor.
     */
    public RMGradientFill()
    {
        _gradientPaint = new GradientPaint();
    }

    /**
     * Constructor.
     */
    public RMGradientFill(GradientPaint aGradientPaint)
    {
        _gradientPaint = aGradientPaint;
    }

    /**
     * Returns the number of color stops in the gradient
     */
    public int getStopCount()  { return _gradientPaint.getStopCount(); }

    /**
     * Returns the individual color stop at given index.
     */
    public Stop getStop(int anIndex)  { return _gradientPaint.getStop(anIndex); }

    /**
     * Returns the color of the stop at the given index.
     */
    public Color getStopColor(int index)  { return getStop(index).color(); }

    /**
     * Returns the position (in the range {0-1}) for the given stop index.
     */
    public double getStopOffset(int index)  { return getStop(index).offset(); }

    /**
     * Returns the list of color stops.
     */
    public Stop[] getStops()  { return _gradientPaint.getStops(); }

    /**
     * Returns whether gradient is radial.
     */
    public boolean isRadial()  { return _gradientPaint.isRadial(); }

    /**
     * Returns the gradient's rotation.
     */
    public double getRoll()  { return _gradientPaint.getRoll(); }

    /**
     * Returns the color associated with this fill.
     */
    public Color getColor()  { return getStopColor(0); }

    /**
     * Returns the snap version of this fill.
     */
    public GradientPaint snap()  { return _gradientPaint; }

    /**
     * Derives an instance of this class from another fill.
     */
    public RMGradientFill copyForColor(Color aColor)
    {
        RMGradientFill clone = clone();
        clone._color = aColor != null ? aColor : _color;
        GradientPaint.Stop[] stops = Arrays.copyOf(getStops(), getStopCount());
        stops[0] = new Stop(getStopOffset(0), aColor);
        clone._gradientPaint = _gradientPaint.copyForStops(stops);
        return clone;
    }

    /**
     * Standard clone implementation.
     */
    public RMGradientFill clone()
    {
        RMGradientFill clone = (RMGradientFill) super.clone(); // Do normal clone
        clone._gradientPaint = _gradientPaint.clone();
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
        return _gradientPaint.equals(other._gradientPaint);
    }

    /**
     * XML archival.
     */
    public XMLElement toXML(RMArchiver anArchiver)
    {
        // Archive basic fill attributes
        XMLElement e = new XMLElement("gradient-fill");

        // Archive Type
        if (_gradientPaint.isRadial())
            e.add("type", "radial");

        // Archive Points/Roll
        if (_gradientPaint.isRadial()) {
            e.add("x0", _gradientPaint.getStartX()); e.add("y0", _gradientPaint.getStartY());
            e.add("x1", _gradientPaint.getEndX()); e.add("y1", _gradientPaint.getEndY());
        }
        else if (_gradientPaint.getRoll() != 0)
            e.add("roll", _gradientPaint.getRoll());

        // Archive first color
        if (!_gradientPaint.getStopColor(0).equals(Color.BLACK))
            e.add("color", "#" + _gradientPaint.getStopColor(0).toHexString());

        // Archive all colors beyond the first one as color2,color3 (for compatibility)
        for (int i = 1, iMax = _gradientPaint.getStopCount(); i < iMax; i++) {
            Color c = _gradientPaint.getStopColor(i);
            if (!c.equals(Color.BLACK))
                e.add("color"+(i+1), "#" + c.toHexString());
        }

        // Archive stop positions (stop 0 defaults to 0.0, and last stop defaults to 1.0)
        for (int i = 0, iMax = _gradientPaint.getStopCount(); i < iMax; i++) {
            double offset = _gradientPaint.getStopOffset(i);
            if (i == 0 && MathUtils.equalsZero(offset)) continue;
            if (i == iMax-1 && MathUtils.equals(offset, 1)) continue;
            e.add("stop"+(i==0 ? "" : (i+1)), offset);
        }

        // Archive the number of stops, since the defaults in the above lists make it possibly indeterminate
        if (_gradientPaint.getStopCount() != 2)
            e.add("nstops", _gradientPaint.getStopCount());

        // Return element
        return e;
    }

    /**
     * XML unarchival.
     */
    public Object fromXML(RMArchiver anArchiver, XMLElement anElement)
    {
        // Unarchive type
        String type = anElement.getAttributeValue("type", "linear");
        GradientPaint.Type gradientType = GradientPaint.Type.LINEAR;
        if (type.equals("radial"))
            gradientType = GradientPaint.Type.RADIAL;

        // Unarchive points
        double sx = anElement.getAttributeDoubleValue("x0", .5);
        double sy = anElement.getAttributeDoubleValue("y0", .5);
        double ex = anElement.getAttributeDoubleValue("x1", 1);
        double ey = anElement.getAttributeDoubleValue("y1", .5);

        // Unarchive roll
        double roll = anElement.getAttributeFloatValue("roll");

        // Unarchive stops
        int stopCount = anElement.getAttributeIntValue("nstops", 2);
        GradientPaint.Stop[] stops = new GradientPaint.Stop[stopCount];
        for (int i = 0; i < stopCount; i++) {

            // Get stop color
            String indexStr = i == 0 ? "" : String.valueOf(i+1);
            String colorStr = anElement.getAttributeValue("color" + indexStr); // unarchive color,color2...
            Color stopColor = colorStr == null ? Color.BLACK : new Color(colorStr);

            // Get stop offset
            double offset;
            XMLAttribute stopAttr = anElement.getAttribute("stop" + indexStr); // unarchive stop,stop2...
            if (stopAttr == null) {
                if (i == 0) offset = 0;
                else if (i == stopCount - 1) offset = 1;
                else continue;
            }
            else offset = stopAttr.getFloatValue();
            stops[i] = new GradientPaint.Stop(offset, stopColor);
        }

        // Return this gradient paint
        GradientPaint gradientPaint = new GradientPaint(gradientType, sx, sy, ex, ey, stops);
        if (roll != 0)
            gradientPaint = gradientPaint.copyForRoll(roll);
        _gradientPaint = gradientPaint;
        return this;
    }

    /**
     * Standard to string implementation.
     */
    public String toString()  { return _gradientPaint.toString(); }
}