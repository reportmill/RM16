/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.apptools.*;
import com.reportmill.base.RMNumberFormat;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.geom.Pos;
import snap.gfx.*;
import snap.props.Undoer;
import snap.text.TextFormat;
import snap.util.*;
import snap.view.*;
import snap.viewx.*;

/**
 * Tool bar for RMEditorPane.
 */
public class RMEditorPaneToolBar extends RMEditorPane.SupportPane {

    // The font face ComboBox
    private ComboBox<String> _fontFaceComboBox;

    // The font size ComboBox
    private ComboBox<Number> _fontSizeComboBox;

    // The editor selected color ColorWell (hidden)
    private ColorWell _colorWell;

    // The toolbar tools
    private List<RMTool<?>> _toolBarTools;

    // Constant for standard font sizes
    private static Number[] sizes = { 6, 8, 9, 10, 11, 12, 14, 16, 18, 22, 24, 36, 48, 64, 72, 96, 128, 144 };

    /**
     * Constructor.
     */
    public RMEditorPaneToolBar(RMEditorPane editorPane)
    {
        super(editorPane);
        _toolBarTools = createToolBarTools();
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
        _fontSizeComboBox.setItems(sizes);
        _fontSizeComboBox.setItemTextFunction(i -> Convert.stringValue(i) + " pt");

        // Create/configure hidden ColorWell
        _colorWell = new ColorWell();
        _colorWell.setName("ColorWell");
        ColorPanel.getShared().setDefaultColorWell(_colorWell);
        _colorWell.setController(this);

        // Install InspectorPanel.TitleLabel
        RowView rowView = (RowView) getUI(ColView.class).getChild(1);
        Label titleLabel = getEditorPane().getInspectorPanel().getView("TitleLabel", Label.class);
        titleLabel.setAlign(Pos.BOTTOM_CENTER);
        titleLabel.setPadding(0, 0, 0, 0);
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
        RMEditor editor = getEditor();
        Font font = RMEditorUtils.getFont(editor);

        // Update UndoButton, RedoButton
        Undoer undoer = editor.getUndoer();
        setViewEnabled("UndoButton", undoer != null && undoer.getLastUndoSet() != null);
        setViewEnabled("RedoButton", undoer != null && undoer.getLastRedoSet() != null);

        // Update MoneyButton, PercentButton, CommaButton
        TextFormat fmt = RMEditorUtils.getFormat(editor);
        RMNumberFormat nfmt = fmt instanceof RMNumberFormat ? (RMNumberFormat) fmt : null;
        setViewValue("MoneyButton", nfmt != null && nfmt.isLocalCurrencySymbolUsed());
        setViewValue("PercentButton", nfmt != null && nfmt.isPercentSymbolUsed());
        //setViewValue("CommaButton", nfmt!=null && nfmt.isGroupingUsed());

        // Reset PreviewEditButton state if out of sync
        if (getViewBoolValue("PreviewEditButton") == getEditorPane().isEditing())
            setViewValue("PreviewEditButton", !getEditorPane().isEditing());

        // Get selected tool button name and button - if found and not selected, select it
        String toolButtonName = editor.getCurrentTool().getClass().getSimpleName() + "Button";
        ToggleButton toolButton = getView(toolButtonName, ToggleButton.class);
        if (toolButton != null && !toolButton.isSelected())
            toolButton.setSelected(true);

        // Reset FontFaceComboBox, FontSizeComboBox
        _fontFaceComboBox.setSelItem(font.getFamily());
        String fontSizeStr = _fontSizeComboBox.getTextForItem(font.getSize());
        _fontSizeComboBox.setText(fontSizeStr);

        // Reset BoldButton, ItalicButton, UnderlineButton
        setViewValue("BoldButton", font.isBold());
        setViewEnabled("BoldButton", font.getBold() != null);
        //setViewValue("ItalicButton", font.isItalic());
        //setViewEnabled("ItalicButton", font.getItalic()!=null);
        //setViewValue("UnderlineButton", RMEditorUtils.isUnderlined(editor));

        // Update AlignLeftButton, AlignCenterButton, AlignRightButton, AlignFullButton, AlignTopButton, AlignMiddleButton
        //RMTypes.AlignX alignX = RMEditorUtils.getAlignmentX(editor);
        //setViewValue("AlignLeftButton", alignX==RMTypes.AlignX.Left);
        //setViewValue("AlignCenterButton", alignX==RMTypes.AlignX.Center);
        //setViewValue("AlignRightButton", alignX==RMTypes.AlignX.Right);
        //setViewValue("AlignFullButton", alignX==RMTypes.AlignX.Full);

        // Update ColorWell
        RMEditorStyler editorStyler = editor.getStyler();
        Color color = editorStyler.getFillColor();
        _colorWell.setColor(color);
    }

    /**
     * Responds to UI panel control changes.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Get the editor
        RMEditorPane epane = getEditorPane();
        RMEditor editor = getEditor();
        RMEditorStyler editorStyler = editor.getStyler();

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
        if (anEvent.equals("FillColorButton")) {
            Color color = anEvent.getView(ColorButton.class).getColor();
            editorStyler.setFillColor(color);
        }
        if (anEvent.equals("StrokeColorButton")) {
            Color color = anEvent.getView(ColorButton.class).getColor();
            editorStyler.setBorderStrokeColor(color);
        }
        if (anEvent.equals("TextColorButton")) {
            Color color = anEvent.getView(ColorButton.class).getColor();
            editorStyler.setTextColor(color);
        }

        // Handle MoneyButton: If currently selected format is number format, add or remove dollars
        if (anEvent.equals("MoneyButton")) {
            if (RMEditorUtils.getFormat(editor) instanceof RMNumberFormat numFormat) {
                numFormat = numFormat.clone(); // Clone it
                numFormat.setLocalCurrencySymbolUsed(!numFormat.isLocalCurrencySymbolUsed()); // Toggle whether $ is used
                RMEditorUtils.setFormat(editor, numFormat);
            }
            else RMEditorUtils.setFormat(editor, RMNumberFormat.CURRENCY);
        }

        // Handle PercentButton: If currently selected format is number format, add or remove percent symbol
        if (anEvent.equals("PercentButton")) {
            if (RMEditorUtils.getFormat(editor) instanceof RMNumberFormat numFormat) {
                numFormat = numFormat.clone(); // Clone it
                numFormat.setPercentSymbolUsed(!numFormat.isPercentSymbolUsed()); // Toggle whether percent symbol is used
                RMEditorUtils.setFormat(editor, numFormat);
            }
            else RMEditorUtils.setFormat(editor, new RMNumberFormat("#,##0.00 %"));
        }

        // Handle CommaButton: If currently selected format is number format, add or remove grouping
        if (anEvent.equals("CommaButton")) {
            if (RMEditorUtils.getFormat(editor) instanceof RMNumberFormat numFormat) {
                numFormat = numFormat.clone();
                numFormat.setGroupingUsed(!numFormat.isGroupingUsed()); // Toggle whether grouping is used
                RMEditorUtils.setFormat(editor, numFormat);
            }
            else RMEditorUtils.setFormat(editor, new RMNumberFormat("#,##0.00"));
        }

        // Handle DecimalAddButton: If currently selected format is number format, add decimal
        if (anEvent.equals("DecimalAddButton")) {
            if (RMEditorUtils.getFormat(editor) instanceof RMNumberFormat numFormat) {
                numFormat = numFormat.clone();
                numFormat.setMinimumFractionDigits(numFormat.getMinimumFractionDigits() + 1);
                numFormat.setMaximumFractionDigits(numFormat.getMinimumFractionDigits());
                RMEditorUtils.setFormat(editor, numFormat);
            }
        }

        // Handle DecimalRemoveButton: If currently selected format is number format, remove decimal digits
        if (anEvent.equals("DecimalRemoveButton")) {
            if (RMEditorUtils.getFormat(editor) instanceof RMNumberFormat numFormat) {
                numFormat = numFormat.clone();
                numFormat.setMinimumFractionDigits(numFormat.getMinimumFractionDigits() - 1);
                numFormat.setMaximumFractionDigits(numFormat.getMinimumFractionDigits());
                RMEditorUtils.setFormat(editor, numFormat);
            }
        }

        // Handle SamplesButton
        if (anEvent.equals("SamplesButton"))
            epane.showSamples();

        // Handle Preview/Edit button and PreviewMenuItem
        if (anEvent.equals("PreviewEditButton") || anEvent.equals("PreviewMenuItem")) {
            if (anEvent.isAltDown()) // Open as text file
                openDocTextFile();
            else getEditorPane().setEditing(!getEditorPane().isEditing());
        }

        // Handle PreviewXMLMenuItem
        if (anEvent.equals("PreviewXMLMenuItem"))
            RMEditorPaneUtils.previewXML(getEditorPane());

        // Handle ToolButton(s)
        if (anEvent.getName().endsWith("ToolButton")) {
            RMTool<?> matchingTool = ListUtils.findMatch(_toolBarTools, tool -> anEvent.getName().startsWith(tool.getClass().getSimpleName()));
            if (matchingTool != null)
                getEditor().setCurrentTool(matchingTool);
        }

        // Handle FontFaceComboBox
        if (anEvent.equals("FontFaceComboBox")) {
            String familyName = anEvent.getText();
            String[] fontNames = Font.getFontNames(familyName);
            if (fontNames == null || fontNames.length == 0) return;
            String fontName = fontNames[0];
            Font font = Font.getFont(fontName, 12);
            RMEditorUtils.setFontFamily(editor, font);
            editor.requestFocus();
        }

        // Handle FontSizeComboBox
        if (anEvent.equals("FontSizeComboBox")) {
            RMEditorUtils.setFontSize(editor, anEvent.getFloatValue(), false);
            editor.requestFocus();
        }

        // Handle FontSizeUpButton, FontSizeDownButton
        if (anEvent.equals("FontSizeUpButton")) {
            Font font = RMEditorUtils.getFont(editor);
            RMEditorUtils.setFontSize(editor, font.getSize() < 16 ? 1 : 2, true);
        }
        if (anEvent.equals("FontSizeDownButton")) {
            Font font = RMEditorUtils.getFont(editor);
            RMEditorUtils.setFontSize(editor, font.getSize() < 16 ? -1 : -2, true);
        }

        // Handle BoldButton, ItalicButton, UnderlineButton
        if (anEvent.equals("BoldButton")) RMEditorUtils.setFontBold(editor, anEvent.getBoolValue());
        if (anEvent.equals("ItalicButton")) RMEditorUtils.setFontItalic(editor, anEvent.getBoolValue());
        if (anEvent.equals("UnderlineButton")) RMEditorUtils.setUnderlined(editor);

        // Handle AlignLeftButton, AlignCenterButton, AlignRightButton, AlignFullButton
        if (anEvent.equals("AlignLeftButton")) RMEditorUtils.setAlignmentX(editor, RMTypes.AlignX.Left);
        if (anEvent.equals("AlignCenterButton")) RMEditorUtils.setAlignmentX(editor, RMTypes.AlignX.Center);
        if (anEvent.equals("AlignRightButton")) RMEditorUtils.setAlignmentX(editor, RMTypes.AlignX.Right);
        if (anEvent.equals("AlignFullButton")) RMEditorUtils.setAlignmentX(editor, RMTypes.AlignX.Full);

        // Handle AddTableButton, AddGraphButton, AddLabelsButton, AddCrossTabFrameButton
        if (anEvent.equals("AddTableButton")) RMTableTool.addTable(getEditor(), null);
        if (anEvent.equals("AddGraphButton")) RMGraphTool.addGraph(getEditor(), null);
        if (anEvent.equals("AddLabelsButton")) RMLabelsTool.addLabels(getEditor(), null);
        if (anEvent.equals("AddCrossTabFrameButton")) RMCrossTabTool.addCrossTab(getEditor(), null);

        // Handle AddCrossTabButton, AddImagePlaceHolderMenuItem, AddSubreportMenuItem
        if (anEvent.equals("AddCrossTabButton")) RMCrossTabTool.addCrossTab(getEditor());
        if (anEvent.equals("AddImagePlaceHolderMenuItem")) RMEditorUtils.addImagePlaceholder(getEditor());
        if (anEvent.equals("AddSubreportMenuItem")) RMEditorUtils.addSubreport(getEditor());

        // Handle ConnectToDataSourceMenuItem
        if (anEvent.equals("ConnectToDataSourceMenuItem") || anEvent.equals("ConnectToDataSourceButton"))
            RMEditorPaneUtils.connectToDataSource(getEditorPane());

        // Handle ColorWell
        if (anEvent.equals("ColorWell")) {
            editorStyler.setFillColor(_colorWell.getColor());
        }
    }

    /**
     * Opens the editor document as a text file.
     */
    private void openDocTextFile()
    {
        // Get filename for doc (if not set, write doc to temp file)
        String fname = getEditor().getDoc().getFilename();
        if (fname == null) {
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
        View samplesButton = getView("SamplesButton");
        samplesButton.setScale(1.2);

        // Configure anim
        ViewAnim anim = samplesButton.getAnim(0);
        anim.getAnim(400).setScale(1.4).getAnim(800).setScale(1.2).getAnim(1200).setScale(1.4).getAnim(1600).setScale(1.2)
                .getAnim(2400).setRotate(360);
        anim.setLoopCount(3).play();
    }

    /**
     * Stops SampleButton animation.
     */
    public void stopSamplesButtonAnim()
    {
        View samplesButton = getView("SamplesButton");
        samplesButton.getAnim(0).finish();
    }

    /**
     * Creates the list of tool instances for tool bar.
     */
    private List<RMTool<?>> createToolBarTools()
    {
        RMEditor editor = getEditor();
        return List.of(editor.getSelectTool(),
            editor.getTool(RMLineShape.class),
            editor.getTool(RMRectShape.class),
            editor.getTool(RMOvalShape.class),
            editor.getTool(RMTextShape.class),
            editor.getTool(RMPolygonShape.class),
            new RMPolygonShapeTool.PencilTool(editor));
    }
}