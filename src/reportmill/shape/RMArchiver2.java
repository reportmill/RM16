package reportmill.shape;
import rmdraw.base.RMDateFormat;
import rmdraw.base.RMNumberFormat;
import rmdraw.shape.RMArchiver;
import snap.util.XMLArchiver;
import snap.util.XMLElement;
import java.util.Map;

/**
 * An RMArchiver subclass for ReportMill.
 */
public class RMArchiver2 extends RMArchiver {

    /**
     * Create RMArchiver2.
     */
    public RMArchiver2()
    {
        updateArchiverClassMapForRM();
    }

    /**
     * Updates RMArchiver ClassMap.
     */
    private void updateArchiverClassMapForRM()
    {
        // Get shared map (just return if already updated)
        Map cmap = RMArchiver.getClassMapShared();
        if (cmap.containsKey("table")) return;

        // Add RMShapes
        cmap.put("cell-table", RMCrossTab.class);
        cmap.put("cell-table-frame", RMCrossTabFrame.class);
        cmap.put("document", RMDocument2.class);
        cmap.put("graph", RMGraph.class);
        cmap.put("graph-legend", RMGraphLegend.class);
        cmap.put("label", RMLabel.class);
        cmap.put("labels", RMLabels.class);
        cmap.put("switchshape", RMSwitchShape.class);
        cmap.put("table", RMTable.class);
        cmap.put("table-group", RMTableGroup.class);
        cmap.put("tablerow", RMTableRow.class);
        cmap.put("PDFShape", RMPDFShape.class);

        // Formats
        cmap.put("format", TextFormatStub.class);
    }

    /**
     * A class to unarchive formats as proper subclass based on type attribute.
     */
    public static class TextFormatStub implements Archivable {

        /** Implement toXML for interface. */
        public XMLElement toXML(XMLArchiver anArchive)  { return null; }

        /** Implement fromXML to return proper format based on type attribute. */
        public Object fromXML(XMLArchiver anArchiver, XMLElement anElmnt)
        {
            String type = anElmnt.getAttributeValue("type","");
            if (type.equals("number"))
                return anArchiver.fromXML(anElmnt, RMNumberFormat.class,null);
            if (type.equals("date"))
                return anArchiver.fromXML(anElmnt, RMDateFormat.class, null);
            if (type.length()>0)
                System.err.println("TextFormatStub: Unknown format type " + type);
            return null;
        }
    }
}
