package reportmill.app;
import rmdraw.app.*;
import rmdraw.apptools.*;

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

}
