/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.*;
import com.reportmill.shape.*;
import com.reportmill.graphics.*;
import java.util.List;
import snap.geom.*;
import snap.gfx.*;
import snap.props.PropChange;
import snap.text.TextModel;
import snap.text.TextLine;
import snap.text.TextRun;
import snap.view.*;

/**
 * This class provides UI editing for text shapes.
 */
public class RMTextTool<T extends RMTextShape> extends RMTool<T> {

    // The TextView
    private TextView _textView;

    // The shape hit by text tool on mouse down
    private RMShape _downShape;

    // Whether editor should resize RMText whenever text changes
    private boolean _updatingSize;

    // The minimum height of the RMText when editor text editor is updating size
    private double _updatingMinHeight;

    // Whether current mouse drag should be moving table column
    private boolean _moveTableColumn;

    /**
     * Constructor.
     */
    public RMTextTool()
    {
        super();
    }

    /**
     * Initialize UI panel.
     */
    @Override
    protected void initUI()
    {
        // Get the TextView and register to update selection
        _textView = getView("TextView", TextView.class);
        _textView.setRichText(true);
        _textView.addPropChangeListener(pc -> handleTextViewSelectionChange(), TextArea.Selection_Prop);
        _textView.getTextAdapter().addTextModelPropChangeListener(this::handleTextShapeTextModelPropChange);
    }

    /**
     * Refresh UI from currently selected text shape.
     */
    @Override
    protected void resetUI()
    {
        // Get currently selected text
        RMTextShape textShape = getSelectedShape();
        if (textShape == null)
            return;

        // Update AlignLeftButton, AlignCenterButton, AlignRightButton, AlignFullButton
        boolean isJustify = textShape.isJustify();
        HPos alignX = textShape.getAlignX();
        setViewValue("AlignLeftButton", !isJustify && alignX == HPos.LEFT);
        setViewValue("AlignCenterButton", !isJustify && alignX == HPos.CENTER);
        setViewValue("AlignRightButton", !isJustify && alignX == HPos.RIGHT);
        setViewValue("AlignFullButton", isJustify);

        // Update AlignTopButton, AlignMiddleButton, AlignBottomButton
        VPos alignY = textShape.getAlignY();
        setViewValue("AlignTopButton", alignY == VPos.TOP);
        setViewValue("AlignMiddleButton", alignY == VPos.CENTER);
        setViewValue("AlignBottomButton", alignY == VPos.BOTTOM);

        // Set TextView TextModel to text shape text model
        _textView.setTextModel(textShape.getTextModel());

        // Set TextView selection
        resetTextViewSelFromTextEditor();

        // Get text's background color and set in TextView if found
        Color color = null;
        for (RMShape shape = textShape; color == null && shape != null; ) {
            if (shape.getFill() == null) shape = shape.getParent();
            else color = shape.getFill().getColor();
        }
        _textView.setFill(color == null ? Color.WHITE : color);

        // Get xstring font size and scale up to 12pt if any string run is smaller
        double fsize = 12;
        for (TextLine line : textShape.getTextModel().getLines())
            for (TextRun run : line.getRuns())
                fsize = Math.min(fsize, run.getFont().getSize());
        _textView.setFontScale(fsize < 12 ? 12 / fsize : 1);

        // Update MarginText, RoundingThumb, RoundingText
        setViewValue("MarginText", textShape.getMarginString());
        setViewValue("RoundingThumb", textShape.getBorderRadius());
        setViewValue("RoundingText", textShape.getBorderRadius());

        // Update ShowBorderCheckBox, CoalesceNewlinesCheckBox, PerformWrapCheckBox
        setViewValue("ShowBorderCheckBox", textShape.getDrawsSelectionRect());
        setViewValue("CoalesceNewlinesCheckBox", textShape.getCoalesceNewlines());
        setViewValue("PerformWrapCheckBox", textShape.getPerformsWrap());

        // Update PaginateRadio, ShrinkRadio, GrowRadio
        setViewValue("PaginateRadio", textShape.getWraps() == RMTextShape.WRAP_BASIC);
        setViewValue("ShrinkRadio", textShape.getWraps() == RMTextShape.WRAP_SCALE);
        setViewValue("GrowRadio", textShape.getWraps() == RMTextShape.WRAP_NONE);

        // Update CharSpacingSpinner, LineSpacingSpinner, LineGapSpinner
        setViewValue("CharSpacingSpinner", textShape.getCharSpacing());
        setViewValue("LineSpacingSpinner", textShape.getLineSpacingFactor());
        setViewValue("LineGapSpinner", textShape.getLineSpacing());

        // If line height min not set (0), update LineHeightMinSpinner with current font size
        // If valid line height min, update LineHeightMinSpinner with line height
        //double lineHtMin = text.getLineHeightMin();
        //boolean lineHtMinSet = lineHtMin!=0; if(!lineHtMinSet) lineHtMin = RMEditorUtils.getFont(editor).getSize();
        //setViewValue("LineHeightMinSpinner", lineHtMin);
        // If line height max not set, update LineHeightMaxSpinner with current font size
        // If line height max is set, update LineHeightMaxSpinner with line height max
        //double lineHtMax = text.getLineHeightMax();
        //boolean lineHtMaxSet = lineHtMax>999; if(!lineHtMaxSet) lineHtMax = RMEditorUtils.getFont(editor).getSize();
        //setViewValue("LineHeightMaxSpinner", lineHtMax);
    }

    /**
     * Handles changes from UI panel controls.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Get editor, currently selected text shape and text shapes (just return if null)
        RMEditor editor = getEditor();
        RMTextShape textShape = getSelectedShape(); if (textShape == null) return;

        // Register repaint for texts
        List<RMTextShape> textShapes = (List<RMTextShape>) getSelectedShapes();
        textShapes.forEach(RMTextShape::repaint);

        switch (anEvent.getName()) {

            // Handle AlignLeftButton, AlignCenterButton, AlignRightButton, AlignFullButton, AlignTopButton, AlignMiddleButton
            case "AlignLeftButton" -> RMEditorUtils.setAlignmentX(editor, HPos.LEFT);
            case "AlignCenterButton" -> RMEditorUtils.setAlignmentX(editor, HPos.CENTER);
            case "AlignRightButton" -> RMEditorUtils.setAlignmentX(editor, HPos.RIGHT);
            case "AlignFullButton" -> setJustify(editor, true);
            case "AlignTopButton" -> textShapes.forEach(txt -> txt.setAlignY(VPos.TOP));
            case "AlignMiddleButton" -> textShapes.forEach(txt -> txt.setAlignY(VPos.CENTER));
            case "AlignBottomButton" -> textShapes.forEach(txt -> txt.setAlignY(VPos.BOTTOM));

            // Handle MarginText, RoundingThumb, RoundingText
            case "MarginText" -> textShapes.forEach(txt -> txt.setMarginString(anEvent.getStringValue()));
            case "RoundingThumb", "RoundingText" -> {
                textShapes.forEach(t -> t.setStroke(new RMStroke()));
                textShapes.forEach(txt -> txt.setBorderRadius(anEvent.getFloatValue()));
            }

            // Handle ShowBorderCheckBox, CoalesceNewlinesCheckBox, PerformWrapCheckBox
            case "ShowBorderCheckBox" -> textShapes.forEach(txt -> txt.setDrawsSelectionRect(anEvent.getBoolValue()));
            case "CoalesceNewlinesCheckBox" -> textShapes.forEach(txt -> txt.setCoalesceNewlines(anEvent.getBoolValue()));
            case "PerformWrapCheckBox" -> textShapes.forEach(txt -> txt.setPerformsWrap(anEvent.getBoolValue()));

            // Handle PaginateRadio, ShrinkRadio, GrowRadio
            case "PaginateRadio" -> textShapes.forEach(txt -> txt.setWraps(RMTextShape.WRAP_BASIC));
            case "ShrinkRadio" -> textShapes.forEach(txt -> txt.setWraps(RMTextShape.WRAP_SCALE));
            case "GrowRadio" -> textShapes.forEach(txt -> txt.setWraps(RMTextShape.WRAP_NONE));

            // Handle CharSpacingSpinner, LineSpacingSpinner, LineSpacingSingleButton, LineSpacingDoubleButton, LineGapSpinner
            case "CharSpacingSpinner" -> setCharSpacing(editor, anEvent.getFloatValue());
            case "LineSpacingSpinner" -> setLineSpacing(editor, anEvent.getFloatValue());
            case "LineSpacingSingleButton" -> setLineSpacing(editor, 1);
            case "LineSpacingDoubleButton" -> setLineSpacing(editor, 2);
            case "LineGapSpinner" -> setLineGap(editor, anEvent.getFloatValue());

            // Handle LineHeightMinSpinner, LineHeightMaxSpinner
            //if(anEvent.equals("LineHeightMinSpinner")) setLineHeightMin(editor, Math.max(anEvent.getFloatValue(), 0));
            //if(anEvent.equals("LineHeightMaxSpinner")) {
            //    float val = anEvent.getFloatValue(); if(val>=999) val = Float.MAX_VALUE; setLineHeightMax(editor, val); }

            // Handle MakeMinWidthMenuItem, MakeMinHeightMenuItem
            case "MakeMinWidthMenuItem" -> textShapes.forEach(txt -> txt.setWidth(txt.getBestWidth()));
            case "MakeMinHeightMenuItem" -> textShapes.forEach(txt -> txt.setHeight(txt.getBestHeight()));

            // Handle TurnToPathMenuItem
            case "TurnToPathMenuItem" -> {
                for (RMTextShape text1 : textShapes) {
                    RMShape textPathShape = RMTextShapeUtils.getTextPathShape(text1);
                    RMParentShape parent = text1.getParent();
                    parent.addChild(textPathShape, text1.indexOf());
                    parent.removeChild(text1);
                    editor.setSelectedShape(textPathShape);
                }
            }

            // Handle TurnToCharsShapeMenuItem
            case "TurnToCharsShapeMenuItem" -> {
                for (RMTextShape text1 : textShapes) {
                    RMShape textCharsShape = RMTextShapeUtils.getTextCharsShape(text1);
                    RMParentShape parent = text1.getParent();
                    parent.addChild(textCharsShape, text1.indexOf());
                    parent.removeChild(text1);
                    editor.setSelectedShape(textCharsShape);
                }
            }

            // Handle LinkedTextMenuItem
            case "LinkedTextMenuItem" -> {

                // Get linked text identical to original text and add to text's parent
                RMLinkedText linkedText = new RMLinkedText(textShape);
                textShape.getParent().addChild(linkedText);

                // Shift linked text down if there's room, otherwise right, otherwise just offset by quarter inch
                if (textShape.getFrameMaxY() + 18 + textShape.getFrame().height * .75 < textShape.getParent().getHeight())
                    linkedText.offsetXY(0, textShape.getHeight() + 18);
                else if (textShape.getFrameMaxX() + 18 + textShape.getFrame().width * .75 < textShape.getParent().getWidth())
                    linkedText.offsetXY(textShape.getWidth() + 18, 0);
                else linkedText.offsetXY(18, 18);

                // Select and repaint new linked text
                editor.setSelectedShape(linkedText);
                linkedText.repaint();
            }
        }
    }

    /**
     * Resets TextView text selection from text editor.
     */
    private void resetTextViewSelFromTextEditor()
    {
        RMEditor editor = getEditor();
        RMTextEditor textEditor = editor.getTextEditor();
        if (textEditor == null || _textView == null)
            return;

        TextModel textModel = textEditor.getTextModel();
        int textStartCharIndex = textModel.getStartCharIndex();
        int selStartCharIndex = textEditor.getSelStart() + textStartCharIndex;
        int selEndCharIndex = textEditor.getSelEnd() + textStartCharIndex;
        _textView.setSel(selStartCharIndex, selEndCharIndex);
    }

    /**
     * Reset super selected text shape text editor from text view.
     */
    private void resetTextEditorSelFromTextView()
    {
        RMEditor editor = getEditor();
        RMTextEditor textEditor = editor.getTextEditor();
        if (textEditor == null)
            return;

        TextModel textModel = textEditor.getTextModel();
        int textStartCharIndex = textModel.getStartCharIndex();
        int selStartCharIndex = Math.max(_textView.getSelStart() - textStartCharIndex, 0);
        int selEndCharIndex = Math.max(_textView.getSelEnd() - textStartCharIndex, 0);
        textEditor.setSel(selStartCharIndex, selEndCharIndex);
    }

    /**
     * Called when TextView.Selection changes.
     */
    private void handleTextViewSelectionChange()
    {
        // If in resetUI, just return
        if (isSendEventDisabled()) return;

        // Get text, repaint and make sure it's super-selected
        RMEditor editor = getEditor();
        RMTextShape textShape = getSelectedShape();
        if (textShape == null)
            return;
        textShape.repaint();
        if (textShape != editor.getSuperSelectedShape())
            editor.setSuperSelectedShape(textShape);

        // Get TextEditor and update selection from TextView
        resetTextEditorSelFromTextView();

        // ResetUI on MouseUp
        ViewUtils.runOnMouseUp(() -> getEditorPane().resetLater());
    }

    /**
     * Overrides standard tool method to deselect any currently editing text.
     */
    @Override
    public void activateTool()
    {
        if (getEditor().getSuperSelectedShape() instanceof RMTextShape)
            getEditor().setSuperSelectedShape(getEditor().getSuperSelectedShape().getParent());
    }

    /**
     * Event handling - overridden to install text cursor.
     */
    @Override
    public void mouseMoved(ViewEvent anEvent)
    {
        getEditor().setCursor(Cursor.TEXT);
    }

    /**
     * Event handling - overridden to install text cursor.
     */
    @Override
    public void mouseMoved(T aShape, ViewEvent anEvent)
    {
        if (getEditor().getShapeAtPoint(anEvent.getPoint()) instanceof RMTextShape) {
            getEditor().setCursor(Cursor.TEXT);
            anEvent.consume();
        }
    }

    /**
     * Handles mouse pressed for text tool. Special support to super select any text hit by tool mouse pressed.
     */
    @Override
    public void mousePressed(ViewEvent anEvent)
    {
        // Register all selectedShapes dirty because their handles will probably need to be wiped out
        for (RMShape shp : getEditor().getSelectedShapes()) shp.repaint();

        // Get shape hit by down point
        _downShape = getEditor().getShapeAtPoint(anEvent.getX(), anEvent.getY());

        // Get _downPoint from editor
        _downPoint = getEditorEvents().getEventPointInShape(true);

        // Create default text instance and set initial bounds to reasonable value
        RMTextShape tshape = new RMTextShape();
        _shape = tshape;
        Rect defaultBounds = getDefaultBounds(tshape, _downPoint);
        _shape.setFrame(defaultBounds);

        // Add shape to superSelectedShape (within an undo grouping) and superSelect
        getEditor().undoerSetUndoTitle("Add Text");
        getEditor().getSuperSelectedParentShape().addChild(_shape);
        getEditor().setSuperSelectedShape(_shape);
        _updatingSize = true;
    }

    /**
     * Handles mouse dragged for tool. If user doesn't really drag, then default text box should align the base line
     * of the text about the pressed point. If they do really drag, then text box should be the rect they drag out.
     */
    @Override
    public void mouseDragged(ViewEvent anEvent)
    {
        // If shape wasn't created in mouse down, just return
        if (_shape == null) return;

        // Set shape to repaint
        _shape.repaint();

        // Get event point in shape parent coords
        Point point = getEditorEvents().getEventPointInShape(true);
        point = _shape.localToParent(point);

        // Get text default bounds and effective down point
        RMTextShape tshape = (RMTextShape) _shape;
        Rect defaultBounds = getDefaultBounds(tshape, _downPoint);
        Point downPoint = defaultBounds.getPoint(Pos.TOP_LEFT);

        // Get new bounds rect from default bounds and drag point (make sure min height is default height)
        Rect rect = Rect.get(downPoint, point);
        rect.width = Math.max(rect.width, defaultBounds.width);
        rect.height = Math.max(rect.height, defaultBounds.height);

        // Set UpdatingMinHeight to drag rect height, but if text rect unreasonably thin, reset to 0
        _updatingMinHeight = rect.height;
        if (rect.width <= 30)
            _updatingMinHeight = 0;

        // Set new shape bounds
        _shape.setFrame(rect);
    }

    /**
     * Event handling for text tool mouse loop.
     */
    @Override
    public void mouseReleased(ViewEvent e)
    {
        // Get event point in shape parent coords
        Point upPoint = getEditorEvents().getEventPointInShape(true);
        upPoint = _shape.localToParent(upPoint);

        // If upRect is really small, see if the user meant to convert a shape to text instead
        if (Math.abs(_downPoint.x - upPoint.x) <= 3 && Math.abs(_downPoint.y - upPoint.y) <= 3) {

            // If hit shape is text, just super-select that text and return
            if (_downShape instanceof RMTextShape) {
                _shape.removeFromParent();
                getEditor().setSuperSelectedShape(_downShape);
            }

            // If hit shape is Rectangle, Oval or Polygon, swap for RMText and return
            else if (shouldConvertShapeToTextShape(_downShape)) {
                _shape.removeFromParent();
                convertShapeToTextShape(_downShape, null);
            }
        }

        // Set editor current tool to select tool
        getEditor().setCurrentToolToSelectTool();

        // Reset tool shape
        _shape = null;
    }

    /**
     * Event handling for shape editing (just forwards to text editor).
     */
    @Override
    public void handleShapeMouseEvent(T textShape, ViewEvent anEvent)
    {
        // Handle KeyEvent
        if (anEvent.isKeyEvent()) {
            processKeyEvent(textShape, anEvent);
            return;
        }

        // If MoveTableColumn, forward to moveTableColumn()
        if (_moveTableColumn)
            moveTableColumn(anEvent);

        // If text is a structured table row column and point is outside column, start MoveTableRow
        else if (anEvent.isMouseDrag()) {
            Point pnt = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), textShape);
            if (textShape.isStructured() && (pnt.x < -20 || pnt.x > textShape.getWidth() + 10) && textShape.getParent().getChildCount() > 1) {
                textShape.undoerSetUndoTitle("Reorder columns");
                getEditor().setSelectedShape(textShape);
                _moveTableColumn = true;
                return;
            }
        }

        // If shape isn't super selected, just return
        if (!isSuperSelected(textShape))
            return;

        // If mouse event, convert event to text shape coords and consume
        if (anEvent.isMouseEvent()) {
            anEvent.consume();
            Point eventPointInShape = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), textShape);
            anEvent = anEvent.copyForPoint(eventPointInShape.x, eventPointInShape.y);
        }

        // Forward on to editor
        textShape.getTextEditor().handleTextAreaMouseAndKeyEvents(anEvent);
        textShape.repaint();
        resetTextViewSelFromTextEditor();
    }

    /**
     * Key event handling for super selected text.
     */
    @Override
    public void processKeyEvent(T aTextShape, ViewEvent anEvent)
    {
        // If tab was pressed and text is structured table row column, forward selection onto next column
        if (aTextShape.isStructured() && anEvent.isKeyPress() &&
                anEvent.getKeyCode() == KeyCode.TAB && !anEvent.isAltDown()) {

            // Get structured text table row, child table rows and index of child
            RMParentShape tableRow = aTextShape.getParent();
            List<RMShape> children = RMShapeUtils.getShapesSortedByX(tableRow.getChildren());
            int index = children.indexOf(aTextShape);

            // If shift is down, get index to the left, wrapped, otherwise get index to the right, wrapped
            if (anEvent.isShiftDown())
                index = (index - 1 + children.size()) % children.size();
            else index = (index + 1) % children.size();

            // Get next text and super-select
            RMShape nextText = children.get(index);
            getEditor().setSuperSelectedShape(nextText);

            // Consume event and return
            anEvent.consume();
            return;
        }

        // Have text editor process key event
        aTextShape.getTextEditor().handleTextAreaMouseAndKeyEvents(anEvent);
        aTextShape.repaint();
        resetLater();
    }

    /**
     * Move Table Column stuff (table row column re-ordering).
     */
    private void moveTableColumn(ViewEvent anEvent)
    {
        // Get editor, editor SelectedShape and TableRow
        RMEditor editor = getEditor();
        RMShape shape = editor.getSelectedOrSuperSelectedShape();
        RMTableRow tableRow = (RMTableRow) shape.getParent();
        tableRow.repaint();

        // Get event x in TableRow coords and whether point is in TableRow
        Point point = editor.convertToShape(anEvent.getX(), anEvent.getY(), tableRow).withY(2);
        boolean inRow = tableRow.contains(point);

        // Handle MouseDragged: layout children by X (if outside row, skip drag shape)
        if (anEvent.isMouseDrag()) {
            List<RMShape> children = RMShapeUtils.getShapesSortedByFrameX(tableRow.getChildren());
            double x = 0;
            for (RMShape child : children) {
                if (child == shape) {
                    if (inRow) child.setX(point.x - child.getWidth() / 2);
                    else {
                        child.setX(9999);
                        continue;
                    }
                }
                else child.setX(x);
                x += child.getWidth();
            }
        }

        // Handle MouseReleased: reset children
        if (anEvent.isMouseRelease()) {

            // If shape in row, set new index
            if (inRow) {
                int oldIndex = shape.indexOf();
                int newIndex = 0;
                while (newIndex < tableRow.getChildCount() && tableRow.getChild(newIndex).getX() <= shape.getX())
                    newIndex++;
                if (oldIndex != newIndex) {
                    tableRow.removeChild(oldIndex);
                    if (newIndex > oldIndex)
                        newIndex--;
                    tableRow.addChild(shape, newIndex);
                }
            }

            // If shape is outside bounds of tableRow, remove it
            else {
                tableRow.removeChild(shape);
                editor.setSuperSelectedShape(tableRow);
            }

            // Do layout again to snap shape back into place
            tableRow.layoutDeep();
            _moveTableColumn = false;
        }
    }

    /**
     * Override to stop listening to text shape text changes.
     */
    @Override
    public void handleShapeLosingSuperSelected(T textShape)
    {
        // If text editor was really just an insertion point and ending text length is zero, remove text
        if (_updatingSize && textShape.length() == 0 &&
                getEditor().getSelectTool().getDragMode() == RMSelectTool.DragMode.None)
            textShape.removeFromParent();

        // Clear text editor and updating vars
        textShape.clearTextEditor();
        _updatingSize = false;
        _updatingMinHeight = 0;
    }

    /**
     * Handle changes to Selected TextShape.TextModel
     */
    private void handleTextShapeTextModelPropChange(PropChange propChange)
    {
        if (_updatingSize)
            runLater(this::updateTextShapeSizeToFitText);
    }

    /**
     * Called to update text shape size to fit text.
     */
    private void updateTextShapeSizeToFitText()
    {
        // Get preferred text shape width
        RMTextShape textShape = getSelectedShape(); if (textShape == null) return;
        double maxWidth = _updatingMinHeight == 0 ? textShape.getParent().getWidth() - textShape.getX() : textShape.getWidth();
        double prefWidth = textShape.getPrefWidth();
        if (prefWidth > maxWidth)
            prefWidth = maxWidth;

        // If width gets updated, get & set pref width (make sure it doesn't go beyond page border)
        if (_updatingMinHeight == 0)
            textShape.setWidth(prefWidth);

        // If PrefHeight or current height is greater than UpdatingMinHeight, set Height to PrefHeight
        double prefHeight = textShape.getPrefHeight();
        if (prefHeight > _updatingMinHeight || textShape.getHeight() > _updatingMinHeight)
            textShape.setHeight(Math.max(prefHeight, _updatingMinHeight));
    }

    /**
     * Event hook during selection.
     */
    public boolean mousePressedSelection(ViewEvent anEvent)
    {
        List<RMShape> selectedShapes = getEditor().getSelectedOrSuperSelectedShapes();

        // Iterator over selected shapes and see if any has an overflow indicator box that was hit
        for (RMShape shape : selectedShapes) {
            RMTextShape textShape = (RMTextShape) shape;

            // If no linked text and not painting text indicator, just continue
            if (textShape.getLinkedText() == null && !isPaintingTextLinkIndicator(textShape)) continue;

            // Get point in text shape coords
            Point pointInShape = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), textShape);

            // If pressed was in overflow indicator box, add linked text (or select existing one)
            if (pointInShape.x >= textShape.getWidth() - 20 && pointInShape.x <= textShape.getWidth() - 10 &&
                pointInShape.y >= textShape.getHeight() - 5) {
                if (textShape.getLinkedText() == null)
                    fireActionEventForObject("LinkedTextMenuItem", anEvent);
                else getEditor().setSelectedShape(textShape.getLinkedText());
                return true; // Return true so SelectTool goes to DragModeNone
            }
        }

        // Return click not in overflow indicator box
        return false;
    }

    /**
     * Moves the handle at the given index to the given point.
     */
    public void moveShapeHandle(T aShape, int aHandle, Point toPoint)
    {
        // If not structured, do normal version
        if (!aShape.isStructured()) {
            super.moveShapeHandle(aShape, aHandle, toPoint);
            return;
        }

        // Get handle point in shape coords and shape parent coords
        Point handlePoint = getHandlePoint(aShape, aHandle, false);
        Point dragPoint = aShape.parentToLocal(toPoint);

        // Get whether width change (adjust if left handle)
        double deltaW = dragPoint.x - handlePoint.x;
        boolean isLeftHandle = aHandle == HandleW || aHandle == HandleNW || aHandle == HandleSW;
        if (isLeftHandle)
            deltaW = -deltaW;

        // Get new width
        double newW = aShape.getWidth() + deltaW;
        if (newW < 8) {
            newW = 8;
            deltaW = newW - aShape.getWidth();
        }

        // Get shape to adjust and new width (make sure it's no less than 8)
        int index = aShape.indexOf();
        int otherShapeIndex = isLeftHandle ? index - 1 : index + 1;

        // Get other shape
        RMParentShape shapeParent = aShape.getParent();
        RMShape otherShape = shapeParent.getChild(otherShapeIndex);
        double otherShapeNewW = otherShape.getWidth() - deltaW;
        if (otherShapeNewW < 8) {
            otherShapeNewW = 8;
            deltaW = otherShape.getWidth() - otherShapeNewW;
            newW = aShape.getWidth() + deltaW;
        }

        // Set new widths and layout parent
        aShape.setWidth(newW);
        otherShape.setWidth(otherShapeNewW);
        shapeParent.layout();
    }

    /**
     * Overrides tool tooltip method to return text string if some chars aren't visible.
     */
    @Override
    public String getToolTip(T aTextShape, ViewEvent anEvent)
    {
        // If all text is visible and greater than 8 pt, return null
        if (aTextShape.isAllTextVisible() && aTextShape.getFont().getSize() >= 8)
            return null;

        // Get text string (just return if empty), trim to 64 chars or less and return
        String string = aTextShape.getText();
        if (string == null || string.isEmpty())
            return null;
        if (string.length() > 64)
            string = string.substring(0, 64) + "...";
        return string;
    }

    /**
     * Paints selected shape indicator, like handles (and maybe a text linking indicator).
     */
    @Override
    public void paintHandles(T aText, Painter aPntr, boolean isSuperSelected)
    {
        // Paint bounds rect (maybe)
        paintBoundsRect(aText, aPntr);

        // If text is structured, draw rectangle buttons
        if (aText.isStructured()) {

            // Iterate over shape handles, get rect and draw
            aPntr.setAntialiasing(false);
            for (int i = 0, iMax = getHandleCount(aText); i < iMax; i++) {
                Rect hr = getHandleRect(aText, i, isSuperSelected);
                aPntr.drawButton(hr, false);
            }
            aPntr.setAntialiasing(true);
        }

        // If not structured or text linking, draw normal
        else if (!isSuperSelected)
            super.paintHandles(aText, aPntr, isSuperSelected);

        // Call paintTextLinkIndicator
        if (isPaintingTextLinkIndicator(aText))
            paintTextLinkIndicator(aText, aPntr);
    }

    /**
     * Paint bounds rect (maybe): Set color (red if selected, light gray otherwise), get bounds path and draw.
     */
    public void paintBoundsRect(RMTextShape aText, Painter aPntr)
    {
        if (!isShowBoundsRect(aText)) return;
        aPntr.save();
        aPntr.setColor(getEditor().isSuperSelected(aText) ? new Color(.9f, .4f, .4f) : Color.LIGHTGRAY);
        aPntr.setStroke(Stroke.Stroke1.copyForDashes(3, 2));
        Shape path = aText.getBoundsShape().copyForBounds(aText.getBoundsInside());
        path = getEditor().convertFromShape(path, aText);
        aPntr.setAntialiasing(false);
        aPntr.draw(path);
        aPntr.setAntialiasing(true);
        aPntr.restore();
    }

    /**
     * Returns whether to show bounds rect.
     */
    private boolean isShowBoundsRect(RMTextShape aText)
    {
        RMEditor editor = getEditor();
        if (aText.getStroke() != null) return false; // If text draws it's own stroke, return false
        if (!editor.isEditing()) return false; // If editor is previewing, return false
        if (aText.isStructured()) return false; // If structured text, return false
        if (editor.isSelected(aText) || editor.isSuperSelected(aText)) return true; // If selected, return true
        if (aText.length() == 0) return true; // If text is zero length, return true
        if (aText.getDrawsSelectionRect()) return true; // If text explicitly draws selection rect, return true
        return false; // Otherwise, return false
    }

    /**
     * Returns whether to paint text link indicator.
     */
    private static boolean isPaintingTextLinkIndicator(RMTextShape aText)
    {
        // If text is child of table row, return false
        if (aText.getParent() instanceof RMTableRow) return false;

        // If there is a linked text, return true
        if (aText.getLinkedText() != null) return true;

        // If height is less than half-inch, return false
        if (aText.getHeight() < 36) return false;

        // If all text visible, return false
        if (aText.isAllTextVisible()) return false;

        // Return true
        return true;
    }

    /**
     * Paints the text link indicator.
     */
    private void paintTextLinkIndicator(RMTextShape aText, Painter aPntr)
    {
        // Turn off anti-aliasing
        aPntr.setAntialiasing(false);

        // Get overflow indicator box center point in editor coords
        Point point = getEditor().convertFromShape(aText.getWidth() - 15, aText.getHeight(), aText);

        // Get overflow indicator box rect in editor coords
        Rect rect = new Rect(point.x - 5, point.y - 5, 10, 10);

        // Draw white background, black frame, and plus sign and turn off aliasing
        aPntr.setColor(aText.getLinkedText() == null ? Color.WHITE : new Color(90, 200, 255));
        aPntr.fill(rect);
        aPntr.setColor(aText.getLinkedText() == null ? Color.BLACK : Color.GRAY);
        aPntr.setStroke(Stroke.Stroke1);
        aPntr.draw(rect);
        aPntr.setColor(aText.getLinkedText() == null ? Color.BLACK : Color.WHITE);
        aPntr.setStroke(new Stroke(1)); //, BasicStroke.CAP_BUTT, 0));
        aPntr.drawLine(rect.getMidX(), rect.y + 2, rect.getMidX(), rect.getMaxY() - 2);
        aPntr.drawLine(rect.x + 2, rect.getMidY(), rect.getMaxX() - 2, rect.getMidY());

        // Turn on antialiasing
        aPntr.setAntialiasing(true);
    }

    /**
     * Override to handle structured text (in table row).
     */
    @Override
    public int getHandleCount(T aText)  { return aText.isStructured() ? 2 : super.getHandleCount(aText); }

    /**
     * Override to handle structured text (in table row).
     */
    @Override
    public Rect getHandleRect(T textShape, int handle, boolean isSuperSelected)
    {
        if (!textShape.isStructured())
            return super.getHandleRect(textShape, handle, isSuperSelected);

        // Get handle point in text bounds, convert to table row bounds
        Point handlePoint = getHandlePoint(textShape, handle, true);
        handlePoint = textShape.localToParent(handlePoint);

        // If point outside of parent, return bogus rect
        if (handlePoint.x < 0 || handlePoint.x > textShape.getParent().getWidth())
            return new Rect(-9999, -9999, 0, 0);

        // Get handle point in text coords
        handlePoint = getHandlePoint(textShape, handle, false);

        // Get handle point in editor coords
        handlePoint = getEditor().convertFromShape(handlePoint.x, handlePoint.y, textShape);

        // Get handle rect (if super-selected, offset)
        Rect handleRect = new Rect(handlePoint.x - 3, handlePoint.y, 6, textShape.height() * getEditor().getZoomFactor());
        if (isSuperSelected)
            handleRect.offset(handle == 0 ? -2 : 2, 0);

        // Return handle rect
        return handleRect;
    }

    /**
     * Overrides Tool implementation to accept KeysPanel drags.
     */
    @Override
    public boolean acceptsDrag(T aShape, ViewEvent anEvent)
    {
        // If KeysPanel is dragging, return true
        if (KeysPanel.getDragKey() != null)
            return true;
        return super.acceptsDrag(aShape, anEvent);
    }

    /**
     * Override normal implementation to handle KeysPanel drop.
     */
    @Override
    public void handleDragDropEvent(T aShape, ViewEvent anEvent)
    {
        // If a keys panel drop, add key to text
        if (KeysPanel.getDragKey() != null) {
            String string = anEvent.getClipboard().getString();
            RMTextShape text = aShape;
            if (text.length() == 0)
                text.setText(string);
            else text.getTextModel().addChars(" " + string);
        }

        // Otherwise, do normal drop
        else super.handleDragDropEvent(aShape, anEvent);
    }

    /**
     * Returns the shape class that this tool edits.
     */
    @Override
    public Class getShapeClass()  { return RMTextShape.class; }

    /**
     * Returns the name of this tool to be displayed by inspector.
     */
    @Override
    public String getWindowTitle()  { return "Text Inspector"; }

    /**
     * Returns whether text tool should convert to text.
     */
    private static boolean shouldConvertShapeToTextShape(RMShape aShape)
    {
        if (aShape instanceof RMImageShape) return false;
        if (aShape instanceof RMPDFShape) return false;
        if (aShape.isLocked()) return false;
        return aShape instanceof RMRectShape || aShape instanceof RMOvalShape ||
                aShape instanceof RMPolygonShape;
    }

    /**
     * Converts a shape to a text shape.
     */
    public void convertShapeToTextShape(RMShape aShape, String aString)
    {
        // If shape is null, just return
        if (aShape == null) return;

        // Get text shape for given shape (if given shape is text, just use it)
        RMTextShape textShape = aShape instanceof RMTextShape ? (RMTextShape) aShape : new RMTextShape();

        // Copy attributes of given shape
        if (textShape != aShape)
            textShape.copyShape(aShape);

        // Copy path of given shape
        if (textShape != aShape)
            textShape.setPathShape(aShape);

        // Swap this shape in for original
        if (textShape != aShape) {
            aShape.getParent().addChild(textShape, aShape.indexOf());
            aShape.getParent().removeChild(aShape);
        }

        // Install a bogus string for testing
        if (aString != null && aString.equals("test"))
            aString = getTestString();

        // If aString is non-null, install in text
        if (aString != null)
            textShape.setText(aString);

        // Select new shape
        getEditor().setSuperSelectedShape(textShape);
    }

    /**
     * Returns a rect suitable for the default bounds of a given text at a given point. This takes into account the font
     * and margins of the given text.
     */
    private static Rect getDefaultBounds(RMTextShape aText, Point aPoint)
    {
        // Get text font (or default font, if not available) and margin
        Font font = aText.getFont();
        if (font == null)
            font = Font.getDefaultFont();
        Insets margin = aText.getMargin();

        // Default width is a standard char plus margin, default height is font line height plus margin
        double w = Math.round(font.charAdvance('x') + margin.getWidth());
        double h = Math.round(font.getLineHeight() + margin.getHeight());

        // Get bounds x/y from given (cursor) point and size
        double x = Math.round(aPoint.x - w / 2) + 1;
        double y = Math.round(aPoint.y - h / 2) - 1;

        // Return integral bounds rect
        Rect rect = new Rect(x, y, w, h);
        rect.snap();
        return rect;
    }

    /**
     * Returns a test string.
     */
    private static String getTestString()
    {
        return """
            Leo vitae diam est luctus, ornare massa mauris urna, vitae sodales et ut facilisis dignissim, \
            imperdiet in diam, quis que ad ipiscing nec posuere feugiat ante velit. Viva mus leo quisque. Neque mi vitae, \
            nulla cras diam fusce lacus, nibh pellentesque libero. \
            Dolor at venenatis in, ac in quam purus diam mauris massa, dolor leo vehicula at commodo. Turpis condimentum \
            varius aliquet accumsan, sit nullam eget in turpis augue, vel tristique, fusce metus id consequat orci \
            penatibus. Ipsum vehicula euismod aliquet, pharetra. \
            Fusce lectus proin, neque cr as eget, integer quam facilisi a adipiscing posuere. Imper diet sem sapien. \
            Pretium natoque nibh, tristique odio eligendi odio molestie mas sa. Volutpat justo fringilla rut rum augue. \
            Lao reet ulla mcorper molestie.""";
    }

    /**
     * Sets whether selected shape is justified.
     */
    private static void setJustify(RMEditor anEditor, boolean aValue)
    {
        anEditor.undoerSetUndoTitle("Justify change");
        for (RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
            if (shape instanceof RMTextShape textShape)
                textShape.setJustify(aValue);
    }

    /**
     * Sets the character spacing for the currently selected shapes.
     */
    private static void setCharSpacing(RMEditor anEditor, float aValue)
    {
        anEditor.undoerSetUndoTitle("Char Spacing Change");
        for (RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
            if (shape instanceof RMTextShape textShape)
                textShape.setCharSpacing(aValue);
    }

    /**
     * Sets the line spacing for all chars (or all selected chars, if editing).
     */
    private static void setLineSpacing(RMEditor anEditor, float aHeight)
    {
        anEditor.undoerSetUndoTitle("Line Spacing Change");
        for (RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
            if (shape instanceof RMTextShape textShape)
                textShape.setLineSpacingFactor(aHeight);
    }

    /**
     * Sets the line gap for all chars (or all selected chars, if editing).
     */
    private static void setLineGap(RMEditor anEditor, float aHeight)
    {
        anEditor.undoerSetUndoTitle("Line Gap Change");
        for (RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
            if (shape instanceof RMTextShape textShape)
                textShape.setLineSpacing(aHeight);
    }

    /**
     * Sets the minimum line height for all chars (or all selected chars, if editing).
     */
    private static void setLineHeightMin(RMEditor anEditor, float aHeight)
    {
        anEditor.undoerSetUndoTitle("Min Line Height Change");
        for (RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
            if (shape instanceof RMTextShape textShape)
                textShape.setLineMinHeight(aHeight);
    }

    /**
     * Sets the maximum line height for all chars (or all selected chars, if eiditing).
     */
    private static void setLineHeightMax(RMEditor anEditor, float aHeight)
    {
        anEditor.undoerSetUndoTitle("Max Line Height Change");
        for (RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
            if (shape instanceof RMTextShape textShape)
                textShape.setLineMaxHeight(aHeight);
    }
}