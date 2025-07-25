/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.base.ReportMill;
import snap.gfx.GFXEnv;
import snap.util.*;
import snap.view.ViewTheme;
import snap.view.ViewUtils;
import snap.view.WindowView;
import snap.viewx.DialogBox;
import snap.viewx.ExceptionReporter;
import snap.web.WebFile;
import snap.web.WebURL;
import java.awt.*;
import java.io.File;
import java.util.List;

/************************************* - All files should be 120 chars wide - *****************************************/

/**
 * This is the main class for the ReportMill app.
 */
public class App {

    // Whether app is in process of quiting
    private static boolean _quiting;

    /**
     * This is the static main method, called by Java when launching with com.reportmill.App.
     */
    public static void main(String[] args)
    {
        ViewUtils.runLater(() -> initAppWithArgs(args));
    }

    /**
     * Creates a new app instance.
     */
    public static void initAppWithArgs(String[] args)
    {
        // Set app is true
        ReportMill.isApp = true;

        // Set UI Theme
        ViewTheme.setThemeForName("Light");

        // Set default preferences
        Prefs prefs = Prefs.getPrefsForName("/com/reportmill");
        Prefs.setDefaultPrefs(prefs);

        // Install Exception reporter
        ExceptionReporter.setAppName("ReportMill");
        ExceptionReporter.setAppInfo("ReportMill Version " + ReportMill.getVersion() + ", Build Date: " + ReportMill.getBuildInfo());
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionReporter());

        // Run welcome panel
        boolean didOpenFile = openFilesFromArgs(args);
        if (!didOpenFile)
            WelcomePanel.getShared().showPanel();

        // Install OpenFiles Handler
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE))
            desktop.setOpenFileHandler(ofh -> ViewUtils.runLater(() -> openFiles(ofh.getFiles())));
        if (desktop.isSupported(Desktop.Action.APP_PREFERENCES))
            desktop.setPreferencesHandler(pe -> new PreferencesPanel().showPanel(null));
        if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER))
            desktop.setQuitHandler((qe,qr) -> { quitApp(); qr.cancelQuit(); });
    }

    /**
     * Quits the app (can be invoked by anyone).
     */
    public static void quitApp()
    {
        // Get open editor panes
        if (_quiting) return;
        _quiting = true;
        RMEditorPane[] editorPanes = WindowView.getOpenWindowOwners(RMEditorPane.class);

        // Iterate over open Editors to see if any have unsaved changes
        int answer = 0;
        for (int i = 0, iMax = editorPanes.length; i < iMax && iMax > 1; i++) {
            RMEditorPane editorPane = editorPanes[i];

            // Turn off editor preview
            editorPane.setEditing(true);

            // If editor has undos, run Review Unsaved panel and break
            if (editorPane.getEditor().undoerHasUndos()) {
                DialogBox dialogBox = new DialogBox("Review Unsaved Documents");
                dialogBox.setWarningMessage("There are unsaved documents");
                dialogBox.setOptions("Review Unsaved", "Quit Anyway", "Cancel");
                answer = dialogBox.showOptionDialog(editorPane.getEditor(), "Review Unsaved");
                break;
            }
        }

        // If user hit Cancel, just go away
        if (answer == 2) {
            _quiting = false;
            return;
        }

        // Disable welcome panel
        boolean old = WelcomePanel.getShared().isEnabled();
        WelcomePanel.getShared().setEnabled(false);

        // If Review Unsaved, iterate through _editors to see if they should be saved or if user wants to cancel instead
        if (answer == 0) {
            for (RMEditorPane editorPane : editorPanes)
                if (!editorPane.close()) {
                    WelcomePanel.getShared().setEnabled(old);
                    _quiting = false;
                    return;
                }
        }

        // Flush Properties to registry and exit
        try { Prefs.getDefaultPrefs().flush(); }
        catch (Exception e) { e.printStackTrace(); }
        GFXEnv.getEnv().exit(0);
    }

    /**
     * Opens files in given list.
     */
    private static void openFiles(List<File> theFiles)
    {
        for (File openFile : theFiles) {
            WebFile openFileSnap = WebFile.getFileForJavaFile(openFile);
            if (openFileSnap != null)
                WelcomePanel.getShared().openFile(openFileSnap);
        }
    }

    /**
     * Opens files in given args list.
     */
    private static boolean openFilesFromArgs(String[] args)
    {
        boolean didOpenFile = false;
        for (String arg : args) {
            WebURL openFileURL = WebURL.getUrl(arg);
            WebFile openFile = openFileURL != null ? openFileURL.getFile() : null;
            if (openFile != null) {
                WelcomePanel.getShared().openFile(openFile);
                didOpenFile = true;
            }
        }

        // Return when did open file
        return didOpenFile;
    }
}