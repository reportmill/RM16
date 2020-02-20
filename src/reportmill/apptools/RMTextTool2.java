/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.apptools;
import reportmill.shape.RMTableRow;
import rmdraw.apptools.RMTextTool;
import rmdraw.shape.*;

/**
 * This class provides UI editing for text shapes.
 */
public class RMTextTool2<T extends RMTextShape> extends RMTextTool<T> {

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