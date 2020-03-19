/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.apptools;
import reportmill.shape.RMGraphLegend;
import rmdraw.apptools.SGParentTool;
import rmdraw.scene.*;
import snap.view.ViewEvent;

/**
 * Provides UI editing for RMGraphLegend.
 */
public class RMGraphLegendTool <T extends RMGraphLegend> extends SGParentTool<T> {

/**
 * Override to configure UI.
 */
protected void initUI()  { enableEvents("LegendText", DragDrop); }

/**
 * Reset UI.
 */
public void resetUI()
{
    // Get selected legend
    RMGraphLegend leg = getSelView();
    
    // Update LegendText
    setViewText("LegendText", leg.getLegendText());
}

/**
 * Respond UI.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Get selected legend
    RMGraphLegend leg = getSelView();
    
    // Handle LegendText (Action and DragDrop)
    if(anEvent.equals("LegendText"))
        leg.setLegendText(anEvent.getStringValue());
}

/**
 * Returns the name of the graph inspector.
 */
public String getWindowTitle()  { return "Graph Legend Inspector"; }

/**
 * Override to make RMGraphLegend not super-selectable. 
 */
public boolean isSuperSelectable(SGView aShape)  { return false; }

}