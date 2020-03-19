/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.apptools;
import reportmill.shape.*;
import rmdraw.app.Editor;
import rmdraw.app.Tool;
import snap.view.ViewEvent;
import rmdraw.scene.*;

/**
 * RMTool subclass to provide UI editing for RMGraphPartLabelAxis.
 */
public class RMGraphPartLabelAxisTool <T extends RMGraphPartLabelAxis> extends Tool<T> {

/**
 * Initialize UI.
 */
protected void initUI()  { enableEvents("ItemKeyText", DragDrop); }

/**
 * Resets UI controls.
 */
public void resetUI()
{
    // Get the selected label axis
    RMGraphPartLabelAxis labelAxis = getSelView(); if(labelAxis==null) return;
    
    // Update ShowLabelsCheckBox, ShowGridLinesCheckBox, ItemKeyText, LabelRollSpinner
    setViewValue("ShowLabelsCheckBox", labelAxis.getShowAxisLabels());
    setViewValue("ShowGridLinesCheckBox", labelAxis.getShowGridLines());
    setViewValue("ItemKeyText", labelAxis.getItemKey());
    setViewValue("LabelRollSpinner", labelAxis.getRoll());
}

/**
 * Responds to UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get the selected label axis
    RMGraphPartLabelAxis labelAxis = getSelView();
    
    // Handle ShowLabelsCheckBox, ShowGridLinesCheckBox, ItemKeyText, LabelRollSpinner
    if(anEvent.equals("ShowLabelsCheckBox")) labelAxis.setShowAxisLabels(anEvent.getBoolValue());
    if(anEvent.equals("ShowGridLinesCheckBox")) labelAxis.setShowGridLines(anEvent.getBoolValue());
    if(anEvent.equals("ItemKeyText")) labelAxis.setItemKey(anEvent.getStringValue());
    if(anEvent.equals("LabelRollSpinner")) labelAxis.setRoll(anEvent.getFloatValue());
}

/**
 * Override to return tool shape class.
 */
public Class <T> getViewClass()  { return (Class<T>)RMGraphPartLabelAxis.class; }

/**
 * Returns the currently selected RMGraphPartBars.
 */
public T getSelView()
{
    RMGraph graph = getSelectedGraph();
    return graph!=null? (T)graph.getLabelAxis() : null;
}

/**
 * Returns the currently selected graph area shape.
 */
public RMGraph getSelectedGraph()
{
    Editor e = getEditor(); if(e==null) return null;
    SGView selShape = e.getSelOrSuperSelView();
    return selShape instanceof RMGraph? (RMGraph)selShape : null;
}

/**
 * Returns the name of the graph inspector.
 */
public String getWindowTitle()  { return "Graph Label Axis Inspector"; }

/**
 * Override to remove handles.
 */
public int getHandleCount(T aView)  { return 0; }

}