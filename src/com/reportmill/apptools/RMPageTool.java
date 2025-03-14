/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.*;
import snap.gfx.Image;
import snap.view.*;
import snap.viewx.DialogBox;

/**
 * This class provides UI editing for RMPage shapes.
 */
public class RMPageTool<T extends RMPage> extends RMParentShapeTool<T> {

    // The Layers table
    private TableView<RMPageLayer> _layersTable;

    // Icons
    private Image _visibleIcon = getImage("LayerVisible.png");
    private Image _invisibleIcon = getImage("LayerInvisible.png");
    private Image _lockedIcon = getImage("LayerLocked.png");

    /**
     * Initialize UI panel for this tool.
     */
    protected void initUI()
    {
        // Configure LayersTable CellConfigure and MouseClicks
        _layersTable = getView("LayersTable", TableView.class);
        _layersTable.setCellConfigure(this::configureLayersTable);
        addViewEventHandler(_layersTable, this::handleLayersTableMouseRelease, MouseRelease);
        addViewEventHandler("DatasetKeyText", this::handleDatasetKeyTextEvent, DragDrop);
    }

    /**
     * Updates the UI controls from currently selected page.
     */
    public void resetUI()
    {
        RMPage page = getSelectedShape(); if (page == null) return;

        // Update DatasetKeyText, PaintBackCheckBox
        setViewValue("DatasetKeyText", page.getDatasetKey());
        setViewValue("PaintBackCheckBox", page.getPaintBackground());

        // Update AddButton, RemoveButton, RenameButton, MergeButton enabled state
        setViewEnabled("AddButton", page.getLayerCount() > 0);
        setViewEnabled("RemoveButton", page.getLayerCount() > 1);
        setViewEnabled("RenameButton", page.getLayerCount() > 0);
        setViewEnabled("MergeButton", page.getLayerCount() > 1 && page.getSelectedLayerIndex() > 0);

        // Update layers table selection
        _layersTable.setItems(page.getLayers());
        _layersTable.setSelIndex(page.getSelectedLayerIndex());
    }

    /**
     * Updates currently selected page from the UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        RMPage page = getSelectedShape(); if (page == null) return;

        // Handle DatasetKeyText, PaintBackCheckBox
        if (anEvent.equals("DatasetKeyText"))
            handleDatasetKeyTextEvent(anEvent);
        if (anEvent.equals("PaintBackCheckBox"))
            page.setPaintBackground(anEvent.getBoolValue());

        // Handle AddButton
        if (anEvent.equals("AddButton"))
            page.addLayerNamed("Layer " + (page.getLayerCount() + 1));

        // Handle RemoveButton
        if (anEvent.equals("RemoveButton")) {
            int index = getViewSelIndex("LayersTable");
            page.removeLayer(index);
        }

        // Handle MergeButton
        if (anEvent.equals("MergeButton")) {

            // Get selected layer and index
            RMPageLayer layer = page.getSelectedLayer();
            int index = page.getSelectedLayerIndex();

            // If index is less than layer count
            if (index < page.getLayerCount()) {
                RMPageLayer resultingLayer = page.getLayer(index - 1);
                resultingLayer.addChildren(layer.getChildren());
                layer.removeChildren();
                page.removeLayer(layer);
            }
        }

        // Handle RenameButton
        if (anEvent.equals("RenameButton")) {
            int srow = getViewSelIndex("LayersTable");
            RMPageLayer layer = page.getLayer(srow);
            DialogBox dbox = new DialogBox("Rename Layer");
            dbox.setQuestionMessage("Layer Name:");
            String newName = dbox.showInputDialog(getUI(), layer.getName());
            if (newName != null && !newName.isEmpty())
                layer.setName(newName);
        }

        // Handle LayersTable
        if (anEvent.equals("LayersTable")) {
            int row = _layersTable.getSelIndex(); if (row < 0) return;
            RMPageLayer layer = page.getLayer(row);
            page.selectLayer(layer);
        }

        // Handle AllVisibleButton
        if (anEvent.equals("AllVisibleButton"))
            for (int i = 0, iMax = page.getLayerCount(); i < iMax; i++)
                page.getLayer(i).setLayerState(RMPageLayer.StateVisible);

        // Handle AllVisibleButton
        if (anEvent.equals("AllInvisibleButton"))
            for (int i = 0, iMax = page.getLayerCount(); i < iMax; i++)
                page.getLayer(i).setLayerState(RMPageLayer.StateInvisible);

        // Handle AllLockedButton
        if (anEvent.equals("AllLockedButton"))
            for (int i = 0, iMax = page.getLayerCount(); i < iMax; i++)
                page.getLayer(i).setLayerState(RMPageLayer.StateLocked);
    }

    /**
     * Configure LayersTable: Set image for second column.
     */
    public void configureLayersTable(ListCell<RMPageLayer> aCell)
    {
        // Get page, cell row/col, page layer
        //RMPage page = getSelectedShape(); if(page==null) return;
        //int row = aCell.getRow(), col = aCell.getCol(); if(row<0 || row>=page.getLayerCount()) return;
        RMPageLayer layer = aCell.getItem(); if (layer == null) return; //page.getLayer(row);
        int col = aCell.getCol();

        // Handle column 0 (layer name)
        if (col == 0) {
            aCell.setText(layer.getName());
        }

        // Handle column 1 (layer state image)
        if (col == 1) {
            int state = layer.getLayerState();
            if (state == RMPageLayer.StateVisible)
                aCell.setImage(_visibleIcon);
            else if (state == RMPageLayer.StateInvisible)
                aCell.setImage(_invisibleIcon);
            else if (state == RMPageLayer.StateLocked)
                aCell.setImage(_lockedIcon);
            aCell.setText(null);
        }
    }

    /**
     * Called when LayersTable gets MouseReleased event.
     */
    private void handleLayersTableMouseRelease(ViewEvent anEvent)
    {
        RMPage page = getSelectedShape(); if (page == null) return;
        int col = _layersTable.getSelColIndex();

        // If column one was selected, cycle through layer states
        if (anEvent.isMouseClick() && col == 1) {
            RMPageLayer layer = page.getSelectedLayer();
            int state = layer.getLayerState();
            if (state == RMPageLayer.StateVisible)
                layer.setLayerState(RMPageLayer.StateInvisible);
            else if (state == RMPageLayer.StateInvisible)
                layer.setLayerState(RMPageLayer.StateLocked);
            else layer.setLayerState(RMPageLayer.StateVisible);
            _layersTable.updateItems();
        }
    }

    /**
     * Called when DatasetKeyText gets Action or DragDrop event.
     */
    private void handleDatasetKeyTextEvent(ViewEvent anEvent)
    {
        RMPage page = getSelectedShape(); if (page == null) return;
        page.setDatasetKey(anEvent.getStringValue().replace("@", ""));
        resetLater();
    }

    /**
     * Returns the shape class that this tool is responsible for.
     */
    public Class<T> getShapeClass()  { return (Class<T>) RMPage.class; }

    /**
     * Returns the name to be used for this tool in the inspector window title.
     */
    public String getWindowTitle()  { return "Page Inspector"; }

    /**
     * Overrides tool method to declare that pages have no handles.
     */
    public int getHandleCount(T aShape)  { return 0; }
}