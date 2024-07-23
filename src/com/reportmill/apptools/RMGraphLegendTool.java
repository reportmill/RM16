/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.*;
import snap.view.ViewEvent;

/**
 * Provides UI editing for RMGraphLegend.
 */
public class RMGraphLegendTool<T extends RMGraphLegend> extends RMParentShapeTool<T> {

    /**
     * Override to configure UI.
     */
    protected void initUI()
    {
        addViewEventHandler("LegendText", this::handleLegendTextDragDropEvent, DragDrop);
    }

    /**
     * Reset UI.
     */
    public void resetUI()
    {
        // Get selected legend
        RMGraphLegend legend = getSelectedShape();

        // Update LegendText
        setViewText("LegendText", legend.getLegendText());
    }

    /**
     * Respond UI.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Get selected legend
        RMGraphLegend legend = getSelectedShape();

        // Handle LegendText
        if (anEvent.equals("LegendText"))
            legend.setLegendText(anEvent.getStringValue());
    }

    /**
     * Called when LegendText gets DragDrop event.
     */
    private void handleLegendTextDragDropEvent(ViewEvent anEvent)
    {
        RMGraphLegend legend = getSelectedShape();
        legend.setLegendText(anEvent.getStringValue());
        resetLater();
    }

    /**
     * Returns the name of the graph inspector.
     */
    public String getWindowTitle()  { return "Graph Legend Inspector"; }

    /**
     * Override to make RMGraphLegend not super-selectable.
     */
    public boolean isSuperSelectable(RMShape aShape)  { return false; }
}