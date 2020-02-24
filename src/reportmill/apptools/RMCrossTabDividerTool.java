/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.apptools;
import rmdraw.app.Tool;
import reportmill.shape.RMCrossTabDivider;
import snap.view.*;

/**
 * Provides ReportMill UI editing for CellDivider shape.
 */
public class RMCrossTabDividerTool <T extends RMCrossTabDivider> extends Tool<T> {

/**
 * Override to return empty panel.
 */
protected View createUI()  { return new Label("CrossTab Divider"); }

/**
 * Overrides tool method to indicate that cell dividers have no handles.
 */
public int getHandleCount(T aShape)  { return 0; }

}
