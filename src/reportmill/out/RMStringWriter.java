/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.out;
import reportmill.shape.RMCrossTab;
import reportmill.shape.RMCrossTabCell;
import reportmill.shape.RMCrossTabRow;
import reportmill.shape.RMTableRowRPG;
import rmdraw.scene.*;
import snap.text.RichText;

import java.util.*;

/**
 * This class is used to write a String representation of an RMDoc.
 */
public class RMStringWriter {

/**
 * Returns a byte array holding an ASCII representation of a given document.
 */
public static byte[] delimitedAsciiBytes(SGDoc aDoc, String fieldDelim, String recordDelim, boolean quoteFields)
{
    // Generate delimited string
    String s = delimitedString(aDoc, fieldDelim, recordDelim, quoteFields);
    
    // Return bytes
    try { return s.getBytes("ISO-8859-1"); }
    catch(Exception e) { System.err.println(e); return null; }
}

/**
 * Returns a String holding the delimited data for a given document.
 */
public static String delimitedString(SGDoc aDoc, String fieldDelim, String recordDelim, boolean quoteFields)
{
    // Validate and resolve page references
    aDoc.layoutDeep();
    aDoc.resolvePageReferences();
    
    // Create new string buffer
    StringBuffer sb = new StringBuffer();

    // Append shapes
    for(int i=0, iMax=aDoc.getPages().size(); i<iMax; i++)
        appendDelimited(sb, aDoc.getPage(i), fieldDelim, recordDelim, quoteFields);

    // Return string
    return sb.toString();
}

/**
 * Appends a string representation of the given shape to the given string buffer.
 */
static void appendDelimited(StringBuffer aSB, SGView aShape, String fieldD, String recD, boolean quoteFields)
{
    // If table row, iterate over children (sorted by minX) and append their strings separated by fieldDelim
    if (aShape instanceof RMTableRowRPG && aShape.getChildCount()>0) {
        
        // Get sorted children
        List <SGView> children = SGViewUtils.getViewsSortedByFrameX(aShape.getChildren());

        // Iterate over children
        for (int i=0, iMax=children.size(); i<iMax; i++) { SGView child = children.get(i);

            // Handle text children
            if(child instanceof SGText) { SGText text = (SGText)child;
                RichText rtext = text.getRichText();
                if(quoteFields)
                    aSB.append('\"').append(rtext.getString()).append('\"').append(fieldD);
                else aSB.append(rtext.getString()).append(fieldD);
            }
        }
        
        // Trim last field delimiter and add record delimiter
        if (aSB.toString().endsWith(fieldD))
            aSB.delete(aSB.length()-fieldD.length(), aSB.length());
        aSB.append(recD);
    }
    
    // Handle RMCrossTab
    else if (aShape instanceof RMCrossTab) { RMCrossTab table = (RMCrossTab)aShape;
        
        // Iterate over rows
        for (int i=0, iMax=table.getRowCount(); i<iMax; i++) { RMCrossTabRow row = table.getRow(i);
            
            // Iterate over row cells and add cell string plus field delimiter
            for (int j=0, jMax=row.getCellCount(); j<jMax; j++) { RMCrossTabCell cell = row.getCell(j);
                RichText rtext = cell.getRichText();
                String str = rtext.getString();
                if (quoteFields)
                    aSB.append('\"').append(str).append('\"').append(fieldD);
                else aSB.append(str).append(fieldD);
            }
            
            // Trim last field delimiter and add record delimiter
            aSB.delete(aSB.length()-fieldD.length(), aSB.length());
            aSB.append(recD);
        }
    }

    // Otherwise descend into shape
    else for (int i=0, iMax=aShape.getChildCount(); i<iMax; i++)
        appendDelimited(aSB, aShape.getChild(i), fieldD, recD, quoteFields);
}

}