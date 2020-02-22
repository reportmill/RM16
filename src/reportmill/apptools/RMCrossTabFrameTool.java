/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.apptools;
import reportmill.shape.RMCrossTab;
import reportmill.shape.RMCrossTabFrame;
import rmdraw.app.Editor;
import rmdraw.apptools.RMTool;
import rmdraw.shape.*;
import snap.gfx.Point;
import snap.util.StringUtils;
import snap.view.ViewEvent;

/**
 * Provides UI inspector for crosstab frame.
 */
public class RMCrossTabFrameTool <T extends RMCrossTabFrame> extends RMTool<T> {

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
    RMCrossTabFrame tableFrame = getSelectedShape(); if(tableFrame==null) return;
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
    RMCrossTabFrame tableFrame = getSelectedShape(); if(tableFrame==null) return;
    RMCrossTab table = tableFrame.getTable();
    
    // Handle DatasetKeyText, FilterKeyText, ReprintHeaderRowsCheckBox
    if(anEvent.equals("DatasetKeyText")) table.setDatasetKey(StringUtils.delete(anEvent.getStringValue(), "@"));
    if(anEvent.equals("FilterKeyText")) table.setFilterKey(StringUtils.delete(anEvent.getStringValue(), "@"));
    if(anEvent.equals("ReprintHeaderRowsCheckBox")) tableFrame.setReprintHeaderRows(anEvent.getBoolValue());
}

/**
 * Event handling from select tool for super selected shapes.
 */
public void mousePressed(T aCTabFrame, ViewEvent anEvent)
{
    // Get event point in TableFrame coords
    Editor editor = getEditor();
    Point point = editor.convertToShape(anEvent.getX(), anEvent.getY(), aCTabFrame);
    
    // Handle mouse press in crosstab when not superselected
    if(editor.isSelected(aCTabFrame)) {
        
        // If click was inside table bounds, super select table and consume event
        if(point.getX()<aCTabFrame.getTable().getWidth() && point.getY()<aCTabFrame.getTable().getHeight()) {
            editor.setSuperSelectedShape(aCTabFrame.getTable());
            anEvent.consume();
        }
    }
    
    // Handle mouse press in super selected crosstab's buffer region
    if(editor.isSuperSelected(aCTabFrame)) {
        
        // If click was outside table bounds, make table frame just selected
        if(point.getX()>=aCTabFrame.getTable().getWidth() || point.getY()>=aCTabFrame.getTable().getHeight()) {
            editor.setSelectedShape(aCTabFrame);
            editor.getSelectTool().setRedoMousePressed(true); // Register for redo
        }
    }
}

/**
 * Returns the shape class this tool edits (RMTable).
 */
public Class getShapeClass()  { return RMCrossTabFrame.class; }

/**
 * Returns the display name for this tool ("Table Inspector").
 */
public String getWindowTitle()  { return "CrossTab Frame Inspector"; }

/**
 * Overridden to make crosstab frame super-selectable.
 */
public boolean isSuperSelectable(RMShape aShape)  { return true; }

}