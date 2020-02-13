package reportmill.app;
import rmdraw.app.*;
import rmdraw.apptools.RMTableRowTool;
import rmdraw.graphics.RMTextEditor;
import snap.gfx.HPos;
import snap.util.ClassUtils;
import snap.util.Prefs;
import snap.util.URLUtils;
import snap.util.Undoer;
import snap.view.ViewEvent;
import snap.view.ViewTheme;
import snap.viewx.RecentFiles;

/**
 * This EditorPaneMenuBar subclass provides some RM specific features.
 */
public class RMEditorPaneMenuBar extends EditorPaneMenuBar {

    /**
     * Creates an RMEditorPaneMenuBar.
     */
    public RMEditorPaneMenuBar(EditorPane anEP)
    {
        super(anEP);
    }

    /**
     * Initialize UI panel.
     */
    protected void initUI()
    {
        // Configure CheckSpellingAsYouTypeMenuItem and HyphenateTextMenuItem
        setViewValue("CheckSpellingAsYouTypeMenuItem", RMTextEditor.isSpellChecking);
        setViewValue("HyphenateTextMenuItem", RMTextEditor.isHyphenating());
    }

    /**
     * Updates the editor's UI.
     */
    protected void resetUI()
    {
        // Get the editor undoer
        Undoer undoer = getEditor().getUndoer();

        // Update UndoMenuItem
        String uTitle = undoer==null || undoer.getUndoSetLast()==null? "Undo" : undoer.getUndoSetLast().getFullUndoTitle();
        setViewValue("UndoMenuItem", uTitle);
        setViewEnabled("UndoMenuItem", undoer!=null && undoer.getUndoSetLast()!=null);

        // Update RedoMenuItem
        String rTitle = undoer==null || undoer.getRedoSetLast()==null? "Redo" : undoer.getRedoSetLast().getFullRedoTitle();
        setViewValue("RedoMenuItem", rTitle);
        setViewEnabled("RedoMenuItem", undoer!=null && undoer.getRedoSetLast()!=null);

        // Update ShowRulersMenuItem
        setViewValue("ShowRulersMenuItem", getEditorPane().isShowRulers());
    }

    /**
     * Handles changes to the editor's UI controls.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Get editor pane
        EditorPane epane = getEditorPane();
        Editor editor = getEditor();

        // Handle NewMenuItem, NewButton: Get new editor pane and make visible
        if (anEvent.equals("NewMenuItem") || anEvent.equals("NewButton")) {
            EditorPane editorPane = ClassUtils.newInstance(epane).newDocument();
            editorPane.setWindowVisible(true);
        }

        // Handle OpenMenuItem, OpenButton: Get new editor pane from open panel and make visible (if created)
        if (anEvent.equals("OpenMenuItem") || anEvent.equals("OpenButton")) {
            EditorPane editorPane = ClassUtils.newInstance(epane).open(epane.getUI());
            if(editorPane!=null)
                editorPane.setWindowVisible(true);
        }

        // Handle OpenRecentMenuItem
        if (anEvent.equals("OpenRecentMenuItem")) {
            String path = RecentFiles.showPathsPanel(epane.getUI(), "RecentDocuments"); if(path==null) return;
            rmdraw.app.Welcome.getShared().open(path); //file.getAbsolutePath());
        }

        // Handle CloseMenuItem
        if (anEvent.equals("CloseMenuItem")) epane.close();

        // Handle SaveMenuItem, SaveButton, SaveAsMenuItem, SaveAsPDFMenuItem, RevertMenuItem
        if (anEvent.equals("SaveMenuItem") || anEvent.equals("SaveButton"))
            epane.save();
        if (anEvent.equals("SaveAsMenuItem"))
            epane.saveAs();
        if (anEvent.equals("SaveAsPDFMenuItem"))
            RMEditorPaneUtils.saveAsPDF(epane);
        if (anEvent.equals("RevertMenuItem"))
            epane.revert();

        // Handle PrintMenuItem, QuitMenuItem
        if (anEvent.equals("PrintMenuItem") || anEvent.equals("PrintButton"))
            editor.print(null, !anEvent.isAltDown());
        if (anEvent.equals("QuitMenuItem"))
            epane.quit();

        // Handle File -> Preview Reports menu items
        if (anEvent.equals("PreviewPDFMenuItem") || anEvent.equals("PreviewPDFButton"))
            RMEditorPaneUtils.previewPDF(epane);
        if (anEvent.equals("PreviewHTMLMenuItem") || anEvent.equals("PreviewHTMLButton"))
            RMEditorPaneUtils.previewHTML(epane);
        if (anEvent.equals("PreviewCSVMenuItem"))
            RMEditorPaneUtils.previewCSV(epane);
        if (anEvent.equals("PreviewExcelMenuItem"))
            RMEditorPaneUtils.previewXLS(epane);
        if (anEvent.equals("PreviewRTFMenuItem"))
            RMEditorPaneUtils.previewRTF(epane);
        if (anEvent.equals("PreviewJPEGMenuItem"))
            RMEditorPaneUtils.previewJPG(epane);
        if (anEvent.equals("PreviewPNGMenuItem"))
            RMEditorPaneUtils.previewPNG(epane);

        // Handle File -> Samples menu items
        if (anEvent.equals("MoviesMenuItem"))
            RMEditorPaneUtils.openSample("Movies");
        if (anEvent.equals("MoviesGraphMenuItem"))
            RMEditorPaneUtils.openSample("MoviesGraph");
        if (anEvent.equals("MoviesLabelsMenuItem"))
            RMEditorPaneUtils.openSample("MoviesLabels");
        if (anEvent.equals("HollywoodMenuItem"))
            RMEditorPaneUtils.openSample("Jar:/reportmill/examples/HollywoodDB.xml");
        if (anEvent.equals("SalesMenuItem"))
            RMEditorPaneUtils.openSample("Jar:/reportmill/examples/Sales.xml");

        // Handle Edit menu items
        if(anEvent.equals("UndoMenuItem") || anEvent.equals("UndoButton")) editor.undo();
        if(anEvent.equals("RedoMenuItem") || anEvent.equals("RedoButton")) editor.redo();
        if(anEvent.equals("CutMenuItem") || anEvent.equals("CutButton")) editor.cut();
        if(anEvent.equals("CopyMenuItem") || anEvent.equals("CopyButton")) editor.copy();
        if(anEvent.equals("PasteMenuItem") || anEvent.equals("PasteButton")) editor.paste();
        if(anEvent.equals("SelectAllMenuItem")) editor.selectAll();
        if(anEvent.equals("CheckSpellingMenuItem")) SpellCheckPanel.getShared().show(editor);

        // Edit -> CheckSpellingAsYouTypeMenuItem
        if(anEvent.equals("CheckSpellingAsYouTypeMenuItem")) {
            RMTextEditor.isSpellChecking = anEvent.getBooleanValue();
            Prefs.get().set("SpellChecking", RMTextEditor.isSpellChecking);
            editor.repaint();
        }

        // Edit -> HyphenateTextMenuItem
        if(anEvent.equals("HyphenateTextMenuItem")) {
            RMTextEditor.setHyphenating(anEvent.getBooleanValue());
            editor.repaint();
        }

        // Handle Format menu items (use name because anObj may come from popup menu)
        if (anEvent.equals("FontPanelMenuItem"))
            epane.getAttributesPanel().setVisibleName(AttributesPanel.FONT);
        if (anEvent.equals("BoldMenuItem") || anEvent.equals("BoldButton"))
            EditorUtils.setFontBold(editor, !EditorUtils.getFont(editor).isBold());
        if (anEvent.equals("ItalicMenuItem") || anEvent.equals("ItalicButton"))
            EditorUtils.setFontItalic(editor, !EditorUtils.getFont(editor).isItalic());
        if (anEvent.equals("UnderlineMenuItem") || anEvent.equals("UnderlineButton"))
            EditorUtils.setUnderlined(editor);
        if (anEvent.equals("OutlineMenuItem"))
            EditorUtils.setTextBorder(editor);
        if (anEvent.equals("AlignLeftMenuItem") || anEvent.equals("AlignLeftButton"))
            EditorUtils.setAlignmentX(editor, HPos.LEFT);
        if (anEvent.equals("AlignCenterMenuItem") || anEvent.equals("AlignCenterButton"))
            EditorUtils.setAlignmentX(editor, HPos.CENTER);
        if (anEvent.equals("AlignRightMenuItem") || anEvent.equals("AlignRightButton"))
            EditorUtils.setAlignmentX(editor, HPos.RIGHT);
        if (anEvent.equals("AlignFullMenuItem") || anEvent.equals("AlignFullButton"))
            EditorUtils.setJustify(editor, true);
        if (anEvent.equals("SuperscriptMenuItem"))
            EditorUtils.setSuperscript(editor);
        if (anEvent.equals("SubscriptMenuItem"))
            EditorUtils.setSubscript(editor);

        // Handle Pages menu items
        if(anEvent.equals("AddPageMenuItem")) editor.addPage();
        if(anEvent.equals("AddPagePreviousMenuItem")) editor.addPagePrevious();
        if(anEvent.equals("RemovePageMenuItem")) editor.removePage();
        if(anEvent.equals("ZoomInMenuItem")) editor.setZoomFactor(editor.getZoomFactor() + .1f);
        if(anEvent.equals("ZoomOutMenuItem")) editor.setZoomFactor(editor.getZoomFactor() - .1f);
        if(anEvent.equals("Zoom100MenuItem")) editor.setZoomFactor(1);
        if(anEvent.equals("Zoom200MenuItem")) editor.setZoomFactor(2);
        if(anEvent.equals("ZoomToggleLastMenuItem")) editor.zoomToggleLast();
        if(anEvent.equals("ZoomToMenuItem")) epane.runZoomPanel();

        // Handle Shapes menu items (use name because anObj may come from popup menu)
        String name = anEvent.getName();
        if(name.equals("GroupMenuItem")) EditorUtils.groupShapes(editor, null, null);
        if(name.equals("UngroupMenuItem")) EditorUtils.ungroupShapes(editor);
        if(name.equals("BringToFrontMenuItem")) EditorUtils.bringToFront(editor);
        if(name.equals("SendToBackMenuItem")) EditorUtils.sendToBack(editor);
        if(name.equals("MakeRowTopMenuItem")) EditorUtils.makeRowTop(editor);
        if(name.equals("MakeRowCenterMenuItem")) EditorUtils.makeRowCenter(editor);
        if(name.equals("MakeRowBottomMenuItem")) EditorUtils.makeRowBottom(editor);
        if(name.equals("MakeColumnLeftMenuItem")) EditorUtils.makeColumnLeft(editor);
        if(name.equals("MakeColumnCenterMenuItem")) EditorUtils.makeColumnCenter(editor);
        if(name.equals("MakeColumnRightMenuItem")) EditorUtils.makeColumnRight(editor);
        if(name.equals("MakeSameSizeMenuItem")) EditorUtils.makeSameSize(editor);
        if(name.equals("MakeSameWidthMenuItem")) EditorUtils.makeSameWidth(editor);
        if(name.equals("MakeSameHeightMenuItem")) EditorUtils.makeSameHeight(editor);
        if(name.equals("SizeToFitMenuItem")) EditorUtils.setSizeToFit(editor);
        if(name.equals("EquallySpaceRowMenuItem")) EditorUtils.equallySpaceRow(editor);
        if(name.equals("EquallySpaceColumnMenuItem")) EditorUtils.equallySpaceColumn(editor);
        if(name.equals("GroupInSwitchShapeMenuItem")) EditorUtils.groupInSwitchShape(editor);
        if(name.equals("GroupInScene3DMenuItem")) EditorUtils.groupInScene3D(editor);
        if(name.equals("MoveToNewLayerMenuItem")) EditorUtils.moveToNewLayer(editor);
        if(name.equals("CombinePathsMenuItem")) EditorUtils.combinePaths(editor);
        if(name.equals("SubtractPathsMenuItem")) EditorUtils.subtractPaths(editor);
        if(name.equals("ConvertToImageMenuItem")) EditorUtils.convertToImage(editor);

        // Handle Tools menu items
        if(anEvent.equals("InspectorMenuItem")) epane.getInspectorPanel().setVisible(-1);
        if(anEvent.equals("ColorPanelMenuItem")) epane.getAttributesPanel().setVisibleName(AttributesPanel.COLOR);
        if(anEvent.equals("FormatPanelMenuItem")) epane.getAttributesPanel().setVisibleName(AttributesPanel.FORMAT);
        if(anEvent.equals("KeysPanelMenuItem")) epane.getAttributesPanel().setVisibleName(AttributesPanel.KEYS);

        // Handle ShowRulersMenuItem, FeedbackMenuItem, PrefsMenuItem
        if(anEvent.equals("ShowRulersMenuItem")) epane.setShowRulers(!epane.isShowRulers());
        if(anEvent.equals("FeedbackMenuItem")) new FeedbackPanel().showPanel(epane.getUI());
        if(anEvent.equals("PrefsMenuItem")) new PrefsPanel().showPanel(epane.getUI());

        // Handle SupportPageMenuItem, TutorialMenuItem, BasicAPIMenuItem, TablesMenuItem
        if(anEvent.equals("SupportPageMenuItem")) URLUtils.openURL("http://reportmill.com/support");
        if(anEvent.equals("TutorialMenuItem")) URLUtils.openURL("http://reportmill.com/support/tutorial.pdf");
        if(anEvent.equals("BasicAPIMenuItem")) URLUtils.openURL("http://reportmill.com/support/BasicApi.pdf");
        if(anEvent.equals("TablesMenuItem")) URLUtils.openURL("http://reportmill.com/support/tables.pdf");

        // Handle AddColumnMenuItem, SplitColumnMenuItem (from right mouse pop-up)
        if(anEvent.equals("AddColumnMenuItem")) RMTableRowTool.addColumn(editor);
        if(anEvent.equals("SplitColumnMenuItem")) EditorUtils.splitHorizontal(editor);

        // Handle Theme menus: StandardThemeMenuItem, LightThemeMenuItem, DarkThemeMenuItem, BlackAndWhiteThemeMenuItem
        if(anEvent.equals("StandardThemeMenuItem")) ViewTheme.setThemeForName("Standard");
        if(anEvent.equals("LightThemeMenuItem")) ViewTheme.setThemeForName("Light");
        if(anEvent.equals("DarkThemeMenuItem")) ViewTheme.setThemeForName("Dark");
        if(anEvent.equals("BlackAndWhiteThemeMenuItem")) ViewTheme.setThemeForName("BlackAndWhite");
    }
}
