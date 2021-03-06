/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.apptools;
import reportmill.shape.RMCrossTab;
import reportmill.shape.RMCrossTabCell;
import rmdraw.app.*;
import rmdraw.apptools.TextTool;
import rmdraw.app.Tool;
import reportmill.util.RMGrouping;
import rmdraw.scene.*;
import java.util.List;

import snap.geom.Point;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;

/**
 * Provides UI editing for RMCell shapes.
 */
public class RMCrossTabCellTool <T extends RMCrossTabCell> extends TextTool<T> implements RMSortPanel.Owner {

    // The sort panel
    RMSortPanel     _sortPanel;
    
    // Cached mouse pressed event in case cell tool needs to start forwarding events to table tool
    ViewEvent       _mousePressedEvent;

/**
 * Creates UI panel - base panel is TextTool version.
 */
protected View createUI()
{
    // Get UI for this tool
    ChildView cellUI = (ChildView)createUI(getClass());
    
    // Create SortPanel, set bounds and install
    _sortPanel = new RMSortPanel(this);
    _sortPanel.getUI().setBounds(4, 45, 267, 100);
    cellUI.addChild(_sortPanel.getUI(), 1);
    
    // Get super UI (TextTool UI - a ColView) and add UI for this tool
    ColView colView = (ColView)createUI(TextTool.class);
    colView.setGrowHeight(false); // So inspector will scroll
    colView.addChild(cellUI, 0);
    return colView;
}

/**
 * Called to initialize UI.
 */
protected void initUI()  { super.initUI(); enableEvents("GroupingKeyText", DragDrop); }

/**
 * Updates UI from currently selected cell.
 */
public void resetUI()
{
    // Do normal reset ui
    super.resetUI();
    
    // Get currently selected cell (just return if null)
    RMCrossTabCell cell = getSelView(); if(cell==null) return;
    
    // Get whether cell is in header row or column
    boolean isHeaderCell = cell.isColHeader() || cell.isRowHeader();
    
    // Update GroupingKeyText
    if(isHeaderCell)
        setViewValue("GroupingKeyText", cell.getGrouping()==null? null : cell.getGrouping().getKey());
    
    // Update GroupingKeyText for invalid case
    else setViewValue("GroupingKeyText", "(header cells only)");
    
    // Set GroupingKeyText enabled
    setViewEnabled("GroupingKeyText", isHeaderCell);
    
    // Set GroupingKeyLabel enabled
    setViewEnabled("GroupingKeyLabel", isHeaderCell);
    
    // Update VisibleCheckBox
    setViewValue("VisibleCheckBox", cell.isVisible());
    
    // Update sortpanel
    _sortPanel.resetUI();
    
    // Update border checkboxes
    setViewValue("ShowLeftBorderCheckBox", cell.isShowLeftBorder());
    setViewValue("ShowRightBorderCheckBox", cell.isShowRightBorder());
    setViewValue("ShowTopBorderCheckBox", cell.isShowTopBorder());
    setViewValue("ShowBottomBorderCheckBox", cell.isShowBottomBorder());
}

/**
 * Updates currently selected cell from UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected cell and table (just return if null)
    RMCrossTabCell cell = getSelView(); if(cell==null) return;
    RMCrossTab table = (RMCrossTab)cell.getParent();
    
    // Get selected cross tab cell views
    List <RMCrossTabCell> cells = (List)getEditor().getSelOrSuperSelViews();
    
    // Handle GroupingKeyText
    if(anEvent.equals("GroupingKeyText")) {
        
        // Get grouping key
        String key = StringUtils.delete(anEvent.getStringValue(), "@");
        
        // If no key, reset grouping
        if(key==null || key.length()==0)
            cell.setGrouping(null);
        
        // If cell grouping is null, create grouping
        else if(cell.getGrouping()==null)
            cell.setGrouping(new RMGrouping(key));
        
        // Otherwise, just set cell grouping
        else cell.getGrouping().setKey(key);
    }
    
    // Handle VisibleCheckBox - Set visible on selected cells
    if(anEvent.equals("VisibleCheckBox"))
        for(RMCrossTabCell cll : cells)
            cll.setVisible(anEvent.getBoolValue());
    
    // Handle ShowLeftBorderCheckBox - Set visible on selected cells
    if(anEvent.equals("ShowLeftBorderCheckBox"))
        for(RMCrossTabCell cll : cells)
            cll.setShowLeftBorder(anEvent.getBoolValue());
    
    // Handle ShowRightBorderCheckBox - Set visible on selected cells
    if(anEvent.equals("ShowRightBorderCheckBox"))
        for(RMCrossTabCell cll : cells)
            cll.setShowRightBorder(anEvent.getBoolValue());
    
    // Handle ShowTopBorderCheckBox - Set visible on selected cells
    if(anEvent.equals("ShowTopBorderCheckBox"))
        for(RMCrossTabCell cll : cells)
            cll.setShowTopBorder(anEvent.getBoolValue());
    
    // Handle ShowBottomBorderCheckBox - Set visible on selected cells
    if(anEvent.equals("ShowBottomBorderCheckBox"))
        for(RMCrossTabCell cll : cells)
            cll.setShowBottomBorder(anEvent.getBoolValue());
    
    // Register for layout
    table.relayout();
    
    // Do text tool respondUI
    super.respondUI(anEvent);    
}

/**
 * Returns the currently selected cell.
 */
public RMCrossTabCell getCell()  { return ClassUtils.getInstance(getSelView(), RMCrossTabCell.class); }

/**
 * Returns the grouping of the selected cell.
 */
public RMGrouping getGrouping()
{
    RMCrossTabCell cell = getCell(); if(cell==null) return null;
    return cell.getGrouping();
}

/**
 * Event handling - overrides text tool to pass handling to table tool if user really wants to select cells.
 */
protected void processMouseEvent(T aText, ViewEvent anEvent)
{
    // Get cell table and tool
    RMCrossTab table = aText.getTable();
    Tool tableTool = getTool(table);
    
    // If event is popup trigger, run crosstab popup
    if(anEvent.isPopupTrigger()) {
        tableTool.processEvent(table, anEvent); return; }
    
    // Handle MousePressed
    if(anEvent.isMousePress())
        _mousePressedEvent = anEvent; // Cache mouse pressed event
        
    // Handle MouseDragged: Pass to table tool if user really wants to select cells.
    else if(anEvent.isMouseDrag()) {
        
        // If mouse pressed event is null, forward events to table
        if(_mousePressedEvent==null) {
            tableTool.processEvent(table, anEvent); return; }
        
        // Get event point in cell coords
        Editor editor = getEditor();
        Point point = editor.convertToSceneView(anEvent.getX(), anEvent.getY(), aText);
        
        // If point is outside cell, start sending to table tool
        if(point.getX()<-20 || point.getY()<-20 || point.getX()> aText.getWidth()+20 || point.getY()> aText.getHeight()+20) {
            
            // Make cell selected instead of super selected
            editor.setSelView(aText);
            
            // Send table table mouse pressed
            tableTool.processEvent(table, _mousePressedEvent);
            
            // Clear mouse pressed event so we'll know that events should be forwarded
            _mousePressedEvent = null;
            
            // Send current mouse dragged event to table and return
            tableTool.processEvent(table, anEvent);
            return;
        }
    }
    
    // Handle MouseReleased: in case cell tool needs to forward events to table tool.
    else if(anEvent.isMouseRelease()) {
    
        // If mouse pressed event is null, forward on to table tool
        if(_mousePressedEvent==null) {
            tableTool.processEvent(table, anEvent); _mousePressedEvent = null; return; }
        
        // Clear mouse pressed event
        _mousePressedEvent = null;
    }
    
    // Call normal text tool mouse released
    super.processMouseEvent(aText, anEvent);
}

/**
 * Key event handler for super selected cell.
 */
protected void processKeyEvent(T aView, ViewEvent anEvent)
{
    // Get key code
    int keyCode = anEvent.getKeyCode();
        
    // If key is tab press (non-alt), move forward or backward (based on shift modifier)
    if(keyCode==KeyCode.TAB && !anEvent.isAltDown() && anEvent.isKeyPress())
        getEditor().setSelView(anEvent.isShiftDown()? aView.getCellBefore() : aView.getCellAfter());
            
    // If key is enter press (non-alt), move down or up
    else if(keyCode==KeyCode.ENTER && !anEvent.isAltDown() && anEvent.isKeyPress())
        getEditor().setSelView(anEvent.isShiftDown()? aView.getCellAbove() : aView.getCellBelow());
        
    // Anything else goes to text tool
    else { super.processKeyEvent(aView, anEvent); return; }

    // Consume event
    anEvent.consume();
}

/**
 * Overrides tool method to indicate that cells have no handles.
 */
public int getHandleCount(T aView)  { return 0; }

/**
 * Override to suppress normal TextTool painting.
 */
public void paintHandles(T aView, Painter aPntr, boolean isSuperSelected)  { }

/**
 * Override to suppress normal TextTool painting.
 */
public void paintBoundsRect(SGText aText, Painter aPntr) { }

/**
 * Override normal implementation to handle KeysPanel drop.
 */
public void dragDrop(T aView, ViewEvent anEvent)
{
    // If KeysPanel is dragging, add key to text
    Clipboard cb = anEvent.getClipboard();
    if(cb.hasString()) {
    
        // Do normal text version to add drop string to text
        super.dragDrop(aView, anEvent);
    
        // Get the string
        String string = cb.getString(); //ClipboardUtils.getString(anEvent.getTransferable());
    
        // If this cell is header row or header column and there is no grouping, set grouping
        if((aView.isColHeader() || aView.isRowHeader()) && aView.getGrouping()==null) {
            String key = StringUtils.delete(string, "@"); // Get key (drop string without @-signs)
            aView.setGrouping(new RMGrouping(key)); // Set new grouping
        }
    }
    
    // Otherwise do normal version
    else super.dragDrop(aView, anEvent);
}

}