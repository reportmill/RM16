/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.apptools;
import reportmill.shape.RMTableRow;
import rmdraw.app.Editor;
import rmdraw.apptools.TextTool;
import rmdraw.scene.*;
import snap.geom.Point;
import snap.geom.Rect;
import snap.gfx.Painter;
import snap.view.KeyCode;
import snap.view.ViewEvent;

import java.util.List;

/**
 * This class provides UI editing for text shapes.
 */
public class TableTextTool<T extends SGText> extends TextTool<T> {

    // Whether current mouse drag should be moving table column
    private boolean  _moveTableColumn;

    /**
     * Editor method - returns handle count.
     */
    public int getHandleCount(T aText)
    {
        return isStructured(aText)? 2 : super.getHandleCount(aText);
    }

    /**
     * Editor method - returns handle rect in editor coords.
     */
    public Rect getHandleRect(T aText, int handle, boolean isSuperSelected)
    {
        // If structured, return special handles (tall & thin)
        if (isStructured(aText)) {

            // Get handle point in text bounds, convert to table row bounds
            Point cp = getHandlePoint(aText, handle, true);
            cp = aText.localToParent(cp);

            // If point outside of parent, return bogus rect
            if (cp.getX()<0 || cp.getX()>aText.getParent().getWidth())
                return new Rect(-9999,-9999,0,0);

            // Get handle point in text coords
            cp = getHandlePoint(aText, handle, false);

            // Get handle point in editor coords
            cp = getEditor().convertFromSceneView(cp.getX(), cp.getY(), aText);

            // Get handle rect
            Rect hr = new Rect(cp.getX()-3, cp.getY(), 6, aText.height() * getEditor().getZoomFactor());

            // If super selected, offset
            if (isSuperSelected)
                hr.offset(handle==0? -2 : 2, 0);

            // Return handle rect
            return hr;
        }

        // Return normal shape handle rect
        return super.getHandleRect(aText, handle, isSuperSelected);
    }

    /**
     * Paints selected shape indicator, like handles (and maybe a text linking indicator).
     */
    public void paintHandles(T aText, Painter aPntr, boolean isSuperSelected)
    {
        // If not structured, do normal version
        if (!isStructured(aText)) {
            super.paintHandles(aText, aPntr, isSuperSelected);
            return;
        }

        // Paint SuperSelected
        if (isSuperSelected)
            paintTextEditor(aText, aPntr);

        // Iterate over shape handles, get rect and draw
        aPntr.setAntialiasing(false);
        for (int i=0, iMax=getHandleCount(aText); i<iMax; i++) {
            Rect hr = getHandleRect(aText, i, isSuperSelected);
            aPntr.drawButton(hr, false);
        }
        aPntr.setAntialiasing(true);
    }

    /**
     * Returns whether to paint text link indicator.
     */
    protected boolean isPaintingTextLinkIndicator(SGText aText)
    {
        // If text is child of table row, return false
        if (aText.getParent() instanceof RMTableRow) return false;
        return super.isPaintingTextLinkIndicator(aText);
    }

    /**
     * Event handling for shape editing (just forwards to text editor).
     */
    protected void processMouseEvent(T aText, ViewEvent anEvent)
    {
        // If MoveTableColumn, forward to moveTableColumn()
        if (_moveTableColumn) {
            moveTableColumn(anEvent);
            return;
        }

            // If text is a structured table row column and point is outside column, start MoveTableRow
        else if (anEvent.isMouseDrag()) {
            SGText tshp = aText;
            Point pnt = getEditor().convertToSceneView(anEvent.getX(), anEvent.getY(), aText);
            double px = pnt.getX();
            if (isStructured(tshp) && (px < -20 || px > tshp.getWidth() + 10) && tshp.getParent().getChildCount() > 1) {
                setUndoTitle("Reorder columns");
                getEditor().setSelView(tshp);
                _moveTableColumn = true;
                return;
            }
        }

        // Otherwise, do normal version
        super.processMouseEvent(aText, anEvent);
    }

    /**
     * Key event handling for super selected text.
     */
    protected void processKeyEvent(T aText, ViewEvent anEvent)
    {
        // If tab was pressed and text is structured table row column, forward selection onto next column
        if (isStructured(aText) && anEvent.isKeyPress() && anEvent.getKeyCode()== KeyCode.TAB && !anEvent.isAltDown()) {

            // Get structured text table row, child table rows and index of child
            SGParent tableRow = aText.getParent();
            List children = SGViewUtils.getShapesSortedByX(tableRow.getChildren());
            int index = children.indexOf(aText);

            // If shift is down, get index to the left, wrapped, otherwise get index to the right, wrapped
            if(anEvent.isShiftDown()) index = (index - 1 + children.size())%children.size();
            else index = (index + 1)%children.size();

            // Get next text and super-select
            SGView nextText = (SGView)children.get(index);
            getEditor().setSuperSelView(nextText);

            // Consume event and return
            anEvent.consume();
        }

        // Otherwise, do normal version
        else super.processKeyEvent(aText, anEvent);
    }

    /**
     * Moves the handle at the given index to the given point.
     */
    public void moveShapeHandle(T aShape, int aHandle, Point toPoint)
    {
        // If not structured, do normal version
        if (!isStructured(aShape)) {
            super.moveShapeHandle(aShape, aHandle, toPoint);
            return;
        }

        // Get handle point in shape coords and shape parent coords
        Point p1 = getHandlePoint(aShape, aHandle, false);
        Point p2 = aShape.parentToLocal(toPoint);

        // Get whether left handle and width change
        boolean left = aHandle==HandleW || aHandle==HandleNW || aHandle==HandleSW;
        double dw = p2.getX() - p1.getX(); if (left) dw = -dw;
        double nw = aShape.getWidth() + dw;
        if (nw<8) { nw = 8; dw = nw - aShape.getWidth(); }

        // Get shape to adjust and new width (make sure it's no less than 8)
        int index = aShape.indexOf();
        int index2 = left? index-1 : index+1;
        SGView other = aShape.getParent().getChild(index2);
        double nw2 = other.getWidth() - dw;
        if (nw2<8) { nw2 = 8; dw = other.getWidth() - nw2; nw = aShape.getWidth() + dw; }

        // Adjust shape and layout parent
        aShape.setWidth(nw);
        other.setWidth(nw2);
        aShape.getParent().layout();
    }

    /**
     * Move Table Column stuff (table row column re-ordering).
     */
    private void moveTableColumn(ViewEvent anEvent)
    {
        // Get editor, editor SelectedShape and TableRow
        Editor editor = getEditor();
        SGView shape = editor.getSelOrSuperSelView();
        SGParent tableRow = shape.getParent();
        tableRow.repaint();

        // Get event x in TableRow coords and whether point is in TableRow
        Point point = editor.convertToSceneView(anEvent.getX(), anEvent.getY(), tableRow); point.y = 2;
        boolean inRow = tableRow.contains(point);

        // Handle MouseDragged: layout children by X (if outside row, skip drag shape)
        if (anEvent.isMouseDrag()) {
            List <SGView> children = SGViewUtils.getShapesSortedByFrameX(tableRow.getChildren());
            float x = 0;
            for (SGView child : children) {
                if (child==shape) { if(inRow) child.setX(point.x-child.getWidth()/2);
                else { child.setX(9999); continue; }}
                else child.setX(x); x += child.getWidth(); }
        }

        // Handle MouseReleased: reset children
        if (anEvent.isMouseRelease()) {

            // If shape in row, set new index
            if (inRow) {
                int iold = shape.indexOf();
                int inew = 0; while(inew<tableRow.getChildCount() && tableRow.getChild(inew).getX()<=shape.getX()) inew++;
                if (iold!=inew) {
                    tableRow.removeChild(iold); if(inew>iold) inew--;
                    tableRow.addChild(shape, inew);
                }
            }

            // If shape is outside bounds of tableRow, remove it
            else {
                tableRow.removeChild(shape);
                editor.setSuperSelView(tableRow);
            }

            // Do layout again to snap shape back into place
            tableRow.layoutDeep();
            _moveTableColumn = false;
        }
    }

    /**
     * Returns whether given shape is in a Structured TableRow.
     */
    protected boolean isStructured(SGView aShape)
    {
        SGView par = aShape.getParent();
        return par instanceof RMTableRow && ((RMTableRow)par).isStructured();
    }
}