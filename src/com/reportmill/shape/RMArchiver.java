/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.*;
import com.reportmill.graphics.*;
import java.util.*;
import snap.util.*;
import snap.web.WebURL;

/**
 * This class handles RM document archival.
 */
public class RMArchiver extends XMLArchiver {

    /**
     * Reads a document for given .rpt file source.
     */
    public RMDocument readDocumentForRptSource(Object aSource, RMDocument aBaseDoc)
    {
        // If source is a document, just return it
        if (aSource instanceof RMDocument) return (RMDocument) aSource;

        // Get URL and/or bytes (complain if not found)
        WebURL url = WebURL.getUrl(aSource);
        byte[] bytes = url != null ? url.getBytes() : SnapUtils.getBytes(aSource);
        if (bytes == null)
            throw new RuntimeException("RMArchiver.getDoc: Cannot read source: " + (url != null ? url : aSource));

        // If PDF, return PDF Doc
        if (RMPDFData.canRead(bytes))
            return RMPDFShape.getDocPDF(url != null ? url : bytes, aBaseDoc);

        // Create archiver, read, set source and return
        setRootObject(aBaseDoc);

        RMDocument doc;
        if (url != null)
            doc = (RMDocument) readObjectFromXmlUrl(url);
        else doc = (RMDocument) readObjectFromXmlBytes(bytes);

        // Set Source URL and return
        doc.setSourceURL(getSourceURL());
        return doc;
    }

    /**
     * Creates the class map.
     */
    protected Map<String, Class<?>> createClassMap()
    {
        // Create class map and add classes
        Map<String,Class<?>> classMap = new HashMap<>();

        // Shape classes
        classMap.put("arrow-head", RMLineShape.ArrowHead.class);
        classMap.put("cell-table", RMCrossTab.class);
        classMap.put("cell-table-frame", RMCrossTabFrame.class);
        classMap.put("document", RMDocument.class);
        classMap.put("flow-shape", RMParentShape.class);
        classMap.put("graph", RMGraph.class);
        classMap.put("graph-legend", RMGraphLegend.class);
        classMap.put("image-shape", RMImageShape.class);
        classMap.put("label", RMLabel.class);
        classMap.put("labels", RMLabels.class);
        classMap.put("line", RMLineShape.class);
        classMap.put("oval", RMOvalShape.class);
        classMap.put("page", RMPage.class);
        classMap.put("polygon", RMPolygonShape.class);
        classMap.put("rect", RMRectShape.class);
        classMap.put("shape", RMParentShape.class);
        classMap.put("spring-shape", RMSpringShape.class);
        classMap.put("subreport", RMSubreport.class);
        classMap.put("switchshape", RMSwitchShape.class);
        classMap.put("table", RMTable.class);
        classMap.put("table-group", RMTableGroup.class);
        classMap.put("tablerow", RMTableRow.class);
        classMap.put("text", RMTextShape.class);
        classMap.put("linked-text", RMLinkedText.class);
        classMap.put("scene3d", RMScene3D.class);
        classMap.put("ViewShape", ViewShape.class);

        // Graphics
        classMap.put("color", RMColor.class);
        classMap.put("font", RMFont.class);
        classMap.put("format", RMArchiverHpr.RMFormatStub.class);
        classMap.put("pgraph", RMParagraph.class);
        classMap.put("xstring", RMXString.class);

        // Strokes
        classMap.put("stroke", RMStroke.class);
        classMap.put("double-stroke", RMStroke.class);
        classMap.put("border-stroke", com.reportmill.graphics.RMBorderStroke.class);

        // Fills
        classMap.put("fill", RMFill.class);
        classMap.put("gradient-fill", RMGradientFill.class);
        classMap.put("radial-fill", RMGradientFill.class);
        classMap.put("image-fill", RMImageFill.class);

        // Sorts, Grouping
        classMap.put("sort", com.reportmill.base.RMSort.class);
        classMap.put("top-n-sort", com.reportmill.base.RMTopNSort.class);
        classMap.put("value-sort", com.reportmill.base.RMValueSort.class);
        classMap.put("grouper", RMGrouper.class);
        classMap.put("grouping", RMGrouping.class);

        // Return classmap
        return classMap;
    }
}