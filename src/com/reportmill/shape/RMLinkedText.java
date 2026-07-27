/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.graphics.*;
import snap.gfx.Font;
import snap.text.TextModel;
import snap.util.*;

/**
 * This class is a shape used to render text that didn't fit in a referenced text shape.
 */
public class RMLinkedText extends RMTextShape {

    // Points to previous text
    private RMTextShape _previousText;

    /**
     * Constructor.
     */
    public RMLinkedText()
    {
        super();
    }

    /**
     * Constructor.
     */
    public RMLinkedText(RMTextShape aText)
    {
        copyShape(aText);
        aText.setLinkedText(this);
    }

    /**
     * Returns the text that this text is linked from.
     */
    public RMTextShape getPreviousText()  { return _previousText; }

    /**
     * Sets the text that this text is linked from.
     */
    public void setPreviousText(RMTextShape aText)
    {
        _previousText = aText;
    }

    /**
     * Returns the same text model as previoust text.
     */
    public TextModel getTextModel()  { return getPreviousText().getTextModel(); }

    /**
     * Returns the font for char 0 of the start text.
     */
    public Font getFont()  { return getPreviousText().getFont(); }

    /**
     * Overrides text implementation to return index where previous text left off.
     */
    public int getVisibleStart()
    {
        return getPreviousText() != null ? getPreviousText().getVisibleEnd() : 0;
    }

    /**
     * Overrides shape method to rewire linked text linked list.
     */
    @Override
    protected void setParent(RMParentShape aShape)
    {
        super.setParent(aShape);

        // If removing from share hierarchy, rewire text chain
        if (aShape == null) {
            _previousText.setLinkedText(getLinkedText());
            _previousText.repaint();
        }
    }

    /**
     * XML archival.
     */
    public XMLElement toXML(RMArchiver anArchiver)
    {
        XMLElement e = super.toXML(anArchiver);
        e.setName("linked-text");

        // Add xref id (someday this may happen automatically, just by having source text reference us)
        e.add("xref", anArchiver.getReference(this));

        return e;
    }
}