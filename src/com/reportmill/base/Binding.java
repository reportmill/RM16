/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import snap.util.*;

/**
 * This class maps a shape property value to a model key value.
 */
public class Binding implements XMLArchiver.Archivable {

    // The property name that is being bound
    private String _propName;

    // The key that is used to get the property value from the bound object
    private String _key;

    /**
     * Constructor.
     */
    public Binding()
    {
    }

    /**
     * Constructor for property name and key.
     */
    public Binding(String aPropName, String aKey)
    {
        _propName = aPropName;
        _key = aKey;
    }

    /**
     * Returns the property name.
     */
    public String getPropName()  { return _propName; }

    /**
     * Returns the key that is used to get the property value from the bound object.
     */
    public String getKey()  { return _key; }

    /**
     * XML archival.
     */
    public XMLElement toXML(XMLArchiver anArchiver)
    {
        XMLElement e = new XMLElement("binding");
        e.add("aspect", getPropName());
        if (getKey() != null && !getKey().isEmpty()) e.add("key", getKey());
        return e;
    }

    /**
     * XML unarchival.
     */
    public Binding fromXML(XMLArchiver anArchiver, XMLElement anElement)
    {
        _propName = anElement.getAttributeValue("aspect");
        _key = anElement.getAttributeValue("key");
        return this;
    }

    /**
     * Standard toString implementation.
     */
    public String toString()  { return "Binding: " + getPropName() + " = " + getKey(); }
}