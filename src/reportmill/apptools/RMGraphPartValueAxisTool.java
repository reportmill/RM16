/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.apptools;
import reportmill.shape.*;
import rmdraw.app.Editor;
import rmdraw.app.Tool;
import rmdraw.scene.*;
import snap.view.*;

/**
 * RMTool subclass to provide UI editing for RMGraphPartValueAxis.
 */
public class RMGraphPartValueAxisTool <T extends RMGraphPartValueAxis> extends Tool<T> {
    
/**
 * Resets UI panel controls.
 */
public void resetUI()
{
    // Get the selected value axis (just return if null)
    RMGraphPartValueAxis valueAxis = getSelView(); if(valueAxis==null) return;
    
    // Update ShowLabelsCheckBox, ShowMajorGridCheckBox, ShowMinorGridCheckBox, LabelRollSpinner
    setViewValue("ShowLabelsCheckBox", valueAxis.getShowAxisLabels());
    setViewValue("ShowMajorGridCheckBox", valueAxis.getShowMajorGrid());
    setViewValue("ShowMinorGridCheckBox", valueAxis.getShowMinorGrid() && valueAxis.getShowMajorGrid());
    setViewEnabled("ShowMinorGridCheckBox", valueAxis.getShowMajorGrid());
    setViewValue("LabelRollSpinner", valueAxis.getRoll());
    
    // Update AxisMinText, AxisMaxText, AxisCountSpinner
    if(valueAxis.getAxisMin()==Float.MIN_VALUE) setViewValue("AxisMinText", "");
    else setViewValue("AxisMinText", valueAxis.getAxisMin());
    if(valueAxis.getAxisMax()==Float.MIN_VALUE) setViewValue("AxisMaxText", "");
    else setViewValue("AxisMaxText", valueAxis.getAxisMax());
    setViewValue("AxisCountSpinner", valueAxis.getAxisCount());
}

/**
 * Responds to UI panel controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get the selected value axis (just return if null)
    RMGraphPartValueAxis valueAxis = getSelView(); if(valueAxis==null) return;
    
    // Handle ShowLabelsCheckBox, ShowMajorGridCheckBox, ShowMinorGridCheckBox, LabelRollSpinner
    if(anEvent.equals("ShowLabelsCheckBox")) valueAxis.setShowAxisLabels(anEvent.getBoolValue());
    if(anEvent.equals("ShowMajorGridCheckBox")) valueAxis.setShowMajorGrid(anEvent.getBoolValue());
    if(anEvent.equals("ShowMinorGridCheckBox")) valueAxis.setShowMinorGrid(anEvent.getBoolValue());
    if(anEvent.equals("LabelRollSpinner")) valueAxis.setRoll(anEvent.getFloatValue());
    
    // Handle AxisMinText, AxisMaxText, AxisCountSpinner
    if(anEvent.equals("AxisMinText"))
        valueAxis.setAxisMin(anEvent.getStringValue().length()>0? anEvent.getFloatValue() : Float.MIN_VALUE);
    if(anEvent.equals("AxisMaxText"))
        valueAxis.setAxisMax(anEvent.getStringValue().length()>0? anEvent.getFloatValue() : Float.MIN_VALUE);
    if(anEvent.equals("AxisCountSpinner"))
        valueAxis.setAxisCount(anEvent.getIntValue());
}

/**
 * Override to return tool shape class.
 */
public Class <T> getViewClass()  { return (Class<T>)RMGraphPartValueAxis.class; }

/**
 * Returns the currently selected RMGraphPartBars.
 */
public T getSelView()
{
    RMGraph graph = getSelectedGraph();
    return graph!=null? (T)graph.getValueAxis() : null;
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
public String getWindowTitle()  { return "Graph Value Axis Inspector"; }

/**
 * Override to remove handles.
 */
public int getHandleCount(T aView)  { return 0; }

}