/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import com.reportmill.base.RMFormat;
import com.reportmill.shape.RMArchiver;
import snap.geom.Rect;
import snap.geom.Shape;
import snap.gfx.*;
import snap.text.*;
import snap.util.*;
import snap.view.*;

/**
 * This class provides all of the event and drawing code necessary to edit text in the form of an RMXString
 * (separated from an actual UI Component).
 */
public class RMTextEditor {

    // The TextDoc
    private TextBlock  _textDoc;

    // The text box
    private TextBox  _textBox;

    // The XString being edited
    private RMXString  _xstr;

    // The text selection
    private TextSel  _sel;

    // The current text style for the cursor or selection
    private RMTextStyle  _selStyle;

    // Whether the editor is word selecting (double click), paragraph selecting (triple click)
    private boolean  _wordSel, _pgraphSel;

    // The mouse down point
    private double  _downX, _downY;

    // Whether RM should be spell checking
    public static boolean isSpellChecking = Prefs.getDefaultPrefs().getBoolean("SpellChecking", false);

    // Whether hyphenating is activated
    static boolean _hyphenating = Prefs.getDefaultPrefs().getBoolean("Hyphenating", false);

    // The MIME type for reportmill xstring
    public static final String RM_XSTRING_TYPE = "reportmill/xstring";

    /**
     * Constructor.
     */
    public RMTextEditor()
    {
        super();
    }

    /**
     * Returns the text box used to layout text.
     */
    public TextBox getTextBox()
    {
        if (_textBox != null) return _textBox;
        return _textBox = new TextBox(true);
    }

    /**
     * Returns the text box used to layout text.
     */
    public void setTextBox(TextBox aTextBox)
    {
        _textBox = aTextBox;
    }

    /**
     * Sets the xstring that is to be edited.
     */
    public void setXString(RMXString aString)
    {
        if (aString == _xstr) return;
        _xstr = aString;
        _textDoc = _xstr.getRichText();
        setSel(0);
    }

    /**
     * Returns the text editor bounds.
     */
    public Rect getBounds()
    {
        return _textBox.getBounds();
    }

    /**
     * Sets the text editor bounds.
     */
    public void setBounds(double aX, double aY, double aW, double aH)
    {
        _textBox.setBounds(aX, aY, aW, aH);
    }

    /**
     * Returns the number of characters in the text string.
     */
    public int length()  { return _textDoc.length(); }

    /**
     * Returns whether editor is doing check-as-you-type spelling.
     */
    public boolean isSpellChecking()  { return isSpellChecking; }

    /**
     * Returns whether the selection is empty.
     */
    public boolean isSelEmpty()
    {
        return getSelStart() == getSelEnd();
    }

    /**
     * Returns the text editor selection.
     */
    public TextSel getSel()  { return _sel; }

    /**
     * Sets the character index of the text cursor.
     */
    public void setSel(int newStartEnd)
    {
        setSel(newStartEnd, newStartEnd);
    }

    /**
     * Sets the character index of the start and end of the text selection.
     */
    public void setSel(int aStart, int anEnd)
    {
        _sel = new TextSel(getTextBox(), aStart, anEnd);
        _selStyle = null;
        TextBlockUtils.setMouseY(getTextBox(), 0);
    }

    /**
     * Returns the character index of the start of the text selection.
     */
    public int getSelStart()  { return _sel.getStart(); }

    /**
     * Returns the character index of the end of the text selection.
     */
    public int getSelEnd()  { return _sel.getEnd(); }

    /**
     * Returns the character index of the last explicitly selected char (confined to the bounds of the selection).
     */
    public int getSelIndex()  { return _sel.getAnchor(); }

    /**
     * Selects all the characters in the text editor.
     */
    public void selectAll()
    {
        setSel(0, length());
    }

    /**
     * Returns the selected range that would result from the given two points.
     */
    public TextSel getSel(double p1x, double p1y, double p2x, double p2y)
    {
        return new TextSel(getTextBox(), p1x, p1y, p2x, p2y, _wordSel, _pgraphSel);
    }

    /**
     * Returns the text style applied to any input characters.
     */
    public RMTextStyle getSelStyle()
    {
        // If already set, just return
        if (_selStyle != null) return _selStyle;

        // Get style for selection
        int selStart = getSelStart();
        int selEnd = getSelEnd();
        RMTextStyle textStyle = _xstr.getStyleForCharRange(selStart, selEnd);

        // Set/return
        return _selStyle = textStyle;
    }

    /**
     * Sets the attributes that are applied to current selection or newly typed chars.
     */
    public void setInputAttribute(String aKey, Object aValue)
    {
        // If selection is zero length, just modify SelStyle
        if (isSelEmpty())
            _selStyle = getSelStyle().copyFor(aKey, aValue);

            // If selection is multiple chars, apply attribute to xstring, reset SelStyle and flush undo changes
        else {
            _xstr.setAttribute(aKey, aValue, getSelStart(), getSelEnd());
            _selStyle = null;
        }
    }

    /**
     * Returns the paragraph of the current selection or cursor position.
     */
    public RMParagraph getInputParagraph()
    {
        return _xstr.getParagraphAt(getSelStart());
    }

    /**
     * Sets the paragraph of the current selection or cursor position.
     */
    public void setInputParagraph(RMParagraph ps)
    {
        _xstr.setParagraph(ps, getSelStart(), getSelEnd());
    }

    /**
     * Returns the plain string of the xstring being edited.
     */
    public String getString()
    {
        return _textDoc.getString();
    }

    /**
     * Returns the color of the current selection or cursor.
     */
    public RMColor getColor()
    {
        return getSelStyle().getColor();
    }

    /**
     * Sets the color of the current selection or cursor.
     */
    public void setColor(RMColor color)
    {
        setInputAttribute(RMTextStyle.COLOR_KEY, color);
    }

    /**
     * Returns the font of the current selection or cursor.
     */
    public RMFont getFont()
    {
        return getSelStyle().getFont();
    }

    /**
     * Sets the font of the current selection or cursor.
     */
    public void setFont(RMFont font)
    {
        setInputAttribute(RMTextStyle.FONT_KEY, font);
    }

    /**
     * Returns the format of the current selection or cursor.
     */
    public RMFormat getFormat()
    {
        return getSelStyle().getFormat();
    }

    /**
     * Sets the format of the current selection or cursor, after trying to expand the selection to encompass currently
     * selected, @-sign delineated key.
     */
    public void setFormat(RMFormat aFormat)
    {
        // Get format selection range and select it (if non-null)
        TextSel sel = smartFindFormatRange();
        if (sel != null)
            setSel(sel.getStart(), sel.getEnd());

        // Return if we are at end of string (this should never happen)
        if (getSelStart() >= length())
            return;

        // If there is a format, add it to current attributes and set for selected text
        setInputAttribute(RMTextStyle.FORMAT_KEY, aFormat);
    }

    /**
     * Returns whether current selection is underlined.
     */
    public boolean isUnderlined()
    {
        return getSelStyle().isUnderlined();
    }

    /**
     * Sets whether current selection is underlined.
     */
    public void setUnderlined(boolean aFlag)
    {
        setInputAttribute(RMTextStyle.UNDERLINE_KEY, aFlag ? 1 : null);
    }

    /**
     * Returns whether current selection is outlined.
     */
    public Border getTextBorder()
    {
        return getSelStyle().getBorder();
    }

    /**
     * Sets whether current selection is outlined.
     */
    public void setTextBorder(Border aBorder)
    {
        setInputAttribute(RMTextStyle.BORDER_KEY, aBorder);
    }

    /**
     * Sets current selection to superscript.
     */
    public void setSuperscript()
    {
        int state = getSelStyle().getScripting();
        setInputAttribute(RMTextStyle.SCRIPTING_KEY, state == 0 ? 1 : 0);
    }

    /**
     * Sets current selection to subscript.
     */
    public void setSubscript()
    {
        int state = getSelStyle().getScripting();
        setInputAttribute(RMTextStyle.SCRIPTING_KEY, state == 0 ? -1 : 0);
    }

    /**
     * Returns the character spacing of the current selection or cursor.
     */
    public float getCharSpacing()
    {
        return (float) getSelStyle().getCharSpacing();
    }

    /**
     * Returns the character spacing of the current selection or cursor.
     */
    public void setCharSpacing(float aValue)
    {
        setInputAttribute(RMTextStyle.CHAR_SPACING_KEY, aValue);
    }

    /**
     * Returns the alignment for current selection.
     */
    public RMTypes.AlignX getAlignX()
    {
        return getInputParagraph().getAlignmentX();
    }

    /**
     * Sets the alignment for current selection.
     */
    public void setAlignX(RMTypes.AlignX anAlignX)
    {
        RMParagraph pg = getInputParagraph().deriveAligned(anAlignX);
        setInputParagraph(pg);
    }

    /**
     * Returns the line spacing for current selection.
     */
    public float getLineSpacing()
    {
        return getInputParagraph().getLineSpacing();
    }

    /**
     * Sets the line spacing for current selection.
     */
    public void setLineSpacing(float aHeight)
    {
        RMParagraph pg = getInputParagraph().deriveLineSpacing(aHeight);
        setInputParagraph(pg);
    }

    /**
     * Returns the line gap for current selection.
     */
    public float getLineGap()
    {
        return getInputParagraph().getLineGap();
    }

    /**
     * Sets the line gap for current selection.
     */
    public void setLineGap(float aHeight)
    {
        RMParagraph pg = getInputParagraph().deriveLineGap(aHeight);
        setInputParagraph(pg);
    }

    /**
     * Returns the min line height for current selection.
     */
    public float getLineHeightMin()
    {
        return getInputParagraph().getLineHeightMin();
    }

    /**
     * Sets the min line height for current selection.
     */
    public void setLineHeightMin(float aHeight)
    {
        RMParagraph pg = getInputParagraph().deriveLineHeightMin(aHeight);
        setInputParagraph(pg);
    }

    /**
     * Returns the maximum line height for a line of text (even if font size would dictate higher).
     */
    public float getLineHeightMax()
    {
        return getInputParagraph().getLineHeightMax();
    }

    /**
     * Sets the maximum line height for a line of text (even if font size would dictate higher).
     */
    public void setLineHeightMax(float aHeight)
    {
        RMParagraph pg = getInputParagraph().deriveLineHeightMax(aHeight);
        setInputParagraph(pg);
    }

    /**
     * Deletes the current selection.
     */
    public void delete()
    {
        // Get start/end. If empty selection, set start to previous index (if at newline, make sure it's before any \r\n)
        int start = getSelStart(), end = getSelEnd();
        if (start == end) {
            start--;
            if (start < 0) return;
            if (_textDoc.isAfterLineEnd(start + 1))
                start = _textDoc.lastIndexOfNewline(start + 1);
        }

        // Do delete for range
        delete(start, end, true);
    }

    /**
     * Deletes the given range of chars.
     */
    public void delete(int aStart, int anEnd, boolean doUpdateSel)
    {
        // If empty range, just return
        if (anEnd <= aStart) return;

        // Delete chars from string
        _textDoc.removeChars(aStart, anEnd);

        // If update selection requested, update selection to start of deleted range
        if (doUpdateSel)
            setSel(aStart);
    }

    /**
     * Replaces the current selection with the given string.
     */
    public void replace(String aString)
    {
        replace(aString, null, getSelStart(), getSelEnd(), true);
    }

    /**
     * Replaces the current selection with the given string.
     */
    public void replace(String aString, int aStart, int anEnd, boolean doUpdateSel)
    {
        replace(aString, null, aStart, anEnd, doUpdateSel);
    }

    /**
     * Replaces the current selection with the given string.
     */
    public void replace(String aString, TextStyle aStyle, int aStart, int anEnd, boolean doUpdateSel)
    {
        // Do replace in xstring with given string and given SelStyle
        TextStyle style = aStyle != null ? aStyle : getSelStyle()._style;
        _textDoc.replaceChars(aString, style, aStart, anEnd);

        // Update selection to be at end of new string
        if (doUpdateSel)
            setSel(aStart + aString.length());
    }

    /**
     * Replaces the current selection with the given xstring.
     */
    public void replace(TextBlock aRichText)
    {
        replace(aRichText, getSelStart(), getSelEnd(), true);
    }

    /**
     * Replaces the current selection with the given xstring.
     */
    public void replace(TextBlock aRichText, int aStart, int anEnd, boolean doUpdateSel)
    {
        // Iterate over string runs and do replace for each one individually
        int start = aStart, end = anEnd;
        for (TextLine line : aRichText.getLines()) {
            TextRun[] lineRuns = line.getRuns();
            for (TextRun run : lineRuns) {
                replace(run.getString(), run.getTextStyle(), start, end, false);
                start = end = start + run.length();
            }
        }

        // Update selection to be at end of new string
        if (doUpdateSel)
            setSel(aStart + aRichText.length());
    }

    /**
     * Copies the current selection onto the clip board, then deletes the current selection.
     */
    public void cut()
    {
        copy();
        delete();
    }

    /**
     * Copies the current selection onto the clipboard.
     */
    public void copy()
    {
        // If no selection, just return
        if (isSelEmpty()) return;

        // Get xstring for selected characters and get as XML string and plain string
        RMXString xStr = _xstr.substring(getSelStart(), getSelEnd());
        String xmlStr = new XMLArchiver().toXML(xStr).toString();
        String str = xStr.getText();

        // Add to clipboard as rm-xstring and String (text/plain)
        Clipboard cb = Clipboard.get();
        cb.addData(RM_XSTRING_TYPE, xmlStr);
        cb.addData(str);
    }

    /**
     * Pasts the current clipboard data over the current selection.
     */
    public void paste()
    {
        // If Clipboard has RMXString, paste it
        Clipboard clipboard = Clipboard.get();
        if (clipboard.hasData(RM_XSTRING_TYPE)) {
            byte[] bytes = clipboard.getDataBytes(RM_XSTRING_TYPE);
            RMXString xStr = (RMXString) new RMArchiver().readFromXMLBytes(bytes);
            replace(xStr.getRichText());
        }

        // If Clipboard has String, paste it
        else if (clipboard.hasString()) {
            String str = clipboard.getString();
            if (str != null && str.length() > 0)
                replace(str);
        }
    }

    /**
     * Moves the insertion point forward a character (or if a range is selected, moves to end of range).
     */
    public void keyForward(boolean isShiftDown)
    {
        // If shift is down, extend selection forward
        if (isShiftDown) {
            if (getSelIndex() == getSelStart() && !isSelEmpty()) setSel(getSelStart() + 1, getSelEnd());
            else {
                setSel(getSelStart(), getSelEnd() + 1);
            }
            return;
        }

        // Get new selection index from current end
        int index = getSel().getCharRight();
        setSel(index);
    }

    /**
     * Moves the insertion point backward a character (or if a range is selected, moves to beginning of range).
     */
    public void keyBackward(boolean isShiftDown)
    {
        // If shift is down, extend selection back
        if (isShiftDown) {
            if (getSelIndex() == getSelEnd() && !isSelEmpty()) setSel(getSelStart(), getSelEnd() - 1);
            else {
                setSel(getSelEnd(), getSelStart() - 1);
            }
            return;
        }

        // Get new selection index from current start
        int index = getSel().getCharLeft();
        setSel(index);
    }

    /**
     * Moves the insertion point up a line, trying to preserve distance from beginning of line.
     */
    public void keyUp()
    {
        int index = getSel().getCharUp();
        setSel(index);
    }

    /**
     * Moves the insertion point down a line, trying preserve distance from beginning of line.
     */
    public void keyDown()
    {
        int index = getSel().getCharDown();
        setSel(index);
    }

    /**
     * Moves the insertion point to the beginning of line.
     */
    public void selectLineStart()
    {
        int index = getSel().getLineStart();
        setSel(index);
    }

    /**
     * Moves the insertion point to next newline or text end.
     */
    public void selectLineEnd()
    {
        int index = getSel().getLineEnd();
        setSel(index);
    }

    /**
     * Deletes the character in front of the insertion point.
     */
    public void deleteForward()
    {
        if (isSelEmpty() && getSelEnd() < length()) {
            int end = getSelEnd() + 1;
            if (_textDoc.isLineEnd(end - 1)) end = _textDoc.indexAfterNewline(end - 1);
            delete(getSelStart(), end, true);
        } else if (!isSelEmpty())
            delete();
    }

    /**
     * Deletes the characters from the insertion point to the end of the line.
     */
    public void deleteToLineEnd()
    {
        // If there is a current selection, just delete it
        if (!isSelEmpty())
            delete();

            // Otherwise, if at line end, delete line end
        else if (_textDoc.isLineEnd(getSelEnd()))
            delete(getSelStart(), _textDoc.indexAfterNewline(getSelStart()), true);

            // Otherwise delete up to next newline or line end
        else {
            int index = _textDoc.indexOfNewline(getSelStart());
            delete(getSelStart(), index >= 0 ? index : length(), true);
        }
    }

    /**
     * Returns the height needed to display all characters.
     */
    public double getPrefHeight()
    {
        return getTextBox().getPrefHeight(getTextBox().getWidth());
    }

    /**
     * Handles events.
     */
    public void processEvent(ViewEvent anEvent)
    {
        // Handle KeyEvents
        switch (anEvent.getType()) {
            case KeyPress: keyPressed(anEvent); return;
            case KeyType: keyTyped(anEvent); return;
            case MousePress: mousePressed(anEvent); return;
            case MouseDrag: mouseDragged(anEvent); return;
            case MouseRelease: mouseReleased(anEvent);
        }
    }

    /**
     * Handle keyPressed.
     */
    protected void keyPressed(ViewEvent anEvent)
    {
        // Get event info
        int keyCode = anEvent.getKeyCode();
        boolean isShiftDown = anEvent.isShiftDown();
        boolean isShortcutDown = anEvent.isShortcutDown(), isControlDown = anEvent.isControlDown();

        // Handle command keys
        if (isShortcutDown) {

            // If shift-down, just return
            if (isShiftDown && keyCode != KeyCode.Z)
                return;

            // Handle common command keys
            switch (keyCode) {
                case KeyCode.X: cut(); break; // Handle command-x cut
                case KeyCode.C: copy(); break; // Handle command-c copy
                case KeyCode.V: paste(); break; // Handle command-v paste
                case KeyCode.A: selectAll(); break; // Handle command-a select all
                default: return; // Any other command keys just return
            }
        }

        // Handle control keys (not applicable on Windows, since they are handled by command key code above)
        else if (isControlDown) {

            // If shift down, just return
            if (isShiftDown) return;

            // Handle common emacs key bindings
            switch (keyCode) {
                case KeyCode.F: keyForward(false); break; // Handle control-f key forward
                case KeyCode.B: keyBackward(false); break; // Handle control-b key backward
                case KeyCode.P: keyUp(); break; // Handle control-p key up
                case KeyCode.N: keyDown(); break; // Handle control-n key down
                case KeyCode.A: selectLineStart(); break; // Handle control-a line start
                case KeyCode.E: selectLineEnd(); break; // Handle control-e line end
                case KeyCode.D: deleteForward(); break; // Handle control-d delete forward
                case KeyCode.K: deleteToLineEnd(); break; // Handle control-k delete line to end
                default: return; // Any other control keys, just return
            }
        }

        // Handle supported non-character keys
        else switch (keyCode) {
                case KeyCode.TAB: replace("\t"); break;
                case KeyCode.ENTER: replace("\n"); break;
                case KeyCode.LEFT: keyBackward(isShiftDown); break;
                case KeyCode.RIGHT: keyForward(isShiftDown); break;
                case KeyCode.UP: keyUp(); break;
                case KeyCode.DOWN: keyDown(); break;
                case KeyCode.HOME: selectLineStart(); break;
                case KeyCode.END: selectLineEnd(); break;
                case KeyCode.BACK_SPACE: delete(); break;
                case KeyCode.DELETE: deleteForward(); break;
                case KeyCode.ESCAPE: return; // Suppress consume so editor gets it
                //default: return; // Any other non-character key, just return
            }

        // Consume the event
        anEvent.consume();
    }

    /**
     * Handle keyTyped.
     */
    protected void keyTyped(ViewEvent anEvent)
    {
        char keyChar = anEvent.getKeyChar();
        boolean isCharDefined = keyChar != KeyCode.CHAR_UNDEFINED && !Character.isISOControl(keyChar);
        boolean isShortcutDown = anEvent.isShortcutDown(), isControlDown = anEvent.isControlDown();

        // If KEY_TYPED with defined char and no command/control modifier, call TextEditor.replace(), consume and return
        if (isCharDefined && !isShortcutDown && !isControlDown) {
            replace(Character.toString(keyChar));
            anEvent.consume();
        }
    }

    /**
     * Handles mouse pressed.
     */
    public void mousePressed(ViewEvent anEvent)
    {
        // Store the mouse down point
        _downX = anEvent.getX();
        _downY = anEvent.getY();

        // Determine if word or paragraph selecting
        if (!anEvent.isShiftDown()) _wordSel = _pgraphSel = false;
        if (anEvent.getClickCount() == 2) _wordSel = true;
        else if (anEvent.getClickCount() == 3) _pgraphSel = true;

        // Get selection for down point
        TextSel sel = getSel(_downX, _downY, _downX, _downY);
        int start = sel.getStart(), end = sel.getEnd();

        // If shift is down, xor selection
        if (anEvent.isShiftDown()) {
            if (start <= getSelStart()) end = getSelEnd();
            else start = getSelStart();
        }

        // Set selection
        setSel(start, end);
        TextBlockUtils.setMouseY(getTextBox(), anEvent.getY());
    }

    /**
     * Handles mouse dragged.
     */
    public void mouseDragged(ViewEvent anEvent)
    {
        // Get selection for down point and drag point
        TextSel sel = getSel(_downX, _downY, anEvent.getX(), anEvent.getY());
        int start = sel.getStart(), end = sel.getEnd();

        // If shift is down, xor selection
        if (anEvent.isShiftDown()) {
            if (start <= getSelStart()) end = getSelEnd();
            else start = getSelStart();
        }

        // Set selection
        setSel(start, end);
        TextBlockUtils.setMouseY(getTextBox(), anEvent.getY());
    }

    /**
     * Handles mouse released.
     */
    public void mouseReleased(ViewEvent anEvent)  { }

    /**
     * Paints a given TextEditor.
     */
    public void paintText(Painter aPntr)
    {
        // Get selection path
        Shape path = getSel().getPath();

        // If empty selection, draw caret
        if (isSelEmpty() && path != null) {
            aPntr.setColor(Color.BLACK);
            aPntr.setStroke(Stroke.Stroke1); // Set color and stroke of cursor
            aPntr.setAntialiasing(false);
            aPntr.draw(path);
            aPntr.setAntialiasing(true); // Draw cursor
        }

        // If selection, get selection path and fill
        else {
            aPntr.setColor(new Color(128, 128, 128, 128));
            aPntr.fill(path);
        }

        // If spell checking, get path for misspelled words and draw
        if (isSpellChecking() && length() > 0) {

            // Set RM SpellCheck
            snap.text.SpellCheck.setSharedClass(RMSpellCheck.class);

            // Get spelling path
            Shape spellingPath = snap.text.SpellCheck.getSpellingPath(getTextBox(), getSelStart());

            // Paint spelling path
            aPntr.setColor(Color.RED);
            aPntr.setStroke(Stroke.StrokeDash1);
            aPntr.draw(spellingPath);
            aPntr.setColor(Color.BLACK);
            aPntr.setStroke(Stroke.Stroke1);
        }

        // Paint TextBox
        getTextBox().paint(aPntr);
    }

    /**
     * This method returns the range of the @-sign delinated key closest to the current selection (or null if not found).
     */
    private TextSel smartFindFormatRange()
    {
        int selStart = getSelStart(), selEnd = getSelEnd();
        int prevAtSignIndex = -1, nextAtSignIndex = -1;
        String string = getString();

        // See if selection contains an '@'
        if (selEnd > selStart)
            prevAtSignIndex = string.indexOf("@", selStart);
        if (prevAtSignIndex >= selEnd)
            prevAtSignIndex = -1;

        // If there wasn't an '@' in selection, see if there is one before the selected range
        if (prevAtSignIndex < 0)
            prevAtSignIndex = string.lastIndexOf("@", selStart - 1);

        // If there wasn't an '@' in or before selection, see if there is one after the selected range
        if (prevAtSignIndex < 0)
            prevAtSignIndex = string.indexOf("@", selEnd);

        // If there is a '@' in, before or after selection, see if there is another after it
        if (prevAtSignIndex >= 0)
            nextAtSignIndex = string.indexOf("@", prevAtSignIndex + 1);

        // If there is a '@' in, before or after selection, but not one after it, see if there is one before that
        if (prevAtSignIndex >= 0 && nextAtSignIndex < 0)
            nextAtSignIndex = string.lastIndexOf("@", prevAtSignIndex - 1);

        // If both a previous and next '@', select the chars inbetween
        if (prevAtSignIndex >= 0 && nextAtSignIndex >= 0 && prevAtSignIndex != nextAtSignIndex) {
            int start = Math.min(prevAtSignIndex, nextAtSignIndex);
            int end = Math.max(prevAtSignIndex, nextAtSignIndex);
            return new TextSel(_textBox, start, end + 1);
        }

        // Return null since range not found
        return null;
    }

    /**
     * Returns whether layout tries to hyphenate wrapped words.
     */
    public static boolean isHyphenating()
    {
        return _hyphenating;
    }

    /**
     * Sets whether layout tries to hyphenate wrapped words.
     */
    public static void setHyphenating(boolean aValue)
    {
        Prefs.getDefaultPrefs().setValue("Hyphenating", _hyphenating = aValue);
    }
}