package reportmill.app;
import rmdraw.app.Editor;
import rmdraw.app.EditorDragDropper;
import reportmill.shape.RMPDFShape;
import rmdraw.scene.SGParent;
import rmdraw.scene.SGView;
import snap.geom.Point;
import snap.view.ClipboardData;
import snap.view.ViewEvent;

/**
 * Subclass of EditorDragDropper to add ReportMill specific features.
 */
public class RMEditorDragDropper extends EditorDragDropper {

    /**
     * Creates a new editor drop target listener.
     */
    public RMEditorDragDropper(Editor anEditor)
    {
        super(anEditor);
    }

    /**
     * Called to drop string.
     */
    public void dropForView(SGView aView, ViewEvent anEvent)
    {
        // If a binding key drop, apply binding
        if (KeysPanel.getDragKey()!=null)
            KeysPanel.dropDragKey(aView, anEvent);

        // Otherwise do normal version
        else super.dropForView(aView, anEvent);
    }

    /**
     * Called to handle drop XML file.
     */
    protected void dropXMLFile(ClipboardData aFile, Point aPoint)
    {
        Editor editor = getEditor();
        RMEditorPane epane = (RMEditorPane) editor.getEditorPane();
        epane.setDataSource(aFile.getSourceURL(), aPoint.getX(), aPoint.getY());
    }

    /**
     * Called to handle an PDF file drop on the editor.
     */
    protected void dropPDFFile(SGView aShape, ClipboardData aFile, Point aPoint)
    {
        // Get image source
        Object imgSrc = aFile.getSourceURL()!=null? aFile.getSourceURL() : aFile.getBytes();

        // If image hit a real shape, see if user wants it to be a texture
        Editor editor = getEditor();
        SGView shape = aShape;
        while(!editor.getToolForView(shape).getAcceptsChildren(shape))
            shape = shape.getParent();

        // Get parent to add image shape to and drop point in parent coords
        SGParent parent = shape instanceof SGParent ? (SGParent)shape : shape.getParent();
        Point point = editor.convertToSceneView(aPoint.x, aPoint.y, parent);

        // Create new PDF shape
        RMPDFShape pdfShape = new RMPDFShape(imgSrc);

        // Create new PDF shape and set bounds centered around point (or at page origin if covers 75% of page or more)
        pdfShape.setXY(point.x - pdfShape.getWidth()/2, point.y - pdfShape.getHeight()/2);
        if(pdfShape.getWidth()/editor.getWidth()>.75f || pdfShape.getHeight()/editor.getHeight()>.75) pdfShape.setXY(0, 0);

        // Add imageShape with undo
        editor.undoerSetUndoTitle("Add Image");
        parent.addChild(pdfShape);

        // Select imageShape and SelectTool
        editor.setSelView(pdfShape);
        editor.setCurrentToolToSelectTool();
    }
}
