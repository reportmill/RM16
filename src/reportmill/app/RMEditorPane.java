package reportmill.app;
import reportmill.shape.RMDoc;
import rmdraw.app.*;
import reportmill.util.RMDataSource;
import reportmill.util.RMKey;
import reportmill.util.RMKeyChain;
import rmdraw.scene.SGDoc;
import snap.geom.Rect;
import snap.util.StringUtils;
import snap.view.ViewEvent;
import snap.view.ViewOwner;
import snap.viewx.DialogBox;
import snap.web.WebURL;

/**
 * This EditorPane subclass provides for some RM specific things.
 */
public class RMEditorPane extends EditorPane {

    // The original editor, if in preview mode
    RMEditor _realEditor;

    /**
     * Override to create new doc.
     */
    public EditorPane newDocument()
    {
        return open(new RMDoc(612, 792));
    }

    /**
     * Override to return as RMEditor.
     */
    public RMEditor getEditor()  { return (RMEditor)getViewer(); }

    /**
     * Override to return RMDoc.
     */
    public RMDoc getDoc()  { return (RMDoc)super.getDoc(); }

    /**
     * Override to return an RMEditor.
     */
    protected Viewer createViewer()  { return new RMEditor(); }

    /**
     * Override to return as RMEditorPaneToolBar.
     */
    public RMEditorPaneToolBar getTopToolBar()  { return (RMEditorPaneToolBar)super.getTopToolBar(); }

    /**
     * Creates the top tool bar.
     */
    protected ViewOwner createTopToolBar()  { return new RMEditorPaneToolBar(this); }

    /**
     * Returns the SwingOwner for the menu bar.
     */
    public RMEditorPaneMenuBar getMenuBar()  { return (RMEditorPaneMenuBar)super.getMenuBar(); }

    /**
     * Creates the EditorPaneMenuBar for the menu bar.
     */
    protected EditorPaneMenuBar createMenuBar()  { return new RMEditorPaneMenuBar(this); }

    /**
     * Override to check version and return as RMEditorPane.
     */
    public RMEditorPane open(Object aSource)
    {
        // Do normal version
        RMEditorPane epane = (RMEditorPane) super.open(aSource);

        // If old version, warn user that saving document will make it unreadable by RM7
        RMDoc doc = getDoc();
        if(doc.getVersion()<7.0) {
            String msg = "This document has been upgraded from an older version.\n" +
                    "If saved, it will not open in earlier versions.";
            DialogBox dbox = new DialogBox("Warning: Document Upgrade"); dbox.setWarningMessage(msg);
            dbox.showMessageDialog(getUI());
        }

        // Return
        return epane;
    }

    /**
     * Returns the datasource associated with the editor's document.
     */
    public RMDataSource getDataSource()  { return getEditor().getDataSource(); }

    /**
     * Sets the datasource associated with the editor's document.
     */
    public void setDataSource(RMDataSource aDataSource)  { setDataSource(aDataSource, -1, -1); }

    /**
     * Sets the datasource for the panel.
     */
    public void setDataSource(RMDataSource aDataSource, double aX, double aY)
    {
        // Set DataSource in editor, show DataSource inspector, KeysBrowser and refocus window
        getEditor().setDataSource(aDataSource, aX, aY);
        if(getWindow().isVisible())
            getWindow().toFront();

        // Show KeysPanel (after delay, so XML icon animation can finish)
        runLaterDelayed(2000, () -> getAttributesPanel().setVisibleName(AttributesPanel.KEYS));
    }

    /**
     * Sets a datasource from a given URL at a given point (if dragged in).
     */
    public void setDataSource(WebURL aURL, double aX, double aY)
    {
        // Create DataSource and load dataset
        RMDataSource dsource = new RMDataSource(aURL);
        try { dsource.getDataset(); }

        // If failed, get error message and run error panel
        catch(Throwable t) {
            while(t.getCause()!=null) t = t.getCause(); // Get root cause
            String e1 = StringUtils.wrap(t.toString(), 40);
            Object line = RMKey.getValue(t, "LineNumber"), column = RMKey.getValue(t, "ColumnNumber");
            if(line!=null || column!=null) e1 += "\nLine: " + line + ", Column: " + column;
            else t.printStackTrace();
            String error = e1;
            runLater(() -> {
                DialogBox dbox = new DialogBox("Error Parsing XML"); dbox.setErrorMessage(error);
                dbox.showMessageDialog(getUI()); });
            return;
        }

        // Set DataSource in editor, show DataSource inspector, KeysBrowser and refocus window
        setDataSource(dsource, aX, aY);
    }

    /**
     * Returns whether editor is really doing editing.
     */
    public boolean isEditing()  { return getEditor().isEditing(); }

    /**
     * Sets whether editor is really doing editing.
     */
    public void setEditing(boolean aFlag)
    {
        // If editor already has requested editing state, just return
        if(aFlag == isEditing()) return;

        // Hide attributes drawer
        hideAttributesDrawer();

        // If not yet previewing, store current template then generate report and swap it in
        if(!aFlag) {

            // Cache current editor
            _realEditor = getEditor();

            // Generate report and restore filename
            SGDoc report = getDoc().generateReport(getEditor().getDataSourceDataset());

            // Create new editor, set editing to false and set report document
            RMEditor editor = new RMEditor();
            editor.setEditing(false);
            editor.setDoc(report);

            // If generateReport hit any keyChain parsing errors, run message dialog
            if(RMKeyChain.getError()!=null) { String err = RMKeyChain.getAndResetError();
                DialogBox dbox = new DialogBox("Error Parsing KeyChain"); dbox.setErrorMessage(err);
                dbox.showMessageDialog(getUI());
            }

            // Set new editor
            setViewer(editor);
        }

        // If turning preview off, restore real editor
        else setViewer(_realEditor);

        // Focus on editor
        requestFocus(getEditor());
        resetLater();
    }

    /**
     * Handles changes to the editor's UI controls.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Do normal version (just return if consumed)
        super.respondUI(anEvent);
        if(anEvent.isConsumed())
            return;

        // If Editor.MouseClick and DataSource is set and we're editing and DataSource icon clicked, show DS Inspector
        if(anEvent.isMouseClick() && getDataSource()!=null && isEditing()) {
            Rect r = getEditor().getVisRect(); // Get visible rect
            if(anEvent.getX()>r.getMaxX()-53 && anEvent.getY()>r.getMaxY()-53) { // If DataSource icon clicked
                if(anEvent.isShortcutDown()) setDataSource(null); // If cmd key down, clear the DataSource
                else getInspectorPanel().setVisible(7); // otherwise show DataSource inspector
            }

            // If mouse isn't in lower right corner and DataSource inspector is showing, show shape specific inspector
            else if(getInspectorPanel().isShowingDataSource())
                getInspectorPanel().setVisible(0);
        }
    }

    /**
     * Overide to return RMAttributesPanel.
     */
    @Override
    protected AttributesPanel createAttributesPanel()  { return new RMAttributesPanel(this); }

    /**
     * A class for any editor pane support panes.
     */
    public static class RMSupportPane extends SupportPane {

        /** Creates a new SupportPane with given editor pane. */
        public RMSupportPane(RMEditorPane anEP)  { super(anEP); }

        /** Returns the EditorPane. */
        public RMEditorPane getEditorPane()  { return (RMEditorPane) super.getEditorPane(); }

        /** Returns the editor. */
        public RMEditor getEditor()  { return (RMEditor) super.getEditor(); }
    }

}
