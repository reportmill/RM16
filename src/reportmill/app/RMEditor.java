package reportmill.app;
import reportmill.apptools.*;
import reportmill.shape.*;
import rmdraw.app.Editor;
import rmdraw.app.EditorDnD;
import rmdraw.apptools.*;
import reportmill.shape.RMCrossTab;
import reportmill.shape.RMCrossTabCell;
import reportmill.shape.RMCrossTabDivider;
import reportmill.shape.RMCrossTabFrame;
import reportmill.shape.RMTable;
import reportmill.shape.RMTableGroup;
import reportmill.shape.RMTableRow;

/**
 * This Editor subclass provides support for some RM specific things.
 */
public class RMEditor extends Editor {

    /**
     * Creates the shapes helper.
     */
    protected EditorDnD createDragHelper()  { return new RMEditorDnD(this); }

    /**
     * Returns the specific tool for a given shape.
     */
    protected RMTool createTool(Class aClass)
    {
        if(aClass== RMCrossTab.class) return new RMCrossTabTool();
        if(aClass== RMCrossTabCell.class) return new RMCrossTabCellTool();
        if(aClass== RMCrossTabDivider.class) return new RMCrossTabDividerTool();
        if(aClass== RMCrossTabFrame.class) return new RMCrossTabFrameTool();
        if (aClass==RMGraph.class) return new RMGraphTool();
        if (aClass==RMGraphLegend.class) return new RMGraphLegendTool();
        if (aClass==RMGraphPartBars.class) return new RMGraphPartBarsTool();
        if (aClass==RMGraphPartLabelAxis.class) return new RMGraphPartLabelAxisTool();
        if (aClass==RMGraphPartPie.class) return new RMGraphPartPieTool();
        if (aClass==RMGraphPartSeries.class) return new RMGraphPartSeriesTool();
        if (aClass==RMGraphPartValueAxis.class) return new RMGraphPartValueAxisTool();
        if (aClass==RMLabel.class) return new RMLabelTool();
        if (aClass==RMLabels.class) return new RMLabelsTool();
        if(aClass== RMTable.class) return new RMTableTool();
        if(aClass== RMTableGroup.class) return new RMTableGroupTool();
        if(aClass== RMTableRow.class) return new RMTableRowTool();
        if (aClass==RMPDFShape.class) return new RMPDFShapeTool();
        return super.createTool(aClass);
    }
}
