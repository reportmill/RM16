/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.geom.RoundRect;
import snap.geom.Shape;
import snap.util.*;

/**
 * This class represents a simple rectangle shape with a rounding radius.
 */
public class RMRectShape extends RMShape {

    /**
     * XML archival.
     */
    public XMLElement toXML(RMArchiver anArchiver)
    {
        XMLElement xml = super.toXML(anArchiver);
        xml.setName("rect");
        return xml;
    }
}