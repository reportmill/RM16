package reportmill.app;
import rmdraw.app.Editor;
import rmdraw.app.EditorDnD;

/**
 * This Editor subclass provides support for some RM specific things.
 */
public class RMEditor extends Editor {

    /**
     * Creates the shapes helper.
     */
    protected EditorDnD createDragHelper()  { return new RMEditorDnD(this); }
}
