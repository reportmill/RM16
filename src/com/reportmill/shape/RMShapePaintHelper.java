/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;

/**
 * A helper class to provide selection information about shapes during painting.
 */
public class RMShapePaintHelper {

    // The current paint helper
    private static RMShapePaintHelper _paintProps;

    // Shared helper for normal paint case
    private static RMShapePaintHelper DEFAULT_PROPS = new RMShapePaintHelper();

    /**
     * Returns whether painting is for editor.
     */
    public boolean isEditing()  { return false; }

    /**
     * Returns whether given shape is selected.
     */
    public boolean isSelected(RMShape aShape)  { return false; }

    /**
     * Returns whether given shape is super selected.
     */
    public boolean isSuperSelected(RMShape aShape)  { return false; }

    /**
     * Returns whether given shape is THE super selected shape.
     */
    public boolean isSuperSelectedShape(RMShape aShape)  { return false; }

    /**
     * Returns the props for current painting.
     */
    public static RMShapePaintHelper get()  { return _paintProps != null ? _paintProps : DEFAULT_PROPS; }

    /**
     * Sets the props for current painting.
     */
    public static void set(RMShapePaintHelper theProps)  { _paintProps = theProps; }
}