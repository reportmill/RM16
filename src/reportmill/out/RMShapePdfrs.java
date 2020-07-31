/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.out;
import reportmill.shape.RMPDFData;
import reportmill.shape.RMPDFShape;
import rmdraw.gfx3d.*;
import rmdraw.scene.*;
import java.util.*;

import snap.geom.Insets;
import snap.geom.Rect;
import snap.geom.Shape;
import snap.gfx.*;
import snappdf.*;
import snappdf.write.*;

/**
 * A class to hold RMShapePdfr subclasses for RMPage, RMTextShape, RMImageShape, RMScene3D.
 */
public class RMShapePdfrs {
    
    // Shared instances of RMShapePdfr subclasses
    static RMTextShapePdfr     _textShapePdfr = new RMTextShapePdfr();
    static RMImageShapePdfr    _imgShapePdfr = new RMImageShapePdfr();
    static RMPagePdfr          _pageShapePdfr = new RMPagePdfr();
    static RMScene3DPdfr       _scene3DPdfr = new RMScene3DPdfr();

    /**
     * This class generates PDF for an RMText.
     */
    public static class RMTextShapePdfr <T extends SGText> extends RMShapePdfr <T> {

        /** Writes a given RMShape hierarchy to a PDF file (recursively). */
        protected void writeShape(T aTextShape, RMPDFWriter aWriter)
        {
            // Do normal version
            super.writeShape(aTextShape, aWriter);

            // If not editable, just write out text and return
            if (!aTextShape.isEditable()) {
                PDFWriterText.writeText(aWriter, aTextShape.getTextBox());
                return;
            }

            // Writing PDF Widget Annotation: Get pdf page object and bump PDF version to 1.4
            PDFPageWriter pwriter = aWriter.getPageWriter();

            // Get TextShape info
            String name = aTextShape.getName();
            String pdfName = name!=null && name.length()>0 ? '(' + name + ')' : null;
            String text = aTextShape.getText();
            String pdfText = text!=null && text.length()>0 ? '(' + text + ')' : null;

            // Get ViewShape frame in PDF page coords (minus text insets)
            SGView page = aTextShape.getPage();
            Rect frame = aTextShape.localToParent(aTextShape.getBoundsLocal(), page).getBounds();
            frame.y = page.getHeight() - frame.getMaxY();
            Insets ins = aTextShape.getMargin();
            frame.x += ins.left; frame.y += ins.bottom;
            frame.width -= ins.getWidth(); frame.height -= ins.getHeight();

            // Create and add annotation to page
            PDFAnnotation widget = new PDFAnnotation.Widget(frame, "");
            pwriter.addAnnotation(widget);

            // Set Annotation Flags, Field-Type
            Map map = widget.getAnnotationMap();
            map.put("P", aWriter.getXRefTable().getRefString(pwriter));
            map.put("F", 4);
            map.put("FT", "/Tx"); // Makes widget printable textfield
            if (aTextShape.isMultiline())
                map.put("Ff", 1<<12);

            // Get font name and set in Default Appearance
            Font font = aTextShape.getFont();
            PDFFontEntry fontEntry = aWriter.getFontEntry(font, 0);
            String fontName = '/' + fontEntry.getPDFName();
            int fontSize = (int)font.getSize();
            map.put("DA", "(0 0 0 rg " + fontName + ' ' + fontSize + " Tf)");

            // Set Widget Name, alt name, value, default value and fonts dict
            if (pdfName!=null)
                map.put("T", pdfName); // Name
            if (pdfName!=null)
                map.put("TU", pdfName); // Alternate name (ToolTip)
            if (pdfText!=null) {
                map.put("V", pdfText);
                map.put("DV", pdfText);
            }

            // Set Widget Default Resources dict
            Object fonts = aWriter.getFonts();
            Object fontsXRef = aWriter.getXRefTable().getRefString(fonts);
            Map drDict = Collections.singletonMap("Font", fontsXRef);
            map.put("DR", drDict);
        }
    }

    /**
     * PDF writer for RMImageShape.
     */
    public static class RMImageShapePdfr <T extends SGImage> extends RMShapePdfr <T> {

        /** Override to write Image. */
        protected void writeShape(T anImageShape, RMPDFWriter aWriter)
        {
            // Do normal version
            super.writeShape(anImageShape, aWriter);

            // Get image data (just return if missing) and image name and add image
            Image img = anImageShape.getImage(); if (img==null) return;
            String iname = aWriter.getImageName(img);
            aWriter.addImage(img);

            // Get PDF page and gsave
            PDFPageWriter pdfPage = aWriter.getPageWriter();
            pdfPage.gsave();

            // Apply clip if needed
            if (anImageShape.getRadius()>.001) {
                Shape path = anImageShape.getPath();
                pdfPage.writePath(path);
                pdfPage.append("W n ");
            }

            // Get image bounds width and height
            Rect bounds = anImageShape.getImageBounds();
            double width = bounds.getWidth();
            double height = bounds.getHeight();

            // Apply CTM - image coords are flipped from page coords ( (0,0) at upper-left )
            pdfPage.writeTransform(width, 0, 0, -height, bounds.getX(), bounds.getMaxY());

            // Do image and grestore
            pdfPage.appendln("/" + iname + " Do");
            pdfPage.grestore();
        }
    }

    /**
     * PDF writer for RMPDFShape.
     */
    public static class RMPDFShapePdfr <T extends RMPDFShape> extends RMShapePdfr <T> {

        /** Override to write PDF XObject form do. */
        protected void writeShape(T aPDFShape, RMPDFWriter aWriter)
        {
            // Do normal version
            super.writeShape(aPDFShape, aWriter);

            // Get PDF data (just return if missing or invalid)
            RMPDFData pdata = aPDFShape.getPDFData(); if (pdata==null) return;

            // Get pdf page (just return if no page contents - which is apparently legal)
            PDFPage page = pdata.getPDFFile().getPage(pdata.getPageIndex()); if (page.getPageContentsStream()==null) return;
            String iname = String.valueOf(System.identityHashCode(page)); //aWriter.getImageName(pdata);

            // Add pdf data
            aWriter.addPDFData(pdata, iname);

            // Get PDF page, gsave and reset gstate defaults
            PDFPageWriter pdfPage = aWriter.getPageWriter();
            pdfPage.gsave();
            pdfPage.setLineCap(0);
            pdfPage.setLineJoin(0);

            // Apply clip if needed
            //if (aPDFShape.getRadius()>.1) { Shape p = aPDFShape.getPath(); pdfPage.writePath(p); pdfPage.append("W n "); }

            // Get image bounds width and height divided by PDF size (pdfImage writes out scale of imageBounds/imageSize)
            Rect bounds = aPDFShape.getImageBounds();
            double width = bounds.width/pdata.getWidth();
            double height = bounds.height/pdata.getHeight();

            // Apply CTM - image coords are flipped from page coords ( (0,0) at upper-left )
            pdfPage.writeTransform(width, 0, 0, -height, bounds.x, bounds.getMaxY());

            // Do image and grestore
            pdfPage.appendln("/" + iname + " Do");
            pdfPage.grestore();
        }
    }

    /**
     * This RMShapePdfr subclass writes PDF for RMPage.
     */
    public static class RMPagePdfr <T extends SGPage> extends RMShapePdfr <T> {

        /** Writes a given RMShape hierarchy to a PDF file (recursively). */
        protected void writeShapeBefore(T aPageShape, RMPDFWriter aWriter)
        {
            // Get pdf page
            PDFPageWriter pdfPage = aWriter.getPageWriter();

            // Write page header comment
            pdfPage.appendln("\n% ------ page " + (aPageShape.page() - 1) + " -----");

            // legacy defaults different from pdf defaults
            pdfPage.setLineCap(1);
            pdfPage.setLineJoin(1);

            // Flip coords to match java2d model
            pdfPage.append("1 0 0 -1 0 ").append(aPageShape.getHeight()).appendln(" cm");
        }

        /** Override to suppress grestore. */
        protected void writeShapeAfter(T aShape, RMPDFWriter aWriter)  { }
    }

    /**
     * This class generates PDF for an RMScene3D.
     */
    public static class RMScene3DPdfr <T extends SGScene3D> extends RMShapePdfr <T> {

        /** Writes a given RMShape hierarchy to a PDF file (recursively). */
        protected void writeShape(T aScene3D, RMPDFWriter aWriter)
        {
            // Do normal version
            super.writeShape(aScene3D, aWriter);

            // Write Path3Ds
            Camera camera = aScene3D.getCamera();
            List <Path3D> paths = camera.getPaths();
            for (Path3D path : paths) {
                writePath(aScene3D, path, aWriter);
                if (path.getLayers().size()>0)
                    for (Path3D layer : path.getLayers())
                        writePath(aScene3D, layer, aWriter);
            }
        }

        /** Writes a path. */
        protected void writePath(SGScene3D aScene3D, Path3D aPath, PDFWriter aWriter)
        {
            // Get path, fill and stroke
            Shape path = aPath.getPath();
            Color fillColor = aPath.getColor();
            Color strokeColor = aPath.getStrokeColor();
            PDFPageWriter pdfPage = aWriter.getPageWriter();

            // Get opacity and set if needed
            double op = aPath.getOpacity();
            if (op<1) {
                pdfPage.gsave();
                pdfPage.setOpacity(op*aScene3D.getOpacityDeep());
            }

            // Do fill and stroke
            if (fillColor!=null)
                SnapPaintPdfr.writeShapeFill(path, fillColor, aWriter);
            if (strokeColor!=null)
                SnapPaintPdfr.writeShapeStroke(path, aPath.getStroke(), strokeColor, aWriter);

            // Reset opacity if needed
            if (op<1) pdfPage.grestore();
        }
    }

    /**
     * Returns a shared RMPDFShapePdfr.
     */
    public static RMPDFShapePdfr getPDFShapePdfr()  { return _pspdfr!=null ? _pspdfr : (_pspdfr=new RMPDFShapePdfr()); }
    private static RMPDFShapePdfr _pspdfr;
}