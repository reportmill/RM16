package reportmill.app;
import rmdraw.app.Editor;
import rmdraw.app.EditorDnD;
import rmdraw.shape.RMShape;
import snap.view.ViewEvent;

/**
 * Subclass of EditorDnD to add ReportMill specific features.
 */
public class RMEditorDnD extends EditorDnD {

    /**
     * Creates a new editor drop target listener.
     */
    public RMEditorDnD(Editor anEditor)
    {
        super(anEditor);
    }

    /**
     * Called to drop string.
     */
    public void dropForView(RMShape aView, ViewEvent anEvent)
    {
        // If a binding key drop, apply binding
        if (KeysPanel.getDragKey()!=null)
            KeysPanel.dropDragKey(aView, anEvent);

        // Otherwise do normal version
        else super.dropForView(aView, anEvent);
    }
}
