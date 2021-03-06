/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.app;
import reportmill.shape.RMDoc;
import reportmill.util.RMExtras;
import rmdraw.app.*;
import reportmill.util.RMDataSource;
import rmdraw.scene.RMArchiver;
import rmdraw.scene.SGDoc;
import snap.util.*;
import snap.view.ViewUtils;
import snap.viewx.DialogBox;
import snap.viewx.FilePanel;
import snap.web.WebURL;

import java.io.File;

/**
 * Some utility methods for RMEditorPane.
 */
public class RMEditorPaneUtils {

/**
 * Installs a sample data source.
 */
public static void connectToDataSource(RMEditorPane anEP)
{
    WebURL url = RMExtras.getHollywoodURL();
    RMDataSource ds = new RMDataSource(url);
    if(ds!=null) anEP.setDataSource(ds, 350, 0);
}

/**
 * Opens the named sample file from the examples package.
 */
public static RMEditorPane openSample(String aTitle)
{
    // If file is xml resource, get temp file, get XML bytes, write to file, open file and return null
    if(aTitle.endsWith(".xml")) {
        File file = FileUtils.getTempFile(FilePathUtils.getFileName(aTitle));
        byte bytes[] = SnapUtils.getBytes(aTitle);
        SnapUtils.writeBytes(bytes, file);
        FileUtils.openFile(file);
        return null;
    }
    
    // If not url, append Jar:/com/reportmill prefix
    if(!aTitle.startsWith("http:"))
        aTitle = "Jar:/reportmill/examples/" + aTitle + ".rpt";
        
    // Create new editor pane, open document and window, and return editor pane
    RMEditorPane editorPane = new RMEditorPane();
    editorPane.open(aTitle);
    editorPane.setWindowVisible(true);
    return editorPane;
}

/**
 * Preview PDF.
 */
public static void previewPDF(RMEditorPane anEP)
{
    // Get filename (if alt key is pressed, change to current doc plus .pdf)
    String filename = SnapUtils.getTempDir() + "RMPDFFile.pdf";
    if(ViewUtils.isAltDown() && anEP.getDoc().getFilename()!=null)
        filename = FilePathUtils.getSimple(anEP.getDoc().getFilename()) + ".pdf";
    
    // Get report, write report and open file
    RMDoc report = generateReport(anEP, true);
    report.writePDF(filename);
    FileUtils.openFile(filename);
}

/**
 * Generates report from editor.
 */
public static RMDoc generateReport(RMEditorPane anEP, boolean doPaginate)
{
    // Get editor (make sure it's in editing mode)
    RMEditor editor = anEP.getEditor();
    anEP.setEditing(true);
    
    // Get document and return report
    RMDoc document = anEP.getDoc();
    return document.generateReport(editor.getDataSourceDataset(), doPaginate);
}

/**
 * Generate report, save as HTML in temp file and open.
 */
public static void previewHTML(RMEditorPane anEP)
{
    SGDoc report = generateReport(anEP, !ViewUtils.isAltDown());
    report.write(SnapUtils.getTempDir() + "RMHTMLFile.html");
    FileUtils.openFile(SnapUtils.getTempDir() + "RMHTMLFile.html");
}

/**
 * Generate report, save as CSV in temp file and open.
 */
public static void previewCSV(RMEditorPane anEP)
{
    SGDoc report = generateReport(anEP, false);
    report.write(SnapUtils.getTempDir() + "RMCSVFile.csv");
    FileUtils.openFile(SnapUtils.getTempDir() + "RMCSVFile.csv");
}

/**
 * Generate report, save as JPG in temp file and open.
 */
public static void previewJPG(RMEditorPane anEP)
{
    SGDoc report = generateReport(anEP, false);
    report.write(SnapUtils.getTempDir() + "RMJPGFile.jpg");
    FileUtils.openFile(SnapUtils.getTempDir() + "RMJPGFile.jpg");
}

/**
 * Generate report, save as PNG in temp file and open.
 */
public static void previewPNG(RMEditorPane anEP)
{
    SGDoc report = generateReport(anEP, false);
    report.write(SnapUtils.getTempDir() + "RMPNGFile.png");
    FileUtils.openFile(SnapUtils.getTempDir() + "RMPNGFile.png");
}

/**
 * Preview XLS.
 */
public static void previewXLS(RMEditorPane anEP)
{
    // Get report, write report and open file (in handler, in case POI jar is missing)
    try {
        SGDoc report = generateReport(anEP, false);
        report.write(SnapUtils.getTempDir() + "RMXLSFile.xls");
        FileUtils.openFile(SnapUtils.getTempDir() + "RMXLSFile.xls");
    }
    
    // Catch exception - handle case where poi jar is missing    
    catch(Throwable t) {
        
        // print it out (in case it's something other than a missing jar)
        t.printStackTrace();
        
        // Run option dialog to ask user if they want to see Excel doc
        String msg = "ReportMill needs the OpenSource POI jar in order to generate Excel. Click Open to see " +
            "the support document on the subject.";
        DialogBox dbox = new DialogBox("Excel POI Jar Missing");
        dbox.setWarningMessage(StringUtils.wrap(msg, 50)); dbox.setOptions("Open", "Cancel");
        int answer = dbox.showOptionDialog(anEP.getEditor(), "Open");
        
        // If user answered "open", open poi doc url
        if(answer==0)
            URLUtils.openURL("https://reportmill.com/support/Excel.html");
    }
}

/**
 * Preview RTF.
 */
public static void previewRTF(RMEditorPane anEP)
{
    // Get report, write report and open file
    SGDoc report = generateReport(anEP, true);
    report.write(SnapUtils.getTempDir() + "RMRTFFile.rtf");
    FileUtils.openFile(SnapUtils.getTempDir() + "RMRTFFile.rtf");
}

/**
 * Preview XML.
 */
public static void previewXML(RMEditorPane anEP)
{
    Editor editor = anEP.getEditor();
    XMLElement xml = new RMArchiver().writeToXML(editor.getDoc());
    File file = FileUtils.getTempFile("RMXMLFile.xml");
    try { FileUtils.writeBytes(file, xml.getBytes()); }
    catch(Exception e) { throw new RuntimeException(e); }
    FileUtils.openFile(file);
}

/**
 * Save document as PDF to given path.
 */
public static void saveAsPDF(RMEditorPane anEP)
{
    RMEditor editor = anEP.getEditor();
    String path = FilePanel.showOpenPanel(editor, "PDF file (.pdf)", "pdf"); if(path==null) return;
    editor.getDoc().writePDF(path);
}

}