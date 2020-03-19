/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.apptools;
import reportmill.shape.RMCrossTab;
import reportmill.shape.RMCrossTabFrame;
import rmdraw.app.Editor;
import rmdraw.app.Tool;
import rmdraw.scene.*;
import snap.geom.Point;
import snap.util.StringUtils;
import snap.view.ViewEvent;

/**
 * Provides UI inspector for crosstab frame.
 */
public class RMCrossTabFrameTool <T extends RMCrossTabFrame> extends Tool<T> {

/**
 * Called to initialize UI.
 */
protected void initUI()  { enableEvents("DatasetKeyText", DragDrop); enableEvents("FilterKeyText", DragDrop); }

/**
 * Updates UI controls from the currently selected crosstab frame.
 */
public void resetUI()
{
    // Get the currently selected crosstab frame and table (just return if null)
    RMCrossTabFrame tableFrame = getSelView(); if(tableFrame==null) return;
    RMCrossTab table = tableFrame.getTable();
    
    // Update the DatasetKeyText, FilterKeyText, ReprintHeaderRowsCheckBox
    setViewValue("DatasetKeyText", table.getDatasetKey());
    setViewValue("FilterKeyText", table.getFilterKey());
    setViewValue("ReprintHeaderRowsCheckBox", tableFrame.getReprintHeaderRows());
}

/**
 * Updates the currently selected crosstab from from UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get the currently selected crosstab frame and table (just return if null)
    RMCrossTabFrame tableFrame = getSelView(); if(tableFrame==null) return;
    RMCrossTab table = tableFrame.getTable();
    
    // Handle DatasetKeyText, FilterKeyText, ReprintHeaderRowsCheckBox
    if(anEvent.equals("DatasetKeyText")) table.setDatasetKey(StringUtils.delete(anEvent.getStringValue(), "@"));
    if(anEvent.equals("FilterKeyText")) table.setFilterKey(StringUtils.delete(anEvent.getStringValue(), "@"));
    if(anEvent.equals("ReprintHeaderRowsCheckBox")) tableFrame.setReprintHeaderRows(anEvent.getBoolValue());
}

/**
 * Event handling from select tool for super selected shapes.
 */
public void mousePressed(T aView, ViewEvent anEvent)
{
    // Get event point in TableFrame coords
    Editor editor = getEditor();
    Point point = editor.convertToSceneView(anEvent.getX(), anEvent.getY(), aView);
    
    // Handle mouse press in crosstab when not superselected
    if(editor.isSelected(aView)) {
        
        // If click was inside table bounds, super select table and consume event
        if(point.getX()< aView.getTable().getWidth() && point.getY()< aView.getTable().getHeight()) {
            editor.setSuperSelView(aView.getTable());
            anEvent.consume();
        }
    }
    
    // Handle mouse press in super selected crosstab's buffer region
    if(editor.isSuperSelected(aView)) {
        
        // If click was outside table bounds, make table frame just selected
        if(point.getX()>= aView.getTable().getWidth() || point.getY()>= aView.getTable().getHeight()) {
            editor.setSelView(aView);
            editor.getSelectTool().setRedoMousePressed(true); // Register for redo
        }
    }
}

/**
 * Returns the shape class this tool edits (RMTable).
 */
public Class getViewClass()  { return RMCrossTabFrame.class; }

/**
 * Returns the display name for this tool ("Table Inspector").
 */
public String getWindowTitle()  { return "CrossTab Frame Inspector"; }

/**
 * Overridden to make crosstab frame super-selectable.
 */
public boolean isSuperSelectable(SGView aShape)  { return true; }

}