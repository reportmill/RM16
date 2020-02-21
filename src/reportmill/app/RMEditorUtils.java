package reportmill.app;
import reportmill.apptools.RMCrossTabTool;
import reportmill.apptools.RMGraphTool;
import reportmill.apptools.RMLabelsTool;
import reportmill.apptools.RMTableTool;
import reportmill.shape.RMSwitchShape;
import rmdraw.app.*;
import rmdraw.shape.RMShape;
import rmdraw.shape.RMShapeUtils;
import snap.gfx.TextFormat;

import java.util.List;

/**
 * Utility methods for editor specific to ReportMill.
 */
public class RMEditorUtils extends EditorUtils {

    /**
     * Runs the dataset key panel to add a table, graph, crosstab or labels to given editor.
     */
    public static void runDatasetKeyPanel(Editor anEditor, String aKeyPath)
    {
        // Hide AttributesPanel Drawer
        EditorPane editorPane = anEditor.getEditorPane();
        editorPane.hideAttributesDrawer();

        // Run dataset key panel to get dataset element type
        int type = new DatasetKeyPanel().showDatasetKeyPanel(anEditor);

        // Add appropriate dataset key element for returned type
        switch(type) {
            case DatasetKeyPanel.TABLE: RMTableTool.addTable(anEditor, aKeyPath); break;
            case DatasetKeyPanel.GRAPH: RMGraphTool.addGraph(anEditor, aKeyPath); break;
            case DatasetKeyPanel.LABELS: RMLabelsTool.addLabels(anEditor, aKeyPath); break;
            case DatasetKeyPanel.CROSSTAB: RMCrossTabTool.addCrossTab(anEditor, aKeyPath); break;
        }

        // If EditorPane.Inspector showing DataSource inspector, reset os ShapeSpecific
        if(editorPane.getInspectorPanel().isShowingDataSource())
            editorPane.getInspectorPanel().setVisible(0);
    }

    /**
     * Adds the selected shapes to a Switch Shape.
     */
    public static void groupInSwitchShape(Editor anEditor)
    {
        // Get selected shapes and parent (just return if no shapes)
        List<RMShape> shapes = anEditor.getSelectedShapes(); if(shapes.size()==0) { anEditor.beep(); return; }
        RMShape parent = anEditor.getSelectedShape(0).getParent();

        // Create switch shape to hold selected shapes with fram of combined bounds of children (ouset by just a little)
        RMSwitchShape groupShape = new RMSwitchShape();
        groupShape.setFrame(RMShapeUtils.getBoundsOfChildren(parent, shapes).getInsetRect(-2));

        // Add shapes to group shape (with undo title)
        anEditor.undoerSetUndoTitle("Group in Switch Shape");
        groupShapes(anEditor, shapes, groupShape);
    }

    /**
     * Returns the format of the editor's selected shape.
     */
    public static TextFormat getFormat(Editor anEditor)
    {
        return anEditor.getSelectedOrSuperSelectedShape().getFormat();
    }

    /**
     * Sets the format of editor's selected shape(s).
     */
    public static void setFormat(Editor anEditor, TextFormat aFormat)
    {
        for (RMShape shape : anEditor.getSelectedOrSuperSelectedShapes())
            shape.setFormat(aFormat);
    }
}
