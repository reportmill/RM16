package reportmill.app;
import reportmill.apptools.RMCrossTabTool;
import reportmill.apptools.RMGraphTool;
import reportmill.apptools.RMLabelsTool;
import rmdraw.app.*;
import rmdraw.apptools.*;
import rmdraw.base.RMFormat;
import rmdraw.base.RMNumberFormat;
import rmdraw.shape.*;
import snap.gfx.*;
import snap.util.SnapUtils;
import snap.util.Undoer;
import snap.view.*;
import snap.viewx.ColorButton;
import snap.viewx.ColorPanel;
import snap.viewx.ColorWell;
import java.util.ArrayList;
import java.util.List;

/**
 * Override EditorPaneToolBar to provide RM specific features.
 */
public class RMEditorPaneToolBar extends EditorPaneToolBar {

    // The font face ComboBox
    private ComboBox <String> _fontFaceComboBox;

    // The font size ComboBox
    private ComboBox _fontSizeComboBox;

    // The editor selected color ColorWell (hidden)
    private ColorWell _colorWell;

    /**
     * Creates an RMEditorPaneToolBar.
     */
    public RMEditorPaneToolBar(EditorPane anEP)
    {
        super(anEP);
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Get/configure FontFaceComboBox
        _fontFaceComboBox = getView("FontFaceComboBox", ComboBox.class);
        _fontFaceComboBox.getPopupList().setMaxRowCount(20);
        _fontFaceComboBox.setItems(Font.getFamilyNames());

        // Get/configure FontSizeComboBox
        _fontSizeComboBox = getView("FontSizeComboBox", ComboBox.class);
        Object sizes[] = { 6, 8, 9, 10, 11, 12, 14, 16, 18, 22, 24, 36, 48, 64, 72, 96, 128, 144 };
        _fontSizeComboBox.setItems(sizes);
        _fontSizeComboBox.setItemTextFunction(i -> SnapUtils.stringValue(i) + " pt");

        // Create/configure hidden ColorWell
        _colorWell = new ColorWell(); _colorWell.setName("ColorWell");
        ColorPanel.getShared().setDefaultColorWell(_colorWell);
        _colorWell.setOwner(this);

        // Install InspectorPanel.TitleLabel
        RowView rowView = (RowView)getUI(ColView.class).getChild(1);
        Label titleLabel = getEditorPane().getInspectorPanel().getView("TitleLabel", Label.class);
        titleLabel.setAlign(Pos.BOTTOM_CENTER);
        titleLabel.setPadding(0,0,0,0);
        titleLabel.setPrefSize(275, 20);
        titleLabel.setLean(Pos.BOTTOM_RIGHT);
        titleLabel.setTransY(2);
        rowView.addChild(titleLabel);
    }

    /**
     * Updates the UI panel controls.
     */
    protected void resetUI()
    {
        // Get the editor
        Editor editor = getEditor();
        Font font = EditorUtils.getFont(editor);

        // Update UndoButton, RedoButton
        Undoer undoer = editor.getUndoer();
        setViewEnabled("UndoButton", undoer!=null && undoer.getUndoSetLast()!=null);
        setViewEnabled("RedoButton", undoer!=null && undoer.getRedoSetLast()!=null);

        // Update MoneyButton, PercentButton, CommaButton
        TextFormat fmt = EditorUtils.getFormat(editor);
        RMNumberFormat nfmt = fmt instanceof RMNumberFormat? (RMNumberFormat)fmt : null;
        setViewValue("MoneyButton", nfmt!=null && nfmt.isLocalCurrencySymbolUsed());
        setViewValue("PercentButton", nfmt!=null && nfmt.isPercentSymbolUsed());
        //setViewValue("CommaButton", nfmt!=null && nfmt.isGroupingUsed());

        // Reset PreviewEditButton state if out of sync
        if(getViewBoolValue("PreviewEditButton")==getEditorPane().isEditing())
            setViewValue("PreviewEditButton", !getEditorPane().isEditing());

        // Get selected tool button name and button - if found and not selected, select it
        String toolButtonName = editor.getCurrentTool().getClass().getSimpleName() + "Button";
        ToggleButton toolButton = getView(toolButtonName, ToggleButton.class);
        if(toolButton!=null && !toolButton.isSelected())
            toolButton.setSelected(true);

        // Reset FontFaceComboBox, FontSizeComboBox
        _fontFaceComboBox.setSelItem(font.getFamily());
        String fstext = _fontSizeComboBox.getText(font.getSize());
        _fontSizeComboBox.setText(fstext);

        // Reset BoldButton, ItalicButton, UnderlineButton
        setViewValue("BoldButton", font.isBold());
        setViewEnabled("BoldButton", font.getBold()!=null);

        // Update ColorWell
        Color color = EditorUtils.getSelectedColor(editor);
        _colorWell.setColor(color);
    }

    /**
     * Responds to UI panel control changes.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Get the editor
        RMEditorPane epane = (RMEditorPane)getEditorPane();
        Editor editor = getEditor();

        // Handle File NewButton, OpenButton, SaveButton, PreviewPDFButton, PreviewHTMLButton, PrintButton
        if (anEvent.equals("NewButton")) epane.respondUI(anEvent);
        if (anEvent.equals("OpenButton")) epane.respondUI(anEvent);
        if (anEvent.equals("SaveButton")) epane.respondUI(anEvent);
        if (anEvent.equals("PreviewPDFButton")) epane.respondUI(anEvent);
        if (anEvent.equals("PreviewHTMLButton")) epane.respondUI(anEvent);
        if (anEvent.equals("PrintButton")) epane.respondUI(anEvent);

        // Handle Edit CutButton, CopyButton, PasteButton, DeleteButton
        if (anEvent.equals("CutButton")) epane.respondUI(anEvent);
        if (anEvent.equals("CopyButton")) epane.respondUI(anEvent);
        if (anEvent.equals("PasteButton")) epane.respondUI(anEvent);
        if (anEvent.equals("DeleteButton")) editor.delete();

        // Handle Edit UndoButton, RedoButton
        if (anEvent.equals("UndoButton")) epane.respondUI(anEvent);
        if (anEvent.equals("RedoButton")) epane.respondUI(anEvent);

        // Handle FillColorButton, StrokeColorButton, TextColorButton
        if (anEvent.equals("FillColorButton"))
            EditorUtils.setColor(editor, anEvent.getView(ColorButton.class).getColor());
        if (anEvent.equals("StrokeColorButton"))
            EditorUtils.setStrokeColor(editor, anEvent.getView(ColorButton.class).getColor());
        if (anEvent.equals("TextColorButton"))
            EditorUtils.setTextColor(editor, anEvent.getView(ColorButton.class).getColor());

        // Handle MoneyButton: If currently selected format is number format, add or remove dollars
        TextFormat fmt = EditorUtils.getFormat(editor);
        RMNumberFormat nfmt = fmt instanceof RMNumberFormat? (RMNumberFormat)fmt : null;
        if (anEvent.equals("MoneyButton")) {
            if(nfmt==null) EditorUtils.setFormat(editor, RMNumberFormat.CURRENCY);
            else { nfmt = nfmt.clone(); // Clone it
                nfmt.setLocalCurrencySymbolUsed(!nfmt.isLocalCurrencySymbolUsed()); // Toggle whether $ is used
                EditorUtils.setFormat(editor, nfmt); }
        }

        // Handle PercentButton: If currently selected format is number format, add or remove percent symbol
        if (anEvent.equals("PercentButton")) {
            if(nfmt==null) EditorUtils.setFormat(editor, new RMNumberFormat("#,##0.00 %"));
            else { nfmt = nfmt.clone(); // Clone it
                nfmt.setPercentSymbolUsed(!nfmt.isPercentSymbolUsed()); // Toggle whether percent symbol is used
                EditorUtils.setFormat(editor, nfmt); }
        }

        // Handle CommaButton: If currently selected format is number format, add or remove grouping
        if (anEvent.equals("CommaButton")) {
            if(nfmt==null) EditorUtils.setFormat(editor, new RMNumberFormat("#,##0.00"));
            else { nfmt = nfmt.clone();
                nfmt.setGroupingUsed(!nfmt.isGroupingUsed()); // Toggle whether grouping is used
                EditorUtils.setFormat(editor, nfmt); }
        }

        // Handle DecimalAddButton: If currently selected format is number format, add decimal
        if (anEvent.equals("DecimalAddButton") && nfmt!=null) {
            nfmt = nfmt.clone();
            nfmt.setMinimumFractionDigits(nfmt.getMinimumFractionDigits()+1);
            nfmt.setMaximumFractionDigits(nfmt.getMinimumFractionDigits());
            EditorUtils.setFormat(editor, nfmt);
        }

        // Handle DecimalRemoveButton: If currently selected format is number format, remove decimal digits
        if (anEvent.equals("DecimalRemoveButton") && nfmt!=null) {
            nfmt = nfmt.clone();
            nfmt.setMinimumFractionDigits(nfmt.getMinimumFractionDigits()-1);
            nfmt.setMaximumFractionDigits(nfmt.getMinimumFractionDigits());
            EditorUtils.setFormat(editor, nfmt);
        }

        // Handle SamplesButton
        if (anEvent.equals("SamplesButton")) {
            stopSamplesButtonAnim();
            epane.hideAttributesDrawer();
            new SamplesPane().showSamples(epane);
        }

        // Handle Preview/Edit button and PreviewMenuItem
        if (anEvent.equals("PreviewEditButton") || anEvent.equals("PreviewMenuItem")) {

            // Hack to open edited file as text file
            if(anEvent.isAltDown()) openDocTextFile();

            // Normal preview
            else epane.setEditing(!epane.isEditing());
        }

        // Handle PreviewXMLMenuItem
        if (anEvent.equals("PreviewXMLMenuItem"))
            EditorPaneUtils.previewXML(epane);

        // Handle ToolButton(s)
        if (anEvent.getName().endsWith("ToolButton")) {
            for(RMTool tool : getToolBarTools())
                if(anEvent.getName().startsWith(tool.getClass().getSimpleName())) {
                    getEditor().setCurrentTool(tool); break; }
        }

        // Handle FontFaceComboBox
        if (anEvent.equals("FontFaceComboBox")) {
            String familyName = anEvent.getText();
            String fontNames[] = Font.getFontNames(familyName); if(fontNames==null || fontNames.length==0) return;
            String fontName = fontNames[0];
            Font font = Font.get(fontName, 12);
            EditorUtils.setFontFamily(editor, font);
            editor.requestFocus();
        }

        // Handle FontSizeComboBox
        if (anEvent.equals("FontSizeComboBox")) {
            EditorUtils.setFontSize(editor, anEvent.getFloatValue(), false);
            editor.requestFocus();
        }

        // Handle FontSizeUpButton, FontSizeDownButton
        if (anEvent.equals("FontSizeUpButton")) { Font font = EditorUtils.getFont(editor);
            EditorUtils.setFontSize(editor, font.getSize()<16? 1 : 2, true); }
        if (anEvent.equals("FontSizeDownButton")) { Font font = EditorUtils.getFont(editor);
            EditorUtils.setFontSize(editor, font.getSize()<16? -1 : -2, true); }

        // Handle BoldButton, ItalicButton, UnderlineButton
        if (anEvent.equals("BoldButton")) EditorUtils.setFontBold(editor, anEvent.getBoolValue());
        if (anEvent.equals("ItalicButton")) EditorUtils.setFontItalic(editor, anEvent.getBoolValue());
        if (anEvent.equals("UnderlineButton")) EditorUtils.setUnderlined(editor);

        // Handle AlignLeftButton, AlignCenterButton, AlignRightButton, AlignFullButton
        if (anEvent.equals("AlignLeftButton"))
            EditorUtils.setAlignmentX(editor, HPos.LEFT);
        if (anEvent.equals("AlignCenterButton"))
            EditorUtils.setAlignmentX(editor, HPos.CENTER);
        if (anEvent.equals("AlignRightButton"))
            EditorUtils.setAlignmentX(editor, HPos.RIGHT);
        if (anEvent.equals("AlignFullButton"))
            EditorUtils.setJustify(editor, true);

        // Handle AddTableButton, AddGraphButton, AddLabelsButton, AddCrossTabFrameButton
        if(anEvent.equals("AddTableButton")) RMTableTool.addTable(getEditor(), null);
        if(anEvent.equals("AddGraphButton")) RMGraphTool.addGraph(getEditor(), null);
        if(anEvent.equals("AddLabelsButton")) RMLabelsTool.addLabels(getEditor(), null);
        if(anEvent.equals("AddCrossTabFrameButton")) RMCrossTabTool.addCrossTab(getEditor(), null);

        // Handle AddCrossTabButton, AddImagePlaceHolderMenuItem
        if(anEvent.equals("AddCrossTabButton")) RMCrossTabTool.addCrossTab(getEditor());
        if(anEvent.equals("AddImagePlaceHolderMenuItem")) EditorUtils.addImagePlaceholder(getEditor());

        // Handle ConnectToDataSourceMenuItem
        if(anEvent.equals("ConnectToDataSourceMenuItem") || anEvent.equals("ConnectToDataSourceButton"))
            RMEditorPaneUtils.connectToDataSource(epane);

        // Handle ColorWell
        if(anEvent.equals("ColorWell"))
            EditorUtils.setSelectedColor(editor, _colorWell.getColor());
    }

    /**
     * Opens the editor document as a text file.
     */
    private void openDocTextFile()
    {
        // Get filename for doc (if not set, write doc to temp file)
        String fname = getEditor().getDoc().getFilename();
        if(fname==null) {
            fname = SnapUtils.getTempDir() + "RMDocument.rpt";
            getEditor().getDoc().write(fname);
        }

        // Open file
        GFXEnv.getEnv().openTextFile(fname);
    }

    /**
     * Animate SampleButton.
     */
    public void startSamplesButtonAnim()
    {
        // Get button
        View btn = getView("SamplesButton");
        btn.setScale(1.2);

        // Configure anim
        ViewAnim anim = btn.getAnim(0);
        anim.getAnim(400).setScale(1.4).getAnim(800).setScale(1.2).getAnim(1200).setScale(1.4).getAnim(1600).setScale(1.2)
                .getAnim(2400).setRotate(360);
        anim.setLoopCount(3).play();
    }

    /**
     * Stops SampleButton animation.
     */
    public void stopSamplesButtonAnim()
    {
        View btn = getView("SamplesButton");
        btn.getAnim(0).finish();
    }

    /**
     * Creates the list of tool instances for tool bar.
     */
    protected RMTool[] createToolBarTools()
    {
        List<RMTool> tools = new ArrayList();
        Editor editor = getEditor();
        tools.add(editor.getSelectTool());
        tools.add(editor.getTool(RMLineShape.class));
        tools.add(editor.getTool(RMRectShape.class));
        tools.add(editor.getTool(RMOvalShape.class));
        tools.add(editor.getTool(RMTextShape.class));
        tools.add(editor.getTool(RMPolygonShape.class));
        tools.add(new RMPolygonShapeTool.PencilTool(editor));
        return tools.toArray(new RMTool[0]);
    }
}
