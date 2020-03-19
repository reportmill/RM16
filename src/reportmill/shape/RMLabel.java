/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.shape;
import rmdraw.scene.SGParent;
import rmdraw.scene.SceneGraph;
import snap.geom.Rect;
import snap.gfx.*;
import snap.util.*;

/**
 * This class represents an individual label inside an RMLabels template.
 */
public class RMLabel extends SGParent {

/**
 * Editor method - indicates that individual label accepts children.
 */
public boolean acceptsChildren()  { return true; }

/**
 * Paints label.
 */
protected void paintView(Painter aPntr)
{
    // Do normal paint shape
    super.paintView(aPntr);
    
    // Table bands should draw a red band around thier perimeter when it is selected
    boolean selected = SceneGraph.isSelected(this) || SceneGraph.isSuperSelected(this);
    if (selected) {
        Rect bounds = getBoundsLocal();
        bounds.inset(2, 2);
        aPntr.setColor(Color.RED);
        aPntr.setStroke(Stroke.Stroke1);
        aPntr.draw(bounds);
    }
}

/**
 * XML archival.
 */
protected XMLElement toXMLView(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and reset element name
    XMLElement e = super.toXMLView(anArchiver); e.setName("label");
    return e;
}

}

