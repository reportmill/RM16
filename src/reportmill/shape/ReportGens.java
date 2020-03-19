package reportmill.shape;
import reportmill.util.RMKeyChain;
import reportmill.util.RMTableOfContents;
import rmdraw.scene.*;
import snap.text.RichText;
import snap.geom.VPos;
import snap.util.ListUtils;
import snap.util.StringUtils;
import snap.web.WebURL;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to hold some base subclasses of ReportGen.
 */
public class ReportGens {

    /**
     * A ReportGen subclass for ImageCell.
     */
    public static class ImageCellRPG<T extends SGImage> extends ReportGen<T> {

        /**
         * Report generation method.
         */
        public SGView rpgShape(ReportOwner aRptOwner, SGView aParent)
        {
            // Do normal version
            SGImage clone = (SGImage) super.rpgShape(aRptOwner, aParent);

            // If key: Evaluate key for image and set
            T imageCell = getShape();
            String key = imageCell.getKey();
            if (key != null && key.length() > 0) {

                // Get key value
                Object value = RMKeyChain.getValue(aRptOwner, imageCell.getKey());
                if (value instanceof RMKeyChain)
                    value = ((RMKeyChain) value).getValue();
                WebURL url = WebURL.getURL(value);
                if (url != null) value = url;

                // If PDF data, return PDFShape
                if (url != null && url.getPath().toLowerCase().endsWith("pdf") ||
                        value instanceof byte[] && RMPDFData.canRead((byte[]) value))
                    return RMPDFShape.rpgShape(aRptOwner, aParent, imageCell, value);

                // Otherwise set new image
                clone.setImageForSource(value);
            }

            // This prevents RMImageShape from growing to actual image size in report
            // Probably need a GrowShapeToFit attribute to allow RPG image shape to grow
            clone.setPrefHeight(imageCell.getHeight() * imageCell.getScaleY());

            // Return clone
            return clone;
        }
    }

    /**
     * A ReportGen subclass for TextCell.
     */
    public static class TextCellRPG<T extends SGText> extends ReportGen<T> {

        /**
         * Report generation method.
         */
        public SGView rpgShape(ReportOwner anRptOwner, SGView aParent)
        {
            SGText textCell = getShape();
            SGText clone = textCell.clone();
            RichText rtext = clone.getRichText();

            // Do xstring RPG (if no change due to RPG, just use normal) with FirePropChangeEnabled turned off
            rtext.setPropChangeEnabled(false);
            anRptOwner.rpgCloneRichText(rtext, null, clone, false);

            // If coalesce newlines is set, coalesce newlines
            if (textCell.getCoalesceNewlines())
                coalesceNewlines(rtext);

            // Trim line ends from end of string to prevent extra empty line height
            int len = rtext.length(), end = len;
            while (end > 0 && StringUtils.isLineEndChar(rtext.charAt(end - 1))) end--;
            if (end != len)
                rtext.removeChars(end, len);

            // If WRAP_SCALE, set FitText ivar
            if (textCell.getWraps() == SGText.WRAP_SCALE)
                clone.setFitText(true);

            // Enable string FirePropChangeEnabled and revalidate
            rtext.setPropChangeEnabled(true);
            clone.revalidate();

            // If paginating, swap in paginated parts (disable in table row)
            if (textCell.getWraps() == SGText.WRAP_BASIC && !(textCell.getParent() instanceof RMTableRow)) {
                ReportOwner.ShapeList shapes = new ReportOwner.ShapeList();
                for (SGText text : paginate(clone))
                    shapes.addChild(text);
                return shapes;
            }

            // Return clone
            return clone;
        }

        /**
         * Paginates this text by creating linked texts to show all text and returns list of this text and linked texts.
         */
        protected List<SGText> paginate(SGText textCell)
        {
            // Create pages list with this text in it
            List<SGText> pages = new ArrayList();
            pages.add(textCell);

            // Cache vertical alignment and set to Top
            VPos alignY = textCell.getAlignY();
            textCell.setAlignY(VPos.TOP);

            // Get linked texts until all text visible
            SGText text = textCell;
            while (!text.isAllTextVisible()) {
                text = new SGLinkedText(text);
                pages.add(text);
            }

            // Restore alignment on last text and return list
            text.setAlignY(alignY);
            return pages;
        }

        /**
         * Re-does the RPG clone to resolve any @Page@ keys (assumed to be present in userInfo).
         */
        @Override
        protected void resolvePageReferences(ReportOwner aRptOwner, Object userInfo)
        {
            // Do normal shape resolve page references
            super.resolvePageReferences(aRptOwner, userInfo);

            // RPG clone RichText again and set
            SGText textCell = getShape();
            RichText rtext = textCell.getRichText();
            RichText clone = aRptOwner.rpgCloneRichText(rtext, userInfo, null, true);
            textCell.setRichText(clone);
        }
    }

    /**
     * Replaces any occurrence of consecutive newlines with a single newline.
     */
    private static void coalesceNewlines(RichText rtext)
    {
        // Iterate over occurrences of adjacent newlines (from back to font) and remove redundant newline chars
        String str = rtext.getString();
        for (int start = str.lastIndexOf("\n\n"); start >= 0; start = str.lastIndexOf("\n\n", start)) {
            int end = start + 1;
            while (start > 0 && str.charAt(start - 1) == '\n') start--;
            rtext.removeChars(start, end);
            str = rtext.getString();
        }

        // Also remove leading newline if present
        if (rtext.length() > 0 && rtext.charAt(0) == '\n')
            rtext.removeChars(0, 1);
    }

    /**
     * A ReportGen subclass for ParentCell.
     */
    public static class ParentCellRPG<T extends SGParent> extends ReportGen<T> {

        /**
         * Generate report with report owner.
         */
        public SGView rpgAll(ReportOwner anRptOwner, SGView aParent)
        {
            // Do normal version
            SGParent clone = (SGParent) super.rpgAll(anRptOwner, aParent);

            // Call rpgChildren and return
            T cell = getShape();
            clone = (SGParent) ReportGen.rpgChildrenFor(cell, anRptOwner, clone);
            return clone;
        }

        /**
         * Generate report with report owner.
         */
        protected SGView rpgChildren(ReportOwner anRptOwner, SGParent aParent)
        {
            SGParent parentCell = getShape();
            SGParent parent = aParent;
            ReportOwner.ShapeList slists[] = null;
            for (int i = 0, iMax = parentCell.getChildCount(); i < iMax; i++) {
                SGView child = parentCell.getChild(i);
                SGView crpg = anRptOwner.rpg(child, aParent);
                if (crpg instanceof ReportOwner.ShapeList) {
                    if (slists == null) slists = new ReportOwner.ShapeList[iMax];
                    slists[i] = (ReportOwner.ShapeList) crpg;
                    aParent.addChild(crpg.getChild(0));
                } else aParent.addChild(crpg);
            }

            // If ShapesList child was encountered, create a ShapesList for this shape
            if (slists != null) {
                int iMax = 0;
                for (ReportOwner.ShapeList slist : slists)
                    if (slist != null)
                        iMax = Math.max(iMax, slist.getChildCount());
                parent = new ReportOwner.ShapeList();
                parent.addChild(aParent);
                for (int i = 1; i < iMax; i++) {
                    SGParent page = parentCell.clone();
                    parent.addChild(page);
                    for (int j = 0; j < slists.length; j++) {
                        ReportOwner.ShapeList slist = slists[j];
                        if (slist == null) {
                            SGView ch = aParent.getChild(j);
                            SGView clone = ch.cloneDeep();
                            page.addChild(clone);
                            if (ListUtils.containsId(anRptOwner.getPageReferenceShapes(), ch))
                                anRptOwner.addPageReferenceShape(clone);
                        } else if (i < slist.getChildCount())
                            page.addChild(slist.getChild(i));
                    }
                }
            }

            // Return parent
            return parent;
        }
    }

    /**
     * A ReportGen subclass for PageCell.
     */
    public static class PageCellRPG<T extends SGPage> extends ParentCellRPG<T> {

        /**
         * Returns a report page.
         */
        public SGView rpgAll(ReportOwner anRptOwner, SGView aParent)
        {
            // Get page objects - if none, do normal version and return
            SGPage pageCell = getShape();
            String datasetKey = pageCell.getDatasetKey();
            List objects = datasetKey != null ? anRptOwner.getKeyChainListValue(datasetKey) : null;
            if (objects == null)
                return super.rpgAll(anRptOwner, aParent);

            // Create parts list
            ReportOwner.ShapeList pagesShape = new ReportOwner.ShapeList();

            // Generate parts reports
            for (int i = 0, iMax = objects.size(); i < iMax; i++) {
                Object obj = objects.get(i);
                anRptOwner.pushDataStack(obj);
                SGParent prpg = (SGParent) super.rpgAll(anRptOwner, aParent);
                anRptOwner.popDataStack();
                if (prpg instanceof ReportOwner.ShapeList)
                    for (SGView c : prpg.getChildArray())
                        pagesShape.addChild(c);
                else pagesShape.addChild(prpg);
            }

            // Return pages
            return pagesShape;
        }

        /**
         * Override to handle pagination.
         */
        protected SGView rpgChildren(ReportOwner anRptOwner, SGParent aParent)
        {
            // If paginating, just do normal version
            if (anRptOwner.getPaginate())
                return super.rpgChildren(anRptOwner, aParent);

            // Otherwise, generate rpg children to RMSpringShape
            SGSpringsView springShape = new SGSpringsView();
            springShape.setSize(aParent.getWidth(), aParent.getHeight());
            SGView page = super.rpgChildren(anRptOwner, springShape);

            // Set best height with springs and propogate to given parent
            springShape.setBestHeight();
            aParent.setSize(springShape.getWidth(), springShape.getHeight());

            // Add children back to given parent
            SGView children[] = springShape.getChildren().toArray(new SGView[0]);
            for (SGView child : children)
                aParent.addChild(child);

            // Return given parent
            return aParent;
        }
    }

    /**
     * A ReportGen subclass for PageCell.
     */
    public static class DocCellRPG<T extends SGDoc> extends ParentCellRPG<T> {

        /**
         * Override to handle ShapeLists special.
         */
        protected SGView rpgChildren(ReportOwner anRptOwner, SGParent aParent)
        {
            // Declare local variable for whether table of contents page was encountered
            SGPage tableOfContentsPage = null; int tocPageIndex = 0;

            SGDoc docCell = getShape();
            SGDoc doc = (SGDoc)aParent;
            for(int i=0, iMax=docCell.getChildCount(); i<iMax; i++) { SGPage page = docCell.getPage(i);

                // Check for table of contents table
                if(RMTableOfContents.checkForTableOfContents(page)) {
                    tableOfContentsPage = page; tocPageIndex = aParent.getChildCount(); continue; }

                // Generate report and add results
                SGParent crpg = (SGParent)anRptOwner.rpg(page, doc);
                if(crpg instanceof ReportOwner.ShapeList) {
                    for(SGView pg : crpg.getChildArray()) doc.addPage((SGPage)pg); }
                else doc.addPage((SGPage)crpg);
            }

            // Do RPG for TableOfContentsPage
            if(tableOfContentsPage!=null) RMTableOfContents.rpgPage(anRptOwner, doc, tableOfContentsPage, tocPageIndex);

            // Report report
            return aParent;
        }
    }
}
