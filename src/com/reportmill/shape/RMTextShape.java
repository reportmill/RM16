/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.graphics.*;
import java.util.*;
import java.util.List;

import snap.geom.*;
import snap.gfx.*;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.text.*;
import snap.util.*;

/**
 * This class is an RMShape subclass for handling rich text. Text is probably the most common and useful element in a
 * ReportMill template. You might use this class to programmatically build or modify a template, like this:
 * <p><blockquote><pre>
 *   RMXString xstring = new RMXString("Hello world!", RMFont.getFont("Arial", 12), RMColor.red);
 *   RMText text = new RMText(xstring);
 *   template.getPage(0).addChild(text);
 *   text.setXY(36, 36);
 *   text.setSizeToFit();
 * </pre></blockquote>
 */
public class RMTextShape extends RMRectShape {

    // A text model to manage text content and style
    private TextModel _textModel;

    // A text layout to manage text in shape bounds
    private TextModelX _textLayout;

    // The text margin (if different than default)
    private Insets _margin = getMarginDefault();

    // Vertical alignment of text
    private VPos _alignY = VPos.TOP;

    // Specifies how text should handle overflow during RPG (ignore it, shrink it or paginate it)
    private byte _wraps;

    // Whether to fit text on layout
    private boolean _fitText;

    // Whether text should wrap around other shapes that cause wrap
    private boolean _performsWrap = false;

    // Whether text should eliminate empty lines during RPG
    private boolean _coalesceNewlines;

    // Whether text should draw box around itself even if there's no stroke
    private boolean _drawsSelectionRect;

    // The linked text shape for rendering overflow, if there is one
    private RMLinkedText _linkedText;

    // The text editor, if one has been set
    private RMTextEditor _textEditor;

    // A listener to handle text model prop changes
    private PropChangeListener _textModelPropChangeLsnr = this::handleTextModelPropChange;

    // The default text margin (top=1, left=2, bottom=0, right=2)
    private static Insets MARGIN_DEFAULT = new Insets(1, 2, 0, 2);

    // Constants for overflow behavior during RPG
    public static final byte WRAP_NONE = 0;
    public static final byte WRAP_BASIC = 1;
    public static final byte WRAP_SCALE = 2;

    /**
     * Constructor.
     */
    public RMTextShape()
    {
        super();
    }

    /**
     * Constructor.
     */
    public RMTextShape(TextModel textModel)
    {
        super();
        setTextModel(textModel);
    }

    /**
     * Constructor.
     */
    public RMTextShape(String plainText)
    {
        super();
        getTextModel().addChars(plainText);
    }

    /**
     * Returns a text model.
     */
    public TextModel getTextModel()
    {
        if (_textModel != null) return _textModel;
        TextModel textModel = TextModel.createDefaultTextModel(true);
        textModel.addPropChangeListener(_textModelPropChangeLsnr);
        return _textModel = textModel;
    }

    /**
     * Sets the text model.
     */
    protected void setTextModel(TextModel textModel)
    {
        if (textModel == _textModel) return;
        if (_textModel != null)
            _textModel.removePropChangeListener(_textModelPropChangeLsnr);
        textModel.addPropChangeListener(_textModelPropChangeLsnr);
        firePropChange("TextModel", _textModel, _textModel = textModel);
        _textLayout = null;
        _textEditor = null;
        revalidate();
        repaint();
    }

    /**
     * Returns the text layout.
     */
    public TextLayout getTextLayout()
    {
        if (_textLayout != null) return _textLayout;

        // Create and set
        TextModel textModel = getTextModel();
        _textLayout = new TextModelX(textModel);
        _textLayout.setWrapLines(true);
        updateTextBox();

        // Return
        return _textLayout;
    }

    /**
     * Updates the text box.
     */
    private void updateTextBox()
    {
        // Get/set text bounds
        Insets pad = getMargin();
        double textW = Math.max(getWidth() - pad.getWidth(), 0);
        double textH = Math.max(getHeight() - pad.getHeight(), 0);
        _textLayout.setBounds(pad.left, pad.top, textW, textH);

        // Set StartCharIndex
        _textLayout.setStartCharIndex(getVisibleStart());
        _textLayout.setLinked(getLinkedText() != null);
        _textLayout.setAlignY(getAlignY());
        _textLayout.setBoundsPath(!(getPath() instanceof Rect) || getPerformsWrap() ? getPath() : null);
        _textLayout.setHyphenate(RMTextEditor.isHyphenating());
        _textLayout.setFontScale(1);

        // Handle FitText: With hack to avoid text wrapping for data columns
        if (_fitText) {
            if (getHeight() < 50 && getWidth() > getHeight() * 3)
                _textLayout.setWrapLines(false);
            _textLayout.scaleTextToFit();
        }
    }

    /**
     * Returns the length, in characters, of the XString associated with this RMText.
     */
    public int length()  { return _textModel!= null ? _textModel.length() : 0; }

    /**
     * Returns the text associated with this RMText as a plain String.
     */
    public String getText()  { return _textModel != null ? _textModel.getString() : ""; }

    /**
     * Replaces the current text associated with this RMText with the given String.
     */
    public void setText(String aString)
    {
        getTextModel().replaceChars(aString, 0, length());
    }

    /**
     * Returns the first character index visible in this text.
     */
    public int getVisibleStart()  { return 0; }

    /**
     * Returns the last character index visible in this text.
     */
    public int getVisibleEnd()  { return getTextLayout().getEndCharIndex(); }

    /**
     * Returns whether all characters can be visibly rendered in text bounds.
     */
    public boolean isAllTextVisible()  { return !getTextLayout().isTextOutOfBounds(); }

    /**
     * Returns the font for char 0.
     */
    public Font getFont()
    {
        if (isTextEditorSet())
            return getTextEditor().getSelFont();
        return getTextModel().getRunForCharIndex(0).getFont();
    }

    /**
     * Sets the font for all characters.
     */
    public void setFont(Font aFont)
    {
        if (isTextEditorSet())
            getTextEditor().setSelFont(aFont);
        else getTextModel().setTextStyleValue(TextStyle.Font_Prop, aFont, 0, length());
    }

    /**
     * Returns the format for char 0.
     */
    public TextFormat getFormat()
    {
        if (isTextEditorSet())
            return getTextEditor().getSelFormat();
        return getTextModel().getRunForCharIndex(0).getFormat();
    }

    /**
     * Sets the format for all characters.
     */
    public void setFormat(TextFormat aFormat)
    {
        if (isTextEditorSet())
            getTextEditor().setSelFormat(aFormat);
        else getTextModel().setTextStyleValue(TextStyle.Format_Prop, aFormat, 0, length());
    }

    /**
     * Returns the color of the first character of the xstring associated with this RMText.
     */
    public Color getTextColor()
    {
        return getTextModel().getRunForCharIndex(0).getColor();
    }

    /**
     * Sets the color of the characters in the XString associated with this RMText.
     */
    public void setTextColor(Color aColor)
    {
         getTextModel().setTextStyleValue(TextStyle.Color_Prop, aColor, 0, length());
    }

    /**
     * Returns if char 0 is underlined.
     */
    public boolean isUnderlined()
    {
        if (isTextEditorSet())
            return getTextEditor().isSelUnderlined();
        return getTextModel().getRunForCharIndex(0).isUnderlined();
    }

    /**
     * Sets all chars to be underlined.
     */
    public void setUnderlined(boolean aFlag)
    {
        if (isTextEditorSet())
            getTextEditor().setSelUnderlined(aFlag);
        else getTextModel().setTextStyleValue(TextStyle.Underline_Prop, aFlag ? 1 : 0, 0, length());
    }

    /**
     * Returns the border for char 0.
     */
    public Border getTextBorder()
    {
        if (isTextEditorSet())
            return getTextEditor().getSelBorder();
        return getTextModel().getRunForCharIndex(0).getBorder();
    }

    /**
     * Sets the border for all characters.
     */
    public void setTextBorder(Border aBorder)
    {
        if (isTextEditorSet())
            getTextEditor().setSelBorder(aBorder);
        else getTextModel().setTextStyleValue(TextStyle.Border_Prop, aBorder, 0, length());
    }

    /**
     * Returns whether text is justified.
     */
    public boolean isJustify()
    {
        if (isTextEditorSet())
            return getTextEditor().getSelLineStyle().isJustify();
        return getTextModel().getLineStyleForCharIndex(0).isJustify();
    }

    /**
     * Sets whether text is justified.
     */
    public void setJustify(boolean aValue)
    {
        if (isTextEditorSet())
            getTextEditor().setSelLineStyle(getTextEditor().getSelLineStyle().copyForPropKeyValue(TextLineStyle.Justify_Prop, aValue));
        else getTextModel().setLineStyleValue(TextLineStyle.Justify_Prop, aValue, 0, length());
    }

    /**
     * Returns the alignment for char 0.
     */
    @Override
    public HPos getAlignX()
    {
        if (isTextEditorSet())
            return getTextEditor().getSelAlignX();
        return getTextModel().getLineStyleForCharIndex(0).getAlign();
    }

    /**
     * Sets the align for all chars.
     */
    @Override
    public void setAlignX(HPos alignX)
    {
        if (isTextEditorSet())
            getTextEditor().setSelAlignX(alignX);
        else getTextModel().setLineStyleValue(TextLineStyle.Align_Prop, alignX, 0, length());
    }

    /**
     * Returns the vertical alignment.
     */
    @Override
    public VPos getAlignY()  { return _alignY; }

    /**
     * Sets the vertical alignment.
     */
    @Override
    public void setAlignY(VPos alignY)
    {
        firePropChange("AlignmentY", _alignY, _alignY = alignY);
        revalidate();
        repaint();
    }

    /**
     * Returns the wrapping behavior for over-filled rpgCloned text (NONE, WRAP, SHRINK).
     */
    public byte getWraps()  { return _wraps; }

    /**
     * Sets the wrapping behavior for over-filled rpgCloned text (NONE, WRAP, SHRINK).
     */
    public void setWraps(byte aValue)  { _wraps = aValue; }

    /**
     * Returns whether text should wrap around other shapes that cause wrap.
     */
    public boolean getPerformsWrap()  { return _performsWrap; }

    /**
     * Sets whether text should wrap around other shapes that cause wrap.
     */
    public void setPerformsWrap(boolean aFlag)  { _performsWrap = aFlag; }

    /**
     * Returns whether text should coalesce consecutive newlines in rpgClone.
     */
    public boolean getCoalesceNewlines()  { return _coalesceNewlines; }

    /**
     * Sets whether text should coalesce consecutive newlines in rpgClone.
     */
    public void setCoalesceNewlines(boolean aFlag)  { _coalesceNewlines = aFlag; }

    /**
     * Returns whether text should always draw at least a light gray border (useful when editing).
     */
    public boolean getDrawsSelectionRect()  { return _drawsSelectionRect; }

    /**
     * Sets whether text should always draw at least a light-gray border (useful when editing).
     */
    public void setDrawsSelectionRect(boolean aValue)  { _drawsSelectionRect = aValue; }

    /**
     * Returns the char spacing at char 0.
     */
    public double getCharSpacing()
    {
        if (isTextEditorSet())
            return getTextEditor().getSelCharSpacing();
        return getTextModel().getRunForCharIndex(0).getCharSpacing();
    }

    /**
     * Sets the char spacing for the text string.
     */
    public void setCharSpacing(double aValue)
    {
        if (isTextEditorSet())
            getTextEditor().setSelCharSpacing(aValue);
        else getTextModel().setTextStyleValue(TextStyle.CharSpacing_Prop, aValue == 0 ? null : aValue, 0, length());
    }

    /**
     * Returns the line spacing.
     */
    public double getLineSpacing()
    {
        if (isTextEditorSet())
            return getTextEditor().getSelLineSpacing();
        return getTextModel().getLineStyleForCharIndex(0).getSpacing();
    }

    /**
     * Sets the line spacing for all chars.
     */
    public void setLineSpacing(double aHeight)
    {
        if (isTextEditorSet())
            getTextEditor().setSelLineSpacing(aHeight);
        else getTextModel().setLineStyleValue(TextLineStyle.Spacing_Prop, aHeight, 0, length());
    }

    /**
     * Returns the line spacing factor.
     */
    public double getLineSpacingFactor()
    {
        if (isTextEditorSet())
            return getTextEditor().getSelLineSpacingFactor();
        return getTextModel().getLineStyleForCharIndex(0).getSpacingFactor();
    }

    /**
     * Sets the line spacing factor for all chars.
     */
    public void setLineSpacingFactor(double aHeight)
    {
        if (isTextEditorSet())
            getTextEditor().setSelLineSpacingFactor(aHeight);
        else getTextModel().setLineStyleValue(TextLineStyle.SpacingFactor_Prop, aHeight, 0, length());
    }

    /**
     * Returns the minimum line height at char 0.
     */
    public double getLineMinHeight()
    {
        if (isTextEditorSet())
            return getTextEditor().getSelLineMinHeight();
        return getTextModel().getLineStyleForCharIndex(0).getMinHeight();
    }

    /**
     * Sets the minimum line height for all chars.
     */
    public void setLineMinHeight(double aHeight)
    {
        if (isTextEditorSet())
            getTextEditor().setSelLineMinHeight(aHeight);
        else getTextModel().setLineStyleValue(TextLineStyle.MinHeight_Prop, aHeight, 0, length());
    }

    /**
     * Returns the maximum line height at char 0.
     */
    public double getLineMaxHeight()
    {
        if (isTextEditorSet())
            return getTextEditor().getSelLineMaxHeight();
        return getTextModel().getLineStyleForCharIndex(0).getMaxHeight();
    }

    /**
     * Sets the maximum line height for all chars.
     */
    public void setLineMaxHeight(double aHeight)
    {
        if (isTextEditorSet())
            getTextEditor().setSelLineMaxHeight(aHeight);
        else getTextModel().setLineStyleValue(TextLineStyle.MaxHeight_Prop, aHeight, 0, length());
    }

    /**
     * Returns margin.
     */
    public Insets getMargin()  { return _margin; }

    /**
     * Sets margin.
     */
    public void setMargin(Insets aMargin)
    {
        if (_margin.equals(aMargin)) return;
        firePropChange("Margin", _margin, _margin = aMargin);
        revalidate();
        repaint();
    }

    /**
     * Returns the default margin of the text (top=1, left=2, right=2, bottom=0).
     */
    public Insets getMarginDefault()
    {
        return MARGIN_DEFAULT;
    }

    /**
     * Returns the margin as a string.
     */
    public String getMarginString()
    {
        return getMarginTop() + ", " + getMarginLeft() + ", " + getMarginBottom() + ", " + getMarginRight();
    }

    /**
     * Sets the margin as a string.
     */
    public void setMarginString(String aString)
    {
        // If given string is empty, set default margins
        if (aString == null || aString.trim().isEmpty()) {
            setMargin(getMarginDefault());
            return;
        }

        // Split the string by commas or spaces and get the parts
        String[] parts = aString.indexOf(",") > 0 ? aString.split(",") : aString.split(" ");
        String p1 = parts[0];
        String p2 = parts[Math.min(1, parts.length - 1)];
        String p3 = parts[Math.min(2, parts.length - 1)];
        String p4 = parts[Math.min(3, parts.length - 1)];

        // Set margin from parts
        setMargin(new Insets(Convert.intValue(p1), Convert.intValue(p4), Convert.intValue(p3), Convert.intValue(p2)));
    }

    /**
     * Returns the left margin of the text (default to 2).
     */
    public int getMarginLeft()  { return (int) Math.round(getMargin().left); }

    /**
     * Returns the right margin of the text (defaults to 2).
     */
    public int getMarginRight()  { return (int) Math.round(getMargin().right); }

    /**
     * Returns the top margin of the text (defaults to 1).
     */
    public int getMarginTop()  { return (int) Math.round(getMargin().top); }

    /**
     * Returns the bottom margin of the text (defaults to 0).
     */
    public int getMarginBottom()  { return (int) Math.round(getMargin().bottom); }

    /**
     * Override to revalidate.
     */
    public void setWidth(double aValue)
    {
        super.setWidth(aValue);
        revalidate();
    }

    /**
     * Override to revalidate.
     */
    public void setHeight(double aValue)
    {
        super.setHeight(aValue);
        revalidate();
    }

    /**
     * Overrides shape implementation to get clip path.
     */
    public Shape getPath()
    {
        // If text doesn't perform wrap or parent is null, return normal path in bounds
        if (!getPerformsWrap() || getParent() == null)
            return getPathShape() != null ? getPathShape().getPath().copyForBounds(getBoundsInside()) : super.getPath();

        // Get peers who cause wrap (if none, just return super path in bounds)
        List<RMShape> peersWhoCauseWrap = getPeersWhoCauseWrap();
        if (peersWhoCauseWrap == null)
            return getPathShape() != null ? getPathShape().getPath().copyForBounds(getBoundsInside()) : super.getPath();

        // Add this text to list
        peersWhoCauseWrap.add(0, this);

        // Get the path minus the neighbors, convert back to this shape, reset bounds to this shape
        _performsWrap = false;
        Shape path = RMShapeUtils.getSubtractedPath(peersWhoCauseWrap, -3);  // INSET NAILED TO -3
        _performsWrap = true;
        path = parentToLocal(path);
        path = path.copyForBounds(getBoundsInside());
        return path;
    }

    /**
     * Returns the subset of children that cause wrap.
     */
    private List<RMShape> getPeersWhoCauseWrap()
    {
        List<RMShape> list = null;

        // Iterate over children and add any that intersect frame
        for (int i = 0, iMax = getParent().getChildCount(); i < iMax; i++) {
            RMShape child = getParent().getChild(i);
            if (child != this && child.getFrame().intersectsShape(getFrame())) {
                if (list == null) list = new ArrayList<>();
                list.add(child);
            }
        }

        // Return
        return list;
    }

    /**
     * This notification method is called when any peer is changed.
     */
    //private void handlePeerChange(RMShape aShape)  {
    //    if (getPerformsWrap() && aShape.getFrame().intersectsRect(getFrame())) { revalidate(); repaint(); } }

    /**
     * Returns the shape that provides the path for this text to wrap text to.
     */
    public RMShape getPathShape()
    {
        return _pathShape;
    }

    RMShape _pathShape;

    /**
     * Sets the shape that provides the path for this text to wrap text to.
     */
    public void setPathShape(RMShape aShape)
    {
        if (Objects.equals(aShape, _pathShape)) return;
        firePropChange("PathShape", _pathShape, _pathShape = aShape);
        revalidate();
        repaint();
    }

    /**
     * Overrides rectangle implementation to potentially clear path shape.
     */
    public void setRadius(float aValue)
    {
        super.setRadius(aValue);
        setPathShape(null);
    }

    /**
     * Returns the linked text for this text (if any).
     */
    public RMLinkedText getLinkedText()
    {
        return _linkedText;
    }

    /**
     * Sets the linked text for this text (if any).
     */
    public void setLinkedText(RMLinkedText aLinkedText)
    {
        // Set linked text, and if non-null, set its previous text to this text
        _linkedText = aLinkedText;
        if (_linkedText != null)
            _linkedText.setPreviousText(this);
        revalidate();
        repaint();
    }

    /**
     * Returns whether there is a text editor.
     */
    public boolean isTextEditorSet()
    {
        return _textEditor != null;
    }

    /**
     * Returns the text editor.
     */
    public RMTextEditor getTextEditor()
    {
        if (_textEditor != null) return _textEditor;
        return _textEditor = new RMTextEditor(getTextLayout());
    }

    /**
     * Clears the text editor.
     */
    public void clearTextEditor()
    {
        _textEditor = null;
    }

    /**
     * Override to compute from RMTextLayout.
     */
    protected double getPrefWidthImpl(double aHeight)
    {
        // If font scaling, return current size
        if (_wraps == WRAP_SCALE) return getWidth();
        if (length() == 0) return 0; // Zero instead of getMarginLeft() + getMarginRight() so empty texts are hidden

        // Get text box width (from first visible char) and return that plus margin
        double pw = getTextLayout().getPrefWidthForStartCharIndex(getVisibleStart());
        return Math.ceil(getMarginLeft() + pw + getMarginRight());
    }

    /**
     * Override to compute from RMTextLayout.
     */
    protected double getPrefHeightImpl(double aWidth)
    {
        if (_wraps == WRAP_SCALE) return getHeight();
        if (length() == 0) return 0; // Zero instead of getMarginTop()+getMarginBottom() so empty texts are hidden
        double ph = getTextLayout().getPrefHeight();
        return Math.ceil(getMarginTop() + ph + getMarginBottom());
    }

    /**
     * Generate report.
     */
    protected RMShape rpgShape(ReportOwner anRptOwner, RMShape aParent)
    {
        RMTextShape clone = clone();
        TextModel cloneTextModel = clone.getTextModel();
        cloneTextModel.setPropChangeEnabled(false);

        // Do xstring RPG (if no change due to RPG, just use normal)
        RMTextShapeUtils.rpgClone(clone.getTextModel(), anRptOwner, null, clone, false);

        // If coalesce newlines is set, coalesce newlines
        if (getCoalesceNewlines())
            RMTextShapeUtils.coalesceNewlines(cloneTextModel);

        // Trim line ends from end of string to prevent extra empty line height
        int len = cloneTextModel.length(), end = len;
        while (end > 0 && CharSequenceUtils.isLineEndChar(cloneTextModel.charAt(end - 1))) end--;
        if (end != len)
            cloneTextModel.removeChars(end, len);

        // If WRAP_SCALE, set FitText ivar
        if (getWraps() == WRAP_SCALE) clone._fitText = true;

        // Enable clone text prop change listeners and revalidate
        cloneTextModel.setPropChangeEnabled(true);
        clone.revalidate();

        // If paginating, swap in paginated parts (disable in table row)
        if (getWraps() == WRAP_BASIC && !(getParent() instanceof RMTableRow)) {
            ReportOwner.ShapeList shapes = new ReportOwner.ShapeList();
            for (RMTextShape text : clone.paginate())
                shapes.addChild(text);
            return shapes;
        }

        // Return clone
        return clone;
    }

    /**
     * Paginates this text by creating linked texts to show all text and returns a list of this text and the linked texts.
     */
    protected List<RMTextShape> paginate()
    {
        // Create pages list with this text in it
        List<RMTextShape> pages = new ArrayList<>();
        pages.add(this);

        // Cache vertical alignment and set to Top
        VPos alignY = getAlignY();
        setAlignY(VPos.TOP);

        // Get linked texts until all text visible
        RMTextShape text = this;
        while (!text.isAllTextVisible()) {
            text = new RMLinkedText(text);
            pages.add(text);
        }

        // Restore alignment on last text and return list
        text.setAlignY(alignY);
        return pages;
    }

    /**
     * Re-does the RPG clone to resolve any @Page@ keys (assumed to be present in userInfo).
     */
    protected void resolvePageReferences(ReportOwner aRptOwner, Object userInfo)
    {
        // Do normal shape resolve page references
        super.resolvePageReferences(aRptOwner, userInfo);

        // RPG clone text again and set
        TextModel textModelCloneRPG = RMTextShapeUtils.rpgClone(getTextModel(), aRptOwner, userInfo, null, true);
        setTextModel(textModelCloneRPG);
    }

    /**
     * Creates a shape suitable for the "remainder" portion of a divideShape call (just a clone by default).
     */
    protected RMShape createDivideShapeRemainder(byte anEdge)
    {
        return anEdge == 0 ? new RMLinkedText(this) : clone();
    }

    /**
     * Editor method - indicates that this shape can be super selected.
     */
    public boolean superSelectable()  { return true; }

    /**
     * Editor method.
     */
    public boolean isStructured()
    {
        return _parent instanceof RMTableRow && ((RMTableRow) _parent).isStructured();
    }

    /**
     * Paints a text shape.
     */
    protected void paintShape(Painter aPntr)
    {
        // Paint normal background
        super.paintShape(aPntr);

        // Clip to shape bounds (cache clip)
        aPntr.save();
        aPntr.clip(getBoundsInside());

        // If editing text, paint TextEditor, otherwise paint text layout
        if (isTextEditorSet())
            TextPainter.DEFAULT.paintTextAdapter(aPntr, getTextEditor());
        else TextPainter.DEFAULT.paintTextLayout(aPntr, getTextLayout());

        // Restore
        aPntr.restore();
    }

    /**
     * Override to catch XString and TextEditor changes.
     */
    protected void handleTextModelPropChange(PropChange aPC)
    {
        _pcs.fireDeepChange(this, aPC);
        repaint();
    }

    /**
     * Override to do home-brew layout.
     */
    public void revalidate()
    {
        // Update text
        if (_textLayout != null)
            updateTextBox();

        // Forward to linked text
        RMLinkedText linkedText = getLinkedText();
        if (linkedText != null) {
            linkedText.revalidate();
            linkedText.repaint();
        }
    }

    /**
     * Standard clone implementation.
     */
    public RMTextShape clone()
    {
        // Get normal shape clone, clone XString, clear layout and return
        RMTextShape clone = (RMTextShape) super.clone();
        clone._textModel = _textModel != null ? _textModel.copyForRange(0, length()) : null;
        clone._textLayout = null;
        clone._textEditor = null;
        clone._textModelPropChangeLsnr = clone::handleTextModelPropChange;
        //clone._textModel.addPropChangeListener(clone._richTextLsnr);
        return clone;
    }

    /**
     * Override to support margin copy.
     */
    public void copyShape(RMShape aShape)
    {
        super.copyShape(aShape);
        RMTextShape other = aShape instanceof RMTextShape ? (RMTextShape) aShape : null;
        if (other == null) return;
        setMargin(other.getMargin());
    }

    /**
     * XML archival.
     */
    public XMLElement toXML(RMArchiver anArchiver)
    {
        XMLElement xml = super.toXML(anArchiver); xml.setName("text");

        // Archive Margin, AlignmentY
        if (getMargin() != getMarginDefault()) xml.add("margin", getMarginString());
        if (getAlignY() != VPos.TOP) xml.add("valign", getAlignYString());

        // Archive Wraps, PerformsWrap
        if (_wraps != 0) xml.add("wrap", _wraps == WRAP_BASIC ? "wrap" : "shrink");
        if (_performsWrap) xml.add("WrapAround", true);

        // Archive CoalesceNewlines, DrawsSelectionRect
        if (_coalesceNewlines) xml.add("coalesce-newlines", true);
        if (_drawsSelectionRect) xml.add("draw-border", true);

        // Archive text model
        if (!(this instanceof RMLinkedText)) {
            XMLElement textModelXml = RMArchiverHpr.textModelToXML(getTextModel(), anArchiver);
            xml.addAll(textModelXml);
        }

        // If linked text present, archive reference to it (it should be archived as normal part of shape hierarchy)
        if (getLinkedText() != null)
            xml.add("linked-text", anArchiver.getReference(getLinkedText()));

        // If there is a path shape, archive path shape
        if (getPathShape() != null) {

            // Get path shape and an element (and add element to master element)
            RMShape pathShape = getPathShape();
            XMLElement pathShapeElement = new XMLElement("path-shape");
            xml.add(pathShapeElement);

            // Archive path shape to path-shape element
            XMLElement pathShapeElementZero = anArchiver.writeObjectToXml(pathShape);
            pathShapeElement.add(pathShapeElementZero);
        }

        return xml;
    }

    /**
     * XML unarchival.
     */
    public Object fromXML(RMArchiver anArchiver, XMLElement anElement)
    {
        // Unarchive basic shape attributes
        super.fromXML(anArchiver, anElement);

        // Unarchive Margin, AlignmentY
        if (anElement.hasAttribute("margin")) setMarginString(anElement.getAttributeValue("margin"));
        if (anElement.hasAttribute("valign"))
            setAlignYString(anElement.getAttributeValue("valign"));

        // Unarchive Wraps, PerformsWrap
        String wrap = anElement.getAttributeValue("wrap", "none");
        if (wrap.equals("wrap")) setWraps(WRAP_BASIC);
        else if (wrap.equals("shrink")) setWraps(WRAP_SCALE);
        setPerformsWrap(anElement.getAttributeBoolValue("WrapAround"));

        // Unarchive CoalesceNewlines, DrawsSelectionRect
        setCoalesceNewlines(anElement.getAttributeBoolValue("coalesce-newlines"));
        if (anElement.getAttributeBoolValue("draw-border")) setDrawsSelectionRect(true);

        // Unarchive xString
        if (!(this instanceof RMLinkedText))
            RMArchiverHpr.textModelFromXML(getTextModel(), anArchiver, anElement);

        // Register for finish call
        anArchiver.getReference(anElement);

        // Unarchive path-shape if present
        if (anElement.get("path-shape") != null) {

            // Get the dedicated path-shape element and its first child (the actual path-shape element)
            XMLElement pathShapeElement = anElement.get("path-shape");
            XMLElement pathShapeElementZero = pathShapeElement.get(0);

            // Unarchive the path shape and set
            RMShape pathShape = (RMShape) anArchiver.readObjectFromXml(pathShapeElementZero);
            setPathShape(pathShape);
        }

        // Return this shape
        return this;
    }

    /**
     * XML reference unarchival - to unarchive linked text.
     */
    //public void fromXMLFinish(RMArchiver anArchiver, XMLElement anElement) {
    //    if (!anElement.hasAttribute("linked-text")) return;
    //    RMLinkedText linkedText = (RMLinkedText) anArchiver.getReference("linked-text", anElement);
    //    setLinkedText(linkedText); }

    /**
     * Standard toSring implementation.
     */
    public String toString()
    {
        String string = super.toString();
        string = string.substring(0, string.length() - 1);
        return string + ", \"" + getText() + "\"]";
    }
}