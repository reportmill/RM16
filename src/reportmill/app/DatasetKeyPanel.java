/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.app;
import snap.view.View;
import snap.view.ViewEvent;
import snap.view.ViewOwner;
import snap.viewx.DialogBox;

/**
 * Runs a simple panel letting the user choose a dataset key element, like table, graph, crosstab or labels.
 */
public class DatasetKeyPanel extends ViewOwner {
    
    // The selected dataset key element type
    private byte _selType = TABLE;
    
    // The DialogBox
    private DialogBox   _dbox = new DialogBox("Dataset Key Element");

    // Constants for Dataset Element Types
    public static final byte TABLE = 1;
    public static final byte LABELS = 2;
    public static final byte GRAPH = 3;
    public static final byte CROSSTAB = 4;

    /**
     * Runs the dataset key panel.
     */
    public int showDatasetKeyPanel(View aView)
    {
        _dbox.setContent(getUI());
        return _dbox.showConfirmDialog(aView) ? _selType : 0;
    }

    /**
     * Initialize UI.
     */
    public void initUI()
    {
        // Configure buttons to accept clicks so we can watch for double click
        enableEvents("TableButton", MouseRelease);
        enableEvents("LabelsButton", MouseRelease);
        enableEvents("GraphButton", MouseRelease);
        enableEvents("CrossTabButton", MouseRelease);
    }

    /**
     * Handles input from UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle TableButton, LabelsButton, GraphButton, CrossTabButton
        if (anEvent.equals("TableButton")) _selType = TABLE;
        if (anEvent.equals("LabelsButton")) _selType = LABELS;
        if (anEvent.equals("GraphButton")) _selType = GRAPH;
        if (anEvent.equals("CrossTabButton")) _selType = CROSSTAB;

        // Handle any double-click
        if (anEvent.getClickCount()>1)
            _dbox.confirm();
    }
}