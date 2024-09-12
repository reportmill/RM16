/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import snap.props.PropChange;
import snap.util.Prefs;
import snap.view.*;
import snap.viewx.FilePanel;
import snap.web.*;

/**
 * An implementation of a panel to manage/open user Snap sites (projects).
 */
public class WelcomePanel extends ViewOwner {

    // Whether welcome panel is enabled
    private boolean  _enabled = true;

    // The FilePanel
    private FilePanel  _filePanel;

    // The WelcomePanelAnim
    private WelcomePanelAnim _welcomePanelAnim;

    // The shared instance
    private static WelcomePanel _shared;

    // Constants
    public static final String RM_FILE_EXT = "rpt";
    public static final String PDF_FILE_EXT = "pdf";

    /**
     * Constructor.
     */
    protected WelcomePanel()
    {
        super();
        _shared = this;
        _welcomePanelAnim = new WelcomePanelAnim();
    }

    /**
     * Returns the shared instance.
     */
    public static WelcomePanel getShared()
    {
        if (_shared != null) return _shared;
        return _shared = new WelcomePanel();
    }

    /**
     * Returns whether welcome panel is enabled.
     */
    public boolean isEnabled()  { return _enabled; }

    /**
     * Sets whether welcome panel is enabled.
     */
    public void setEnabled(boolean aValue)  { _enabled = aValue; }

    /**
     * Shows the welcome panel.
     */
    public void showPanel()
    {
        getWindow().setVisible(true);
        resetLater();
    }

    /**
     * Hides the welcome panel.
     */
    public void hide()
    {
        // Hide window and flush prefs
        getWindow().setVisible(false);
        Prefs.getDefaultPrefs().flush();
    }

    /**
     * Initialize UI panel.
     */
    protected void initUI()
    {
        // Add WelcomePaneAnim view
        View animView = _welcomePanelAnim.getUI();
        getUI(ChildView.class).addChild(animView, 0);
        animView.playAnimDeep();

        // Create FilePanel
        _filePanel = createFilePanel();
        View filePanelUI = _filePanel.getUI();
        filePanelUI.setGrowHeight(true);

        // Add FilePanel.UI to ColView
        ColView topColView = (ColView) getUI();
        ColView colView2 = (ColView) topColView.getChild(1);
        colView2.addChild(filePanelUI, 1);

        // Hide ProgressBar
        getView("ProgressBar").setVisible(false);

        // Make OpenButton default
        getView("OpenButton", Button.class).setDefaultButton(true);
    }

    /**
     * Override to initialize window.
     */
    @Override
    protected void initWindow(WindowView aWindow)
    {
        // Add WindowListener to indicate app should exit when close button clicked
        WindowView window = getWindow();
        window.setTitle("Welcome");
        window.addEventHandler(e -> hide(), WinClose);
    }

    /**
     * Responds to UI changes.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle SamplesButton
        if (anEvent.equals("SamplesButton"))
            newFile(true);

        // Handle NewButton
        if (anEvent.equals("NewButton"))
            newFile(false);

        // Handle OpenButton
        if (anEvent.equals("OpenButton")) {
            WebFile selFile = _filePanel.getSelFileAndAddToRecentFiles();
            openFile(selFile);
        }

        // Handle QuitButton
        if (anEvent.equals("QuitButton"))
            App.quitApp();
    }

    /**
     * Creates a new file.
     */
    protected void newFile(boolean showSamples)
    {
        // Get new editor pane
        RMEditorPane editorPane = new RMEditorPane().newDocument();

        // Make editor window visible
        editorPane.setWindowVisible(true);

        // Show Samples or start SamplesButtonAnim
        if (showSamples) //editorPane = RMEditorPaneUtils.openSample("Movies");
            runDelayed(() -> editorPane.showSamples(), 300);
        else runLater(() -> editorPane.getTopToolBar().startSamplesButtonAnim());

        // Hide WelcomePanel
        hide();
    }

    /**
     * Opens a document for given file.
     */
    public void openFile(WebFile aFile)
    {
        // Get the new editor pane that will open the document
        RMEditorPane editorPane = new RMEditorPane().openSource(aFile);
        if (editorPane == null)
            return;

        // Make editor window visible
        editorPane.setWindowVisible(true);

        // Hide WelcomePanel
        hide();
    }

    /**
     * Creates the FilePanel to be added to WelcomePanel.
     */
    private FilePanel createFilePanel()
    {
        // Add recent files
        WebSite recentFilesSite = RecentFilesSite.getShared();
        FilePanel.addDefaultSite(recentFilesSite);

        // Create/config FilePanel
        FilePanel filePanel = new FilePanel();
        String[] EXTENSIONS = { RM_FILE_EXT, PDF_FILE_EXT };
        filePanel.setTypes(EXTENSIONS);
        filePanel.setSelSite(recentFilesSite);
        filePanel.setActionHandler(e -> WelcomePanel.this.fireActionEventForObject("OpenButton", e));

        // Add PropChangeListener
        filePanel.addPropChangeListener(pc -> filePanelDidPropChange(pc));

        // Return
        return filePanel;
    }

    /**
     * Called when FilePanel does prop change.
     */
    private void filePanelDidPropChange(PropChange aPC)
    {
        String propName = aPC.getPropName();

        // Handle SelSite change:
        if (propName.equals(FilePanel.SelSite_Prop)) {
            WebSite selSite = _filePanel.getSelSite();;
            boolean minimize = !(selSite instanceof RecentFilesSite);
            _welcomePanelAnim.setMinimized(minimize);
        }

        // Handle SelFile change: Update OpenButton.Enabled
        else if (propName.equals(FilePanel.SelFile_Prop)) {
            boolean isOpenFileSet = _filePanel.getSelFile() != null;
            getView("OpenButton").setEnabled(isOpenFileSet);
        }
    }
}