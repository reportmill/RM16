/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.apptools;
import reportmill.shape.RMLabel;
import rmdraw.app.Tool;
import snap.view.*;

/**
 * Tool for editing a Label.
 */
public class RMLabelTool <T extends RMLabel> extends Tool<T> {

    /**
     * Override.
     */
    public Class getShapeClass()  { return RMLabel.class; }

    /**
     * Override.
     */
    public String getWindowTitle()  { return "Label Inspector"; }

    /**
     * Override.
     */
    public View createUI()  { return new Label(); }

    /**
     * Overrides to declare labels have no handles.
     */
    public int getHandleCount(T aShape)  { return 0; }
}