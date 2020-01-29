/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.app;
import rmdraw.app.*;
import snap.view.*;

/**
 * This class manages the attributes panel which holds the color panel, font panel, formatter panel and keys panel.
 */
public class RMAttributesPanel extends AttributesPanel {

/**
 * Creates new AttributesPanel for EditorPane.
 */
public RMAttributesPanel(EditorPane anEP)  { super(anEP); }

/**
 * Creates the inspector names array.
 */
public String[] createInspectorNames()  { return new String[] { KEYS, COLOR, FONT, FORMAT }; }

/**
 * Creates the inspectors array.
 */
public ViewOwner[] createInspectors()
{
    KeysPanel keys = new KeysPanel(getEditorPane());
    APColorPanel color = new APColorPanel();
    FontPanel font = new FontPanel(getEditorPane());
    FormatPanel format = new FormatPanel(getEditorPane());
    return new ViewOwner[] { keys, color, font, format };
}

}