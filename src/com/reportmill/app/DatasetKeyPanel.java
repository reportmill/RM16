/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import snap.view.*;
import snap.viewx.DialogBox;

/**
 * Runs a simple panel letting the user choose a dataset key element, like table, graph, crosstab or labels.
 */
public class DatasetKeyPanel extends ViewOwner {

    // The selected dataset key element type
    private byte _selectedType = TABLE;

    // The DialogBox
    private DialogBox _dialogBox = new DialogBox("Dataset Key Element");

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
        _dialogBox.setContent(getUI());
        return _dialogBox.showConfirmDialog(aView) ? _selectedType : 0;
    }

    /**
     * Initialize UI.
     */
    @Override
    protected void initUI()
    {
        // Configure buttons to accept clicks so we can watch for double click
        getView("TableButton").addEventHandler(this::handleButtonMouseRelease, MouseRelease);
        getView("LabelsButton").addEventHandler(this::handleButtonMouseRelease, MouseRelease);
        getView("GraphButton").addEventHandler(this::handleButtonMouseRelease, MouseRelease);
        getView("CrossTabButton").addEventHandler(this::handleButtonMouseRelease, MouseRelease);
    }

    /**
     * Handles input from UI controls.
     */
    private void handleButtonMouseRelease(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle TableButton, LabelsButton, GraphButton, CrossTabButton
            case "TableButton": _selectedType = TABLE; break;
            case "LabelsButton": _selectedType = LABELS; break;
            case "GraphButton": _selectedType = GRAPH; break;
            case "CrossTabButton": _selectedType = CROSSTAB; break;
        }

        // Handle any double-click
        if (anEvent.getClickCount() > 1)
            _dialogBox.confirm();
    }
}