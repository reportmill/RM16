/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.shape.*;
import java.util.*;
import snap.geom.Ellipse;
import snap.geom.Point;
import snap.geom.Rect;
import snap.geom.Shape;
import snap.gfx.*;
import snap.text.TextModelX;
import snap.text.TextSel;
import snap.util.ListUtils;
import snap.view.*;

/**
 * This class handles functionality related to mouse and keyboard input on a viewer, so that different behavior
 * can easily be swapped in beyond the standard interactive behavior, like text selection or area-selection.
 */
public class RMViewerEvents {

    // The viewer
    private RMViewer _viewer;

    // The mode
    private int _mode = 1;

    // The last shape that was hit by a mouse press (PLAYER)
    private RMShape _shapePressed;

    // The stack of shapes under the mouse for mouse moves (PLAYER)
    private Stack<RMShape> _shapeUnderStack = new Stack<>();

    // The stack of cursors (one for each shape in shape stack) for mouse moves (PLAYER)
    private Stack<Cursor> _shapeUnderCursorStack = new Stack<>();

    // The list of text shapes selected (SELECT_TEXT)
    private List<RMTextShape> _selectedTexts = new ArrayList<>();

    // The down point for the last mouse loop (SELECT_TEXT/SELECT_IMAGE)
    private Point _downPoint;

    // The drag point for the last mouse loop (SELECT_TEXT/SELECT_IMAGE)
    private Point _dragPoint;

    // The paint area (SELECT_TEXT)
    private Shape _paintArea = new Rect();

    // The selection rect (SELECT_IMAGE)
    private Rect _rect = new Rect();

    // The selected sides (a mask of sides) (SELECT_IMAGE)
    private int _selectedSides;

    // Constants for mode
    public static final int NONE = 0;
    public static final int DEFAULT = 1;
    public static final int SELECT_TEXT = 2;
    public static final int SELECT_IMAGE = 3;

    // DivdeRect constants
    public static final byte MinXEdge = 1;
    public static final byte MinYEdge = 1 << 1;
    public static final byte MaxXEdge = 1 << 2;
    public static final byte MaxYEdge = 1 << 3;

    /**
     * Constructor.
     */
    public RMViewerEvents(RMViewer aViewer)
    {
        _viewer = aViewer;
    }

    /**
     * Returns the viewer we work for.
     */
    public RMViewer getViewer()  { return _viewer; }

    /**
     * Returns the mode.
     */
    public int getMode()  { return _mode; }

    /**
     * Sets the mode.
     */
    public void setMode(int aMode)
    {
        _mode = aMode;
        getViewer().repaint();
    }

    /**
     * Handle mouse events.
     */
    protected void processEvent(ViewEvent anEvent)
    {
        switch (_mode) {
            case NONE -> { }
            case DEFAULT -> processEventDefault(anEvent);
            case SELECT_TEXT -> processEventSelText(anEvent);
            case SELECT_IMAGE -> processEventSelImage(anEvent);
        }
    }

    /**
     * Handle mouse events.
     */
    protected void processEventDefault(ViewEvent anEvent)
    {
        switch (anEvent.getType()) {
            case MouseMove -> mouseMoved(anEvent);
            case MousePress -> mousePressed(anEvent);
            case MouseDrag -> mouseDragged(anEvent);
            case MouseRelease -> mouseReleased(anEvent);
            case KeyPress -> keyPressed(anEvent);
        }
    }

    /**
     * Handle mouse events.
     */
    protected void processEventSelText(ViewEvent anEvent)
    {
        switch (anEvent.getType()) {
            case MousePress -> mousePressedSelText(anEvent);
            case MouseDrag -> mouseDraggedSelText(anEvent);
        }
    }

    /**
     * Handle mouse events.
     */
    protected void processEventSelImage(ViewEvent anEvent)
    {
        switch (anEvent.getType()) {
            case MousePress -> mousePressedSelImage(anEvent);
            case MouseMove -> mouseMovedSelImage(anEvent);
            case MouseDrag -> mouseDraggedSelImage(anEvent);
        }
    }

    /**
     * Handle paint.
     */
    public void paint(Painter aPntr)
    {
        switch (_mode) {
            case SELECT_TEXT -> paintSelText(aPntr);
            case SELECT_IMAGE -> paintSelImage(aPntr);
        }
    }

    /**
     * Handle copy.
     */
    public void copy()
    {
        switch (_mode) {
            case SELECT_TEXT -> copySelText();
            case SELECT_IMAGE -> copySelImage();
        }
    }

    /**
     * Handle mouse pressed event.
     */
    public void mousePressed(ViewEvent anEvent)
    {
        // Get deepest shape hit by that point that has a URL
        _shapePressed = getViewer().getShapeAtPoint(anEvent.getX(), anEvent.getY(), true);
        while (_shapePressed != null && !_shapePressed.acceptsMouse())
            _shapePressed = _shapePressed.getParent();

        // If shape has URL, open it
        if (_shapePressed != null)
            _shapePressed.processEvent(_viewer.copyEventForShapeAndType(anEvent, _shapePressed, null));
    }

    /**
     * Handle mouse dragged event.
     */
    public void mouseDragged(ViewEvent anEvent)
    {
        // Get shape under drag point
        RMShape shape = getViewer().getShapeAtPoint(anEvent.getX(), anEvent.getY(), true);
        while (shape != null && !shape.acceptsMouse())
            shape = shape.getParent();

        // If shape under move point is different than that of last last move point, update shape under stack
        RMShape lastShapeUnder = _shapeUnderStack.isEmpty() ? null : _shapeUnderStack.peek();
        if (shape != lastShapeUnder)
            updateShapeUnderStack(shape, anEvent);

        // Send mouse dragged to pressed shape
        if (_shapePressed != null)
            _shapePressed.processEvent(_viewer.copyEventForShapeAndType(anEvent, _shapePressed, null));
    }

    /**
     * Handle mouse released event.
     */
    public void mouseReleased(ViewEvent anEvent)
    {
        if (_shapePressed != null)
            _shapePressed.processEvent(_viewer.copyEventForShapeAndType(anEvent, _shapePressed, null));
    }

    /**
     * Handle key pressed.
     */
    public void keyPressed(ViewEvent anEvent)  { }

    /**
     * Handle mouse moved event.
     */
    public void mouseMoved(ViewEvent anEvent)
    {
        // Get shape under move point
        RMShape shape = getViewer().getShapeAtPoint(anEvent.getX(), anEvent.getY(), true);
        while (shape != null && !shape.acceptsMouse())
            shape = shape.getParent();

        // If shape under move point is identical to shape under last move point, call its mouseMoved
        if (!_shapeUnderStack.isEmpty() && _shapeUnderStack.peek() == shape)
            shape.processEvent(_viewer.copyEventForShapeAndType(anEvent, shape, null));

            // If shape under move point is different from last shape under, update it
        else updateShapeUnderStack(shape, anEvent);
    }

    /**
     * The shape under stack should always be a stack of descendants that acceptEvents.
     */
    protected void updateShapeUnderStack(RMShape aShape, ViewEvent anEvent)
    {
        // Get first ancestor that acceptsEvents
        RMShape parent = aShape == null ? null : aShape.getParent();
        while (parent != null && !parent.acceptsMouse())
            parent = parent.getParent();

        // If a parent acceptEvents, then empty _shapeUnderStack so it only contains parent
        if (parent != null && !ListUtils.containsId(_shapeUnderStack, parent))
            updateShapeUnderStack(parent, anEvent);

        // Empty _shapeUnderStack so it only contains aShape
        while (!_shapeUnderStack.isEmpty() && _shapeUnderStack.peek() != parent && _shapeUnderStack.peek() != aShape) {

            // Pop top shape and send mouse exited
            RMShape shape = _shapeUnderStack.pop();

            // Pop top cursor
            _shapeUnderCursorStack.pop();

            // Send mouse exited
            shape.processEvent(_viewer.copyEventForShapeAndType(anEvent, shape, EventType.MouseEnter));

            // Reset cursor
            getViewer().setCursor(_shapeUnderCursorStack.isEmpty() ? Cursor.DEFAULT : _shapeUnderCursorStack.peek());
        }

        // If aShape is no longer child of parent, just return (could happen if mouse over parent changes children)
        if (parent != null && !parent.isDescendant(aShape))
            return;

        // Add aShape if non-null
        if (aShape != null && (_shapeUnderStack.isEmpty() || _shapeUnderStack.peek() != aShape)) {
            aShape.processEvent(_viewer.copyEventForShapeAndType(anEvent, aShape, EventType.MouseEnter));
            _shapeUnderStack.push(aShape);
            _shapeUnderCursorStack.push(getViewer().getCursor());
        }
    }

    /*
     * SELECT_TEXT code.
     */

    /**
     * Handle mouse pressed event.
     */
    private void mousePressedSelText(ViewEvent anEvent)
    {
        _downPoint = new Point(anEvent.getX(), anEvent.getY());              // Get down point
        getViewer().repaint(_paintArea.getBounds());  // Repaint paint area
        _paintArea = new Rect();               // Reset area
    }

    /**
     * Handle mouse dragged event.
     */
    private void mouseDraggedSelText(ViewEvent anEvent)
    {
        // Get drag point
        _dragPoint = new Point(anEvent.getX(), anEvent.getY());

        // Repaint paint area
        RMViewer viewer = getViewer();
        viewer.repaint(_paintArea.getBounds());

        // Get rectangle for down point and current event point - in SelPage coords
        double x = Math.min(_downPoint.getX(), anEvent.getX());
        double y = Math.min(_downPoint.getY(), anEvent.getY());
        double w = Math.max(_downPoint.getX(), anEvent.getX()) - x;
        double h = Math.max(_downPoint.getY(), anEvent.getY()) - y;
        Rect rect = viewer.convertToShape(new Rect(x, y, w, h), viewer.getSelPage()).getBounds();

        // Get path for rect and find/set text shapes
        findTextShapes(viewer.getSelPage(), rect, _selectedTexts = new ArrayList<>());

        // Get selection paint area and repaint
        _paintArea = getTextSelectionArea();
        viewer.repaint(_paintArea.getBounds());
    }

    /**
     * Handle SELECT_TEXT paint.
     */
    private void paintSelText(Painter aPntr)
    {
        aPntr.setOpacity(.33);
        aPntr.setColor(Color.BLUE);
        aPntr.fill(_paintArea);
        aPntr.setOpacity(1);
    }

    /**
     * Handle SELECT_TEXT copy.
     */
    private void copySelText()
    {
        // Get first selected text (just return if none)
        RMTextShape selText = !_selectedTexts.isEmpty() ? _selectedTexts.get(0) : null;
        if (selText == null) return;
        RMDocument sdoc = selText.getDocument();

        // Create new document and add clone of SelectedTexts to new document
        RMDocument doc = new RMDocument(sdoc.getPageSize().width, sdoc.getPageSize().height);
        for (RMTextShape text : _selectedTexts) {
            RMTextShape clone = text.clone();
            doc.getPage(0).addChild(clone);
        }

        // Add doc RTF and CSV to clipboard
        Clipboard cb = Clipboard.getCleared();
        cb.addData(doc.getBytesCSV());
        cb.addDataForMimeType(doc.getBytesRTF(), "text/rtf");
    }

    /**
     * Finds the text shape children of the given shape in the given rect. Recurses into child shapes.
     */
    private void findTextShapes(RMParentShape aParent, Shape aPath, List<RMTextShape> aList)
    {
        // Get list of hit shapes
        List<RMShape> shapes = aParent.getChildrenIntersecting(aPath);

        // Iterate over shapes
        for (RMShape shape : shapes) {

            // If shape is text, just add it
            if (shape instanceof RMTextShape textShape)
                aList.add(textShape);

                // Otherwise if shape has children, recurse (with path converted to shape coords)
            else if (shape instanceof RMParentShape parent) {
                Shape path = parent.parentToLocal(aPath);
                findTextShapes(parent, path, aList);
            }
        }
    }

    /**
     * Returns the text selection shape.
     */
    private Shape getTextSelectionArea()
    {
        TextModelX textModel = new TextModelX(true);
        Shape textAreaShape = new Rect();

        // Iterate over texts and create composite shape
        for (RMTextShape text : _selectedTexts) {

            // Convert points to text
            Point p1 = getViewer().convertToShape(_downPoint.x, _downPoint.y, text);
            Point p2 = getViewer().convertToShape(_dragPoint.x, _dragPoint.y, text);

            // Configure text editor for text
            textModel.setSourceText(text.getRichText());
            textModel.setBounds(0, 0, text.getWidth(), text.getHeight());

            // Get text selection for point, path for selection (int viewer coords) and add
            TextSel sel = new TextSel(textModel, p1.getX(), p1.getY(), p2.getX(), p2.getY(), false, false);
            Shape path = sel.getPath();
            path = getViewer().convertFromShape(path, text);
            textAreaShape = Shape.addShapes(textAreaShape, path);
        }

        // Return
        return textAreaShape;
    }

    /*
     * SELECT_IMAGE code.
     */

    /**
     * Handle mouse pressed event.
     */
    public void mousePressedSelImage(ViewEvent anEvent)
    {
        // Get down point
        _downPoint = new Point(anEvent.getX(), anEvent.getY());

        // If rect isn't empty, repaint
        if (!_rect.isEmpty()) getViewer().repaint();

        // Get rect in viewer coords
        Rect rect = getViewer().convertFromShape(_rect, getViewer().getSelPage()).getBounds();

        // Reset selected sides to edges hit by point
        Point point = new Point(anEvent.getX(), anEvent.getY());
        _selectedSides = rect.isEmpty() ? 0 : getHitEdges(rect, point, 5);

        // Set drag rect
        _dragPoint = _selectedSides > 0 || !rect.contains(anEvent.getX(), anEvent.getY()) ? null :
                new Point(anEvent.getX(), anEvent.getY());

        // If no selected sides, reset rect
        if (_selectedSides == 0 && _dragPoint == null)
            _rect = new Rect();
    }

    /**
     * Handle mouse dragged event.
     */
    public void mouseDraggedSelImage(ViewEvent anEvent)
    {
        // Get rect in viewer coords
        Rect rect = getViewer().convertFromShape(_rect, getViewer().getSelPage()).getBounds();

        // Repaint rect
        getViewer().repaint(rect.isEmpty() ? getViewer().getBounds() : rect.getInsetRect(-5));

        // If there are selected sides, move them
        if (_selectedSides > 0)
            setHitEdges(rect, new Point(anEvent.getX(), anEvent.getY()), _selectedSides);

            // Otherwise, if point is in rect, move rect
        else if (_dragPoint != null) {
            rect.offset(anEvent.getX() - _dragPoint.getX(), anEvent.getY() - _dragPoint.getY());
            _dragPoint = new Point(anEvent.getX(), anEvent.getY());
        }

        // Otherwise, reset rect from down point and event event point
        else {
            double x = Math.min(_downPoint.getX(), anEvent.getX());
            double y = Math.min(_downPoint.getY(), anEvent.getY());
            double w = Math.max(_downPoint.getX(), anEvent.getX()) - x;
            double h = Math.max(_downPoint.getY(), anEvent.getY()) - y;
            rect = new Rect(x, y, w, h);
        }

        // Set rect ivar in viewer coords
        _rect = getViewer().convertToShape(rect, getViewer().getSelPage()).getBounds();

        // Repaint rect
        getViewer().repaint(rect.isEmpty() ? getViewer().getBounds() : rect.getInsetRect(-5));
    }

    /**
     * Handle mouse moved event.
     */
    public void mouseMovedSelImage(ViewEvent anEvent)
    {
        // Get point in selected page coords
        Point point = getViewer().convertToShape(anEvent.getX(), anEvent.getY(), getViewer().getSelPage());

        // Get hit edges
        int hitEdges = _rect.isEmpty() ? 0 : getHitEdges(_rect, point, 5);

        // If selected edge, set cursor
        if (hitEdges != 0)
            getViewer().setCursor(getResizeCursor(hitEdges));

            // If point in rect, set move cursor
        else if (_rect.contains(point.getX(), point.getY()))
            getViewer().setCursor(Cursor.MOVE);

            // Otherwise, reset cursor
        else getViewer().setCursor(Cursor.DEFAULT);
    }

    /**
     * Handle paint.
     */
    public void paintSelImage(Painter aPntr)
    {
        // If selection rect is empty, just return
        if (_rect.isEmpty()) return;

        // Get selection rect in viewer coords
        Rect rect = getViewer().convertFromShape(_rect, getViewer().getSelPage()).getBounds();

        // Create area for bounds, subtract rect and fill
        Shape shape = Shape.subtractShapes(getViewer().getBounds(), rect);
        aPntr.setColor(new Color(0, 0, 0, .667f));
        aPntr.fill(shape);

        // Paint corners
        drawCircle(aPntr, rect.getX(), rect.getY());
        drawCircle(aPntr, rect.getMidX(), rect.getY());
        drawCircle(aPntr, rect.getMaxX(), rect.getY());
        drawCircle(aPntr, rect.getX(), rect.getMidY());
        drawCircle(aPntr, rect.getMaxX(), rect.getMidY());
        drawCircle(aPntr, rect.getX(), rect.getMaxY());
        drawCircle(aPntr, rect.getMidX(), rect.getMaxY());
        drawCircle(aPntr, rect.getMaxX(), rect.getMaxY());
    }

    /**
     * Draws a circle.
     */
    private void drawCircle(Painter aPntr, double aX, double aY)
    {
        aPntr.setColor(Color.WHITE);
        aPntr.fill(new Ellipse(aX - 4, aY - 4, 8, 8));
        aPntr.setColor(Color.GRAY);
        aPntr.fill(new Ellipse(aX - 3, aY - 3, 6, 6));
    }

    /**
     * Handle SELECT_IMAGE copy.
     */
    private void copySelImage()
    {
        // Get an image of the current page and sub-image
        RMShape page = getViewer().getDoc().getSelPage();
        Image pageImage = RMShapeUtils.createImage(page, Color.WHITE);
        Image selImage = pageImage.copyForCropRect(_rect.x, _rect.y, _rect.width, _rect.height);

        // Get transferable and add to clipboard
        Clipboard.get().addData(selImage);
    }

    /**
     * Returns a resize cursor for a rect edge mask.
     */
    private static Cursor getResizeCursor(int anEdgeMask)
    {
        // Handle W_RESIZE_CURSOR, E_RESIZE_CURSOR, N_RESIZE_CURSOR, S_RESIZE_CURSOR
        if (anEdgeMask == MinXEdge) return Cursor.W_RESIZE;
        if (anEdgeMask == MaxXEdge) return Cursor.E_RESIZE;
        if (anEdgeMask == MinYEdge) return Cursor.N_RESIZE;
        if (anEdgeMask == MaxYEdge) return Cursor.S_RESIZE;

        // Handle NW_RESIZE_CURSOR, NE_RESIZE_CURSOR, SW_RESIZE_CURSOR, SE_RESIZE_CURSOR
        if (anEdgeMask == (MinXEdge | MinYEdge)) return Cursor.NW_RESIZE;
        if (anEdgeMask == (MaxXEdge | MinYEdge)) return Cursor.NE_RESIZE;
        if (anEdgeMask == (MinXEdge | MaxYEdge)) return Cursor.SW_RESIZE;
        if (anEdgeMask == (MaxXEdge | MaxYEdge)) return Cursor.SE_RESIZE;
        return null; // Return null since not found
    }

    /**
     * Returns the mask of edges hit by the given point.
     */
    public static int getHitEdges(Rect aRect, Point aPoint, double aRadius)
    {
        // Check MinXEdge, MaxXEdge, MinYEdge, MaxYEdge
        int hitEdges = 0;
        if (Math.abs(aPoint.getX() - aRect.getX()) < aRadius) hitEdges |= MinXEdge;
        else if (Math.abs(aPoint.getX() - aRect.getMaxX()) < aRadius) hitEdges |= MaxXEdge;
        if (Math.abs(aPoint.getY() - aRect.getY()) < aRadius) hitEdges |= MinYEdge;
        else if (Math.abs(aPoint.getY() - aRect.getMaxY()) < aRadius) hitEdges |= MaxYEdge;
        return hitEdges;
    }

    /**
     * Resets the edges of a rect, given a mask of edges and a new point.
     */
    public static void setHitEdges(Rect aRect, Point aPoint, int anEdgeMask)
    {
        // Handle MinXEdge drag
        if ((anEdgeMask & MinXEdge) > 0) {
            double newX = Math.min(aPoint.getX(), aRect.getMaxX() - 1);
            aRect.setWidth(aRect.getMaxX() - newX);
            aRect.setX(newX);
        }

        // Handle MaxXEdge drag
        else if ((anEdgeMask & MaxXEdge) > 0)
            aRect.setWidth(Math.max(1, aPoint.getX() - aRect.getX()));

        // Handle MinYEdge drag
        if ((anEdgeMask & MinYEdge) > 0) {
            double newY = Math.min(aPoint.getY(), aRect.getMaxY() - 1);
            aRect.setHeight(aRect.getMaxY() - newY);
            aRect.setY(newY);
        }

        // Handle MaxYEdge drag
        else if ((anEdgeMask & MaxYEdge) > 0)
            aRect.setHeight(Math.max(1, aPoint.getY() - aRect.getY()));
    }
}