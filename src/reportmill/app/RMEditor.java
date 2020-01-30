package reportmill.app;
import reportmill.apptools.*;
import reportmill.shape.*;
import rmdraw.app.Editor;
import rmdraw.app.EditorDnD;
import rmdraw.apptools.*;
import rmdraw.shape.RMArchiver;
import java.util.Map;

/**
 * This Editor subclass provides support for some RM specific things.
 */
public class RMEditor extends Editor {

    /**
     * Creates the shapes helper.
     */
    protected EditorDnD createDragHelper()  { return new RMEditorDnD(this); }

    /**
     * Creates an archiver.
     */
    public RMArchiver createArchiver()
    {
        RMArchiver arch = new RMArchiver();
        Map cmap = arch.getClassMap();
        cmap.put("label", RMLabel.class);
        cmap.put("labels", RMLabels.class);
        return arch;
    }

    /**
     * Returns the specific tool for a given shape.
     */
    protected RMTool createTool(Class aClass)
    {
        if (aClass == RMLabel.class) return new RMLabelTool();
        if (aClass == RMLabels.class) return new RMLabelsTool();
        return super.createTool(aClass);
    }
}
