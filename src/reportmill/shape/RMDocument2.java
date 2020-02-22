package reportmill.shape;
import reportmill.out.RMExcelWriter;
import reportmill.out.RMRTFWriter;
import reportmill.out.RMStringWriter;
import reportmill.util.RMTableOfContents;
import reportmill.out.RMHtmlFile;
import reportmill.util.RMDataSource;
import rmdraw.base.ReportMill;
import rmdraw.shape.*;
import snap.util.SnapUtils;
import snap.util.XMLArchiver;
import snap.util.XMLElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An RMDoc subclass for ReportMill stuff.
 * RMDocuments are also what ReportMill refers to as templates, and is commonly used like
 * this:
 * <p><blockquote><pre>
 *   RMDocument template = new RMDocument(aSource); // Load from path String, File, byte array, etc.
 *   RMDocument report = template.generateReport(aDataset); // Any Java dataset: EJBs, custom classes, collctns, etc.
 *   report.writePDF("MyReport.pdf");
 * </pre></blockquote><p>
 */
public class RMDocument2 extends RMDocument implements ReportGen.RPG {

    // The ReportMill version this document was created with
    private double _version = ReportMill.getVersion();

    // Datasource
    private RMDataSource _dataSource;

    // A map for extra document metadata (added to PDF)
    private Map<String, String> _metadata;

    // The ReportOwner that created this document (if from RPG)
    private ReportOwner _rptOwner;

    /** Initialize ReportMill. */
    static { ReportMill.init(); }

    /**
     * Creates a plain empty document. It's really only used by the archiver.
     */
    public RMDocument2()
    {
        super();
    }

    /**
     * Creates a document with the given width and height (in printer points).
     */
    public RMDocument2(double aWidth, double aHeight)
    {
        super(aWidth, aHeight);
    }

    /**
     * Creates a new document from the given source.
     */
    public RMDocument2(Object aSource)
    {
        new RMArchiver().getDoc(aSource, this);
    }

    /**
     * Creates a new document from aSource using RMArchiver.
     */
    public static RMDocument2 getDoc(Object aSource)
    {
        return (RMDocument2) new RMArchiver().getDoc(aSource);
    }

    /**
     * Returns the version this document was loaded as.
     */
    public double getVersion()  { return _version; }

    /**
     * Returns the RMDataSource associated with this document.
     */
    public RMDataSource getDataSource()
    {
        return _dataSource;
    }

    /**
     * Sets the RMDataSource associated with this document.
     */
    public void setDataSource(RMDataSource aDataSource)
    {
        firePropChange("DataSource", _dataSource, _dataSource = aDataSource);
    }

    /**
     * Returns the document as a byte array of a PDF file.
     */
    public byte[] getBytesPDF()
    {
        return new reportmill.out.RMPDFWriter().getBytes(this);
    }

    /**
     * Returns the document as a byte array of an HTML file.
     */
    public byte[] getBytesHTML()
    {
        return new RMHtmlFile(this).getBytes();
    }

    /**
     * Returns the document as a byte array of a CSV file.
     */
    public byte[] getBytesCSV()
    {
        return getBytesDelimitedAscii(",", "\n", true);
    }

    /**
     * Returns the document as a byte array of a delimited ASCII file (using given field, record separator strings).
     */
    public byte[] getBytesDelimitedAscii(String fieldDelimiter, String recordDelimiter, boolean quoteFields)
    {
        System.err.println("RMDocument.getBytesDelimitedAscii: Not implemented");
        return RMStringWriter.delimitedAsciiBytes(this, fieldDelimiter, recordDelimiter, quoteFields);
    }

    /**
     * Returns the document as byte array of an Excel file.
     */
    public byte[] getBytesExcel()
    {
        return new RMExcelWriter().getBytes(this);
    }

    /**
     * Returns the document as byte array of an Excel file.
     */
    public byte[] getBytesRTF()
    {
        System.err.println("RMDocument.getBytesDelimitedAscii: Not implemented");
        return new RMRTFWriter().getBytes(this);
    }

    /**
     * Writes the document out to the given path String (it extracts type from path extension).
     */
    public void write(String aPath)
    {
        String path = aPath.toLowerCase();
        if (path.endsWith(".pdf")) writePDF(aPath);
        else if (path.endsWith(".html")) new RMHtmlFile(this).write(aPath);
        else if (path.endsWith(".csv")) SnapUtils.writeBytes(getBytesCSV(), aPath);
        else if (path.endsWith(".xls")) SnapUtils.writeBytes(getBytesExcel(), aPath);
        else if (path.endsWith(".rtf")) SnapUtils.writeBytes(getBytesRTF(), aPath);
        else super.write(aPath);
    }

    /**
     * Writes the document to the given path String as PDF.
     */
    public void writePDF(String aPath)
    {
        SnapUtils.writeBytes(getBytesPDF(), aPath);
    }

    /**
     * Returns map of metadata.
     */
    public Map<String, String> getMetadata()
    {
        return _metadata != null ? _metadata : Collections.EMPTY_MAP;
    }

    /**
     * Returns a metadata value.
     */
    public String getMetaValue(String aKey)
    {
        return _metadata != null ? _metadata.get(aKey) : null;
    }

    /**
     * Sets a metadata value.
     */
    public void setMetaValue(String aKey, Object aValue)
    {
        if (_metadata == null) _metadata = new HashMap();
        if (aValue == null) _metadata.remove(aKey);
        else _metadata.put(aKey, aValue.toString());
    }

    /**
     * Returns a generated report from this template evaluated against the given object.
     */
    public RMDocument2 generateReport()
    {
        return generateReport(getDataSource() != null ? getDataSource().getDataset() : null, null, true);
    }

    /**
     * Returns a generated report from this template evaluated against the given object.
     */
    public RMDocument2 generateReport(Object theObjects)
    {
        return generateReport(theObjects, null, true);
    }

    /**
     * Returns a generated report from this template evaluated against the given object and userInfo.
     */
    public RMDocument2 generateReport(Object objects, Object userInfo)
    {
        return generateReport(objects, userInfo, true);
    }

    /**
     * Returns a generated report from this template evaluated against the given object with an option to paginate.
     */
    public RMDocument2 generateReport(Object objects, boolean paginate)
    {
        return generateReport(objects, null, paginate);
    }

    /**
     * Returns generated report from this template evaluated against given object/userInfo (with option to paginate).
     */
    public RMDocument2 generateReport(Object theObjects, Object theUserInfo, boolean aPaginateFlag)
    {
        // Create and configure reportmill with objects, userinfo
        ReportOwner rptOwner = new ReportOwner();
        rptOwner.setTemplate(this);
        if (theObjects != null)
            rptOwner.addModelObject(theObjects);
        if (theUserInfo != null)
            rptOwner.addModelObject(theUserInfo);

        // Set pagination and null-string
        rptOwner.setPaginate(aPaginateFlag && isPaginate());
        rptOwner.setNullString(getNullString());

        // Generate report and return (set ivar to pass ReportOwner to new report in rpg clone)
        _rptOwner = rptOwner;
        RMDocument2 rpt = rptOwner.generateReport();
        _rptOwner = null;
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
        RMDocument2 doc = aDoc instanceof RMDocument2 ? (RMDocument2) aDoc : null;
        if (doc != null && _rptOwner != null && doc._rptOwner != null) {
            _rptOwner.getPageReferenceShapes().addAll(doc._rptOwner.getPageReferenceShapes());
            doc._rptOwner.getPageReferenceShapes().clear();
        }
    }

    /**
     * Override to handle ShapeLists special.
     */
    public RMShape rpgChildren(ReportOwner anRptOwner, RMParentShape aParent)
    {
        // Declare local variable for whether table of contents page was encountered
        RMPage tableOfContentsPage = null;
        int tocPageIndex = 0;

        RMDocument doc = (RMDocument) aParent;
        for (int i = 0, iMax = getChildCount(); i < iMax; i++) {
            RMPage page = getPage(i);

            // Check for table of contents table
            if (RMTableOfContents.checkForTableOfContents(page)) {
                tableOfContentsPage = page;
                tocPageIndex = aParent.getChildCount();
                continue;
            }

            // Generate report and add results
            RMParentShape crpg = (RMParentShape) anRptOwner.rpg(page, doc);
            if (crpg instanceof ReportOwner.ShapeList) {
                for (RMShape pg : crpg.getChildArray()) doc.addPage((RMPage) pg);
            } else doc.addPage((RMPage) crpg);
        }

        // Do RPG for TableOfContentsPage
        if (tableOfContentsPage != null) RMTableOfContents.rpgPage(anRptOwner, doc, tableOfContentsPage, tocPageIndex);

        // Report report
        return aParent;
    }

    /**
     * Performs page substitutions on any text fields that were identified as containing @Page@ keys.
     */
    public void resolvePageReferences()
    {
        if (_rptOwner != null)
            _rptOwner.resolvePageReferences();
    }

    /**
     * Override for RMDocument.
     */
    public RMDocument2 clone()
    {
        RMDocument2 clone = (RMDocument2) super.clone();
        if (_metadata != null) clone._metadata = new HashMap(_metadata);
        return clone;
    }

    /**
     * XML archival.
     */
    protected XMLElement toXMLShape(XMLArchiver anArchiver)
    {
        // Archive basic shape attributes and reset element name
        XMLElement e = super.toXMLShape(anArchiver);

        // Archive Version
        e.add("version", ReportMill.getVersion());

        // Archive DataSource
        XMLElement dxml = _dataSource != null ? anArchiver.toXML(_dataSource, this) : null;
        if (dxml != null && dxml.getAttributeCount() > 0) e.add(dxml);

        // Return xml
        return e;
    }

    /**
     * XML unarchival.
     */
    protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
    {
        // Do normal version
        super.fromXMLShape(anArchiver, anElement);

        // Unarchive Version
        _version = anElement.getAttributeFloatValue("version", 8.0f);
        anArchiver.setVersion(_version);

        // Unarchive Datasource
        XMLElement dxml = anElement.get("datasource");
        if (dxml != null) {
            RMDataSource ds = anArchiver.fromXML(dxml, RMDataSource.class, this);
            setDataSource(ds);
        }
    }
}
