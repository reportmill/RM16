/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.shape;
import reportmill.util.RMGroup;

import java.util.*;

import rmdraw.shape.*;
import snap.gfx.Font;
import snap.gfx.Rect;
import snap.gfx.RichText;
import snap.util.*;

/**
 * An inner class for Legend.
 */
public class RMGraphLegend extends RMParentShape implements ReportGen.RPG {

    // The legend text
    String          _legendText;
    
    // The font
    Font          _font;
    
/**
 * Returns the legend text.
 */
public String getLegendText()  { return _legendText; }

/**
 * Sets the legend text.
 */
public void setLegendText(String aString)
{
    _legendText = aString; resetItems();
}

/**
 * Override to get from graph.
 */
public String getDatasetKey()
{
    RMGraph graph = getGraph();
    return graph!=null? graph.getDatasetKey() : null;
}

/**
 * Returns whether font has been set.
 */
public boolean isFontSet()  { return _font!=null; }

/**
 * Return current font.
 */
public Font getFont()  { return _font!=null? _font : Font.Arial10; }

/**
 * Set current font.
 */
public void setFont(Font aFont)  { _font = aFont; }

/**
 * Override to reset sample legend items if no children.
 */
public void setParent(RMParentShape aPar)
{
    super.setParent(aPar);
    if(getChildCount()==0) resetItems();
}

/**
 * Reset items.
 */
public void resetItems()
{
    removeChildren();
    relayout();
}

/**
 * Override to reset items if needed.
 */
protected void layoutImpl()
{
    // If items already set or no parent, just return
    if(getChildCount()>0 || getParent()==null) return;
    
    // Configure items
    RMGraphRPG graphRPG = getGraphRPG(getParent());
    configureRPG(graphRPG, false);
}

/**
 * Override to .
 */
@Override
public RMShape rpgAll(ReportOwner anRptOwner, RMShape aParent)
{
    // Do normal clone (rpgShape())
    RMGraphLegend clone = (RMGraphLegend)clone();
    
    // Get Graph RPG and configure
    RMGraphRPG graphRPG = getGraphRPG((RMParentShape)aParent);
    clone.configureRPG(graphRPG, true);
    
    // Do bindings and return
    rpgBindings(anRptOwner, clone);
    return clone;
}

/**
 * Override to layout legend.
 */
protected void configureRPG(RMGraphRPG graphRPG, boolean doRPG)
{
    // Make sure there is a graphRPG
    if(graphRPG==null) { System.err.println("RMGraphLegend.configureRPG: Graph RPG not found"); return; }
    
    // Get Graph
    RMGraph graph = getGraph(); if(graph==null) graph = graphRPG._graph;
    
    // Get legend text whether to add legend entry per item
    String legendText = getLegendText(); if(legendText==null) legendText = "";
    boolean doPerItem = graph.isColorItems() || graph.getType()==RMGraph.Type.Pie;
    
    // Get strings and groups
    List <String> strings = new ArrayList();
    List <RMGroup> groups = new ArrayList();
    
    // If doPerItem, add for each item
    if(doPerItem) { RMGraphSeries series = graphRPG.getSeries(0);
        for(int i=0,iMax=series.getItemCount();i<iMax;i++) { RMGraphSeries.Item item = series.getItem(i);
            String text = legendText.length()>0? legendText : ("Item " + (i+1));
            strings.add(text); groups.add(item._group);
        }
    }

    // If more than one series, add item for each series
    else for(int i=0, iMax=graphRPG.getSeriesCount(); i<iMax; i++) { RMGraphSeries series = graphRPG.getSeries(i);
        String text = legendText.length()>0? legendText : series.getTitle();
        strings.add(text); groups.add(series._group);
    }
    
    // Iterate over strings and add legend items
    double x = 2, y = 2;
    for(int i=0,iMax=strings.size();i<iMax; i++) {
        
        // Create Legend item box
        RMRectShape box = new RMRectShape();
        box.setColor(graphRPG.getColor(i));
        box.setBounds(x,y,16,12); x += 18;
        
        // Create Legend item text
        String text = strings.get(i);
        RMTextShape label = new RMTextShape();
        label.setText(text);
        label.setFont(getFont());
        
        // If text is a key, evaluate it
        if(doRPG && text.contains("@")) {
            RMGroup group = groups.get(i);
            RichText rtext = label.getRichText();
            graphRPG._rptOwner.rpgCloneRichText(rtext, group, null, false);
        }
        
        // Reset label to appropriate size
        label.setBounds(x,y,getWidth()-x-2,8);
        label.setBestHeight();
        
        // Add box and label and increment x/y
        addChild(box); addChild(label);
        x = 2; y += label.getHeight() + 2;
    }
    
    // Resize
    Rect bounds = getBoundsOfChildren();
    if(bounds.getMaxX()+2>getWidth()) setWidth(bounds.getMaxX()+2);
    if(bounds.getMaxY()+2>getHeight()) setHeight(bounds.getMaxY()+2);
}

/**
 * Returns the graph that owns this legend.
 */
private RMGraph getGraph()  { return getParent()!=null? getParent().getChildWithClass(RMGraph.class) : null; }

/**
 * Returns RMGraphRPG, if parent has RMGraphRPG.GraphShape child.
 */
private RMGraphRPG getGraphRPG(RMParentShape aParent)
{
    RMGraphRPG.GraphShape gshp = aParent!=null? aParent.getChildWithClass(RMGraphRPG.GraphShape.class) : null;
    return gshp!=null? gshp.getGraphRPG() : null;
}

/**
 * XML archival.
 */
public XMLElement toXMLShape(XMLArchiver anArchiver)
{
    // Archive basic shape attributes
    XMLElement e = super.toXMLShape(anArchiver); e.setName("graph-legend");
    
    // Archive LegendText
    if(getLegendText()!=null && getLegendText().length()>0) e.add("text", getLegendText());
    return e;
}

/**
 * XML unarchival.
 */
public void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes
    super.fromXMLShape(anArchiver, anElement);
    
    // Unarchive LegendText
    setLegendText(anElement.getAttributeValue("text"));
}

/** Override to suppress child archival. */
protected void toXMLChildren(XMLArchiver anArchiver, XMLElement anElement)  { }

}