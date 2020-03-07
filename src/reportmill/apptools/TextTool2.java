/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.apptools;
import reportmill.shape.RMTableRow;
import rmdraw.apptools.TextTool;
import rmdraw.shape.*;

/**
 * This class provides UI editing for text shapes.
 */
public class TextTool2<T extends RMTextShape> extends TextTool<T> {

    /**
     * Returns whether to paint text link indicator.
     */
    protected boolean isPaintingTextLinkIndicator(RMTextShape aText)
    {
        // If text is child of table row, return false
        if (aText.getParent() instanceof RMTableRow) return false;
        return super.isPaintingTextLinkIndicator(aText);
    }

    /**
     * Returns whether given shape is in a Structured TableRow.
     */
    protected boolean isStructured(RMShape aShape)
    {
        RMShape par = aShape.getParent();
        return par instanceof RMTableRow && ((RMTableRow)par).isStructured();
    }

}