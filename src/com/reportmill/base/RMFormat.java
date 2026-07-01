/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import snap.text.TextFormat;
import snap.util.XMLArchiver;

/**
 * An interface for RM format classes (they all should get/set format strings, format objects and archive XML).
 */
public interface RMFormat extends TextFormat, XMLArchiver.Archivable {


}