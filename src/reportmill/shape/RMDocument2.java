package reportmill.shape;
import rmdraw.base.RMTableOfContents;
import rmdraw.shape.*;

import java.util.HashMap;

/**
 * An RMDoc subclass for ReportMill stuff.
 */
public class RMDocument2 extends RMDocument {

    // The ReportOwner that created this document (if from RPG)
    private ReportOwner       _reportOwner;

    /**
     * Returns a generated report from this template evaluated against the given object.
     */
    public RMDocument generateReport()
    {
        return generateReport(getDataSource()!=null? getDataSource().getDataset() : null, null, true);
    }

    /**
     * Returns a generated report from this template evaluated against the given object.
     */
    public RMDocument generateReport(Object theObjects)  { return generateReport(theObjects, null, true); }

    /**
     * Returns a generated report from this template evaluated against the given object and userInfo.
     */
    public RMDocument generateReport(Object objects, Object userInfo)  { return generateReport(objects, userInfo, true); }

    /**
     * Returns a generated report from this template evaluated against the given object with an option to paginate.
     */
    public RMDocument generateReport(Object objects, boolean paginate)  { return generateReport(objects, null, paginate); }

    /**
     * Returns generated report from this template evaluated against given object/userInfo (with option to paginate).
     */
    public RMDocument generateReport(Object theObjects, Object theUserInfo, boolean aPaginateFlag)
    {
        // Create and configure reportmill with objects, userinfo, pagination and null-string
        ReportOwner ro = new ReportOwner();
        ro.setTemplate(this);
        if(theObjects!=null) ro.addModelObject(theObjects);
        if(theUserInfo!=null) ro.addModelObject(theUserInfo);
        ro.setPaginate(aPaginateFlag && isPaginate());
        ro.setNullString(getNullString());
        RMDocument rpt = ro.generateReport();
        return rpt;
    }

    /**
     * Add the pages in the given document to this document (at end) and clears the pages list in the given document.
     */
    public void addPages(RMDocument aDoc)
    {
        // Do normal version
        super.addPages(aDoc);

        // Add page reference shapes from given document and clear from old document
        RMDocument2 doc = aDoc instanceof RMDocument2 ? (RMDocument2)aDoc : null;
        if(doc!=null && _reportOwner!=null && doc._reportOwner!=null) {
            _reportOwner.getPageReferenceShapes().addAll(doc._reportOwner.getPageReferenceShapes());
            doc._reportOwner.getPageReferenceShapes().clear();
        }
    }

    /**
     * Override to handle ShapeLists special.
     */
    protected RMShape rpgChildren(ReportOwner anRptOwner, RMParentShape aParent)
    {
        // Declare local variable for whether table of contents page was encountered
        RMPage tableOfContentsPage = null; int tocPageIndex = 0;

        RMDocument doc = (RMDocument)aParent;
        for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMPage page = getPage(i);

            // Check for table of contents table
            if(RMTableOfContents.checkForTableOfContents(page)) {
                tableOfContentsPage = page; tocPageIndex = aParent.getChildCount(); continue; }

            // Generate report and add results
            RMParentShape crpg = (RMParentShape)anRptOwner.rpg(page, doc);
            if(crpg instanceof ReportOwner.ShapeList) {
                for(RMShape pg : crpg.getChildArray()) doc.addPage((RMPage)pg); }
            else doc.addPage((RMPage)crpg);
        }

        // Do RPG for TableOfContentsPage
        if(tableOfContentsPage!=null) RMTableOfContents.rpgPage(anRptOwner, doc, tableOfContentsPage, tocPageIndex);

        // Report report
        return aParent;
    }

    /**
     * Performs page substitutions on any text fields that were identified as containing @Page@ keys.
     */
    public void resolvePageReferences()  { if(_reportOwner!=null) _reportOwner.resolvePageReferences(); }

    /**
     * Override for RMDocument.
     */
    public RMDocument2 clone()
    {
        RMDocument2 clone = (RMDocument2)super.clone();
        clone._reportOwner = null;
        return clone;
    }

}
