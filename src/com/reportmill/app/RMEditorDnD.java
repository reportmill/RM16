/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.shape.*;
import snap.geom.Rect;
import snap.view.*;

/**
 * Handles editor methods specific to drag and drop operations.
 */
class RMEditorDnD {

    // The editor that this class is working for
    private RMEditor _editor;

    // The last shape that a drag and drop action was over
    private RMShape _lastOverShape;

    /**
     * Constructor.
     */
    public RMEditorDnD(RMEditor anEditor)
    {
        _editor = anEditor;
    }

    /**
     * Handle events.
     */
    protected void handleEditorDragEvent(ViewEvent anEvent)
    {
        switch (anEvent.getType()) {
            case DragEnter -> dragEnter(anEvent);
            case DragOver -> dragOver(anEvent);
            case DragExit -> dragExit(anEvent);
            case DragDrop -> dragDrop(anEvent);
            default -> throw new RuntimeException("RMEditorDnD: Unknown event type: " + anEvent.getType());
        }
    }

    /**
     * Drop target listener method.
     */
    private void dragEnter(ViewEvent anEvent)
    {
        _lastOverShape = null;  // Reset last over shape and last drag point
        dragOver(anEvent);                             // Do a drag over to get things started
    }

    /**
     * Drop target listener method.
     */
    private void dragOver(ViewEvent anEvent)
    {
        // Windows calls this method continuously, as long as the mouse is held down
        //if(anEvent.getPoint().equals(_lastDragPoint)) return; _lastDragPoint = anEvent.getPoint();

        // Accept drag
        anEvent.acceptDrag(); //DnDConstants.ACTION_COPY);

        // Get shape at drag point (or the page, if none there)
        RMShape overShape = _editor.getShapeAtPoint(anEvent.getPoint(), true);
        if (overShape == null)
            overShape = _editor.getSelPage();

        // Go up chain until we find a shape that accepts drag
        while (!_editor.getTool(overShape).acceptsDrag(overShape, anEvent))
            overShape = overShape.getParent();

        // If new overShape, do drag exit/enter and reset border
        if (overShape != _lastOverShape) {

            // Send drag exit
            if (_lastOverShape != null)
                _editor.getTool(_lastOverShape).dragExit(_lastOverShape, anEvent);

            // Send drag enter
            _editor.getTool(overShape).dragEnter(overShape, anEvent);

            // Get bounds of over shape in editor coords
            Rect bounds = overShape.getBoundsInside();
            _editor._dragShape = _editor.convertFromShape(bounds, overShape);
            _editor.repaint();

            // Update last drop shape
            _lastOverShape = overShape;
        }

        // If over shape didn't change, send drag over
        else _editor.getTool(overShape).dragOver(overShape, anEvent);
    }

    /**
     * Drop target listener method.
     */
    private void dragExit(ViewEvent anEvent)
    {
        _editor._dragShape = null;
        _editor.repaint();        // Clear DragShape
        RMEditorProxGuide.clearGuidelines(_editor);          // Reset proximity guide
    }

    /**
     * Drop target listener method.
     */
    private void dragDrop(ViewEvent anEvent)
    {
        // Formally accept drop
        anEvent.acceptDrag();//DnDConstants.ACTION_COPY);

        // Order window front (for any getMainEditor calls, but really should be true anyway)
        _editor.getWindow().toFront();

        // Forward drop to last over shape
        _editor.getTool(_lastOverShape).drop(_lastOverShape, anEvent);

        // Formally complete drop
        anEvent.dropComplete();  //(true);

        // Clear DragShape (which may have been set during dragOver)
        _editor._dragShape = null;
        _editor.repaint();
    }
}