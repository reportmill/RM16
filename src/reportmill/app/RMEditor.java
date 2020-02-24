package reportmill.app;
import reportmill.apptools.*;
import reportmill.shape.*;
import rmdraw.app.Editor;
import rmdraw.app.EditorDnD;
import rmdraw.app.Tool;
import rmdraw.apptools.*;
import reportmill.shape.RMCrossTab;
import reportmill.shape.RMCrossTabCell;
import reportmill.shape.RMCrossTabDivider;
import reportmill.shape.RMCrossTabFrame;
import reportmill.shape.RMTable;
import reportmill.shape.RMTableGroup;
import reportmill.shape.RMTableRow;
import reportmill.shape.RMSwitchShape;
import reportmill.util.RMDataSource;
import rmdraw.shape.RMTextShape;
import snap.gfx.Image;
import snap.gfx.Painter;
import snap.geom.Rect;
import snap.util.SnapUtils;
import snap.view.ViewAnim;

/**
 * This Editor subclass provides support for some RM specific things.
 */
public class RMEditor extends Editor {

    // Icon for XML image
    private static Image _xmlImage = Image.get(Editor.class, "DS_XML.png");

    // XML Image offset for animation
    private static double       _xmlDX, _xmlDY;

    /**
     * Returns the document associated with this viewer.
     */
    public RMDocument2 getDoc()  { return (RMDocument2)super.getDoc(); }

    /**
     * Creates the shapes helper.
     */
    protected EditorDnD createDragHelper()  { return new RMEditorDnD(this); }

    /**
     * Returns the datasource associated with the editor's document.
     */
    public RMDataSource getDataSource()
    {
        RMDocument2 d = getDoc();
        return d!=null ? d.getDataSource() : null;
    }

    /**
     * Sets the datasource associated with the editor's document.
     */
    public void setDataSource(RMDataSource aDataSource, double aX, double aY)
    {
        // Set Doc.DataSource and repaint
        getDoc().setDataSource(aDataSource);
        repaint();

        // If valid drop point, animate into place
        if(aX>0) {
            Rect vrect = getVisRect();
            double dx = aX - (vrect.getMaxX() - 53);
            double dy = aY - (vrect.getMaxY() - 53);
            getAnimCleared(1800).setOnFrame(() -> setDataSourceAnimFrame(dx, dy)).play();
        }
    }

    /**
     * Called when setDataSource gets frame update.
     */
    private void setDataSourceAnimFrame(double dx, double dy)
    {
        ViewAnim anim = getAnim(0);
        double time = anim.getTime(), maxTime = anim.getMaxTime();
        double ratio = time/maxTime;
        _xmlDX = SnapUtils.doubleValue(anim.interpolate(dx, 0, ratio));
        _xmlDY = SnapUtils.doubleValue(anim.interpolate(dy, 0, ratio));
        repaint();
    }

    /**
     * Returns the sample dataset from the document's datasource.
     */
    public Object getDataSourceDataset()  { RMDataSource ds = getDataSource(); return ds!=null? ds.getDataset() : null; }

    /**
     * Overrides Viewer implementation to paint tool extras, guides, datasource image.
     */
    public void paintFront(Painter aPntr)
    {
        // Do normal paint
        super.paintFront(aPntr);

        // If datasource is present and editing, draw XMLImage in lower right corner of doc
        if(getDataSource()!=null && isEditing()) {

            // Get visible rect and image X & Y
            Rect vrect = getVisRect();
            int x = (int)vrect.getMaxX() - 53;
            int y = (int)vrect.getMaxY() - 53;

            // Draw semi-transparent image: Cache previous composite, set semi-transparent composite, paint image, restore
            aPntr.setOpacity(.9);
            aPntr.drawImage(_xmlImage, x + _xmlDX, y + _xmlDY, 53, 53);
            aPntr.setOpacity(1);
        }
    }

    /**
     * Returns the specific tool for a given shape.
     */
    protected Tool createTool(Class aClass)
    {
        if (aClass== RMCrossTab.class) return new RMCrossTabTool();
        if (aClass== RMCrossTabCell.class) return new RMCrossTabCellTool();
        if (aClass== RMCrossTabDivider.class) return new RMCrossTabDividerTool();
        if (aClass== RMCrossTabFrame.class) return new RMCrossTabFrameTool();
        if (aClass== RMDocument2.class) return new RMDocumentTool();
        if (aClass==RMGraph.class) return new RMGraphTool();
        if (aClass==RMGraphLegend.class) return new RMGraphLegendTool();
        if (aClass==RMGraphPartBars.class) return new RMGraphPartBarsTool();
        if (aClass==RMGraphPartLabelAxis.class) return new RMGraphPartLabelAxisTool();
        if (aClass==RMGraphPartPie.class) return new RMGraphPartPieTool();
        if (aClass==RMGraphPartSeries.class) return new RMGraphPartSeriesTool();
        if (aClass==RMGraphPartValueAxis.class) return new RMGraphPartValueAxisTool();
        if (aClass==RMLabel.class) return new RMLabelTool();
        if (aClass==RMLabels.class) return new RMLabelsTool();
        if (aClass== RMSwitchShape.class) return new RMSwitchShapeTool();
        if (aClass== RMTable.class) return new RMTableTool();
        if (aClass== RMTableGroup.class) return new RMTableGroupTool();
        if (aClass== RMTableRow.class) return new RMTableRowTool();
        if (aClass== RMTextShape.class) return new RMTextTool2();
        if (aClass==RMPDFShape.class) return new RMPDFShapeTool();
        return super.createTool(aClass);
    }
}
