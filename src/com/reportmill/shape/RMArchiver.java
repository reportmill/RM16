/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.reportmill.base.RMGrouper;
import com.reportmill.base.RMGrouping;
import com.reportmill.graphics.*;
import snap.util.ListUtils;
import snap.util.SnapUtils;
import snap.util.XMLElement;
import snap.web.WebURL;

/**
 * This class manages archival and unarchival to/from XMLElements.
 * <p>
 * For archival, objects simply implement the toXML() method to configure and return XMLElements. Archiver's
 * toXML() method manages the process, allowing for object references.
 * <p>
 * For unarchival, classes register for particular element names. Then during Archiver.fromXML(), Archiver will
 * call fromXML() on the classes for encountered tags to reconstruct the object graph.
 */
public class RMArchiver {

    // The owner class that initated archival/unarchival
    private Object _owner;

    // The URL of the source the archiver is reading from
    private WebURL _surl;

    // Root element for unarchival
    private XMLElement _root;

    // The root object to be used in unarchival
    private Object _rootObject;

    // The version of unarchived object
    private double _version;

    // Whether element should ignore case when asking for attributes/elements by name
    private boolean _ignoreCase;

    // Unarchival keeps track of read elements to guarantee that each element results in one instance
    private Map<XMLElement, Object> _readElements = new HashMap<>();

    // list of objects that are archived by reference
    private List<Object> _references = new ArrayList<>();

    // Archiver manages archival of shared BLOBs external to normal element hierarchy
    private List<Resource> _resources = new ArrayList<>();

    // The map of classes for unarchival
    private Map<String,Class<?>> _classMap;

    // Constant for RM Shapes list xml name
    private static final String RM_SHAPES_LIST_NAME = "RMShapesList";

    /**
     * Constructor.
     */
    public RMArchiver()
    {
    }

    /**
     * Returns the WebURL of the currently loading archive.
     */
    public WebURL getSourceURL()  { return _surl; }

    /**
     * Sets the WebURL of the currently loading archive.
     */
    public void setSourceURL(WebURL aURL)
    {
        _surl = aURL;
    }

    /**
     * Returns the owner.
     */
    public Object getOwner()  { return _owner; }

    /**
     * Sets the owner.
     */
    public void setOwner(Object anOwner)  { _owner = anOwner; }

    /**
     * Returns the object that the archiver should read "into".
     */
    public Object getRootObject()  { return _rootObject; }

    /**
     * Sets the object that the archiver should read "into".
     */
    public void setRootObject(Object anObj)  { _rootObject = anObj; }

    /**
     * Returns the version of the document.
     */
    public double getVersion()  { return _version; }

    /**
     * Sets the version of the document.
     */
    public void setVersion(double aVersion)  { _version = aVersion; }

    /**
     * Returns whether element should ignore case when asking for attributes/elements by name.
     */
    public boolean isIgnoreCase()  { return _ignoreCase; }

    /**
     * Sets whether element should ignore case when asking for attributes/elements by name.
     */
    public void setIgnoreCase(boolean aVal)  { _ignoreCase = aVal; }

    /**
     * Returns the class map.
     */
    public Map<String, Class<?>> getClassMap()
    {
        if (_classMap != null) return _classMap;
        return _classMap = createClassMap();
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

        // Graphics
        classMap.put("color", RMColor.class);
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
     * Reads shapes from given XML.
     */
    public List<RMShape> readShapesFromXmlBytes(byte[] theBytes)
    {
        XMLElement shapesListXml = XMLElement.readXmlFromBytes(theBytes);
        if (shapesListXml == null || !shapesListXml.getName().equals(RM_SHAPES_LIST_NAME)) {
            System.err.println("RMArchiver.readShapesFromXmlBytes: XML element not found");
            return Collections.emptyList();
        }

        // Read shapes list
        List<RMShape> shapesList = new ArrayList<>();
        for (int i = 0, iMax = shapesListXml.size(); i < iMax; i++)
            shapesList.add((RMShape) readObjectFromXml(shapesListXml.get(i), null));
        return shapesList;
    }

    /**
     * Returns a root object unarchived from a generic input source (a File, String path, InputStream, URL, byte[], etc.).
     */
    public Object readObjectFromXmlUrl(WebURL sourceUrl)
    {
        // Set source URL
        if (getSourceURL() == null)
            setSourceURL(sourceUrl);

        // Get string and read
        String xmlStr = sourceUrl.getText();
        if (xmlStr == null)
            throw new RuntimeException("RMArchiver.readXmlFromUrl: Cannot read url: " + sourceUrl);
        return readObjectFromXmlString(xmlStr);
    }

    /**
     * Returns a root object unarchived from a generic XML source (File, String path, InputStream, URL, byte[], etc.).
     */
    public Object readObjectFromXmlString(String xmlString)
    {
        XMLElement xml = XMLElement.readXmlFromString(xmlString);
        return readObjectFromXml(xml);
    }

    /**
     * Returns a root object unarchived from an RMByteSource.
     */
    public Object readObjectFromXmlBytes(byte[] theBytes)
    {
        XMLElement xml = XMLElement.readXmlFromBytes(theBytes);
        return readObjectFromXml(xml);
    }

    /**
     * Returns a root object unarchived from the XML source (a File, String path, InputStream, URL, byte[], etc.).
     * You can also provide a root object to be read "into", and an owner that the object is being read "for".
     */
    public Object readObjectFromXml(XMLElement theXML)
    {
        // Set IgnoreCase property
        if (isIgnoreCase())
            theXML.setIgnoreCase(true);

        // Read xml
        _root = theXML;

        // Get resources from top level xml
        getResources(_root);

        // Unarchive from xml and return
        Object object = readObjectFromXml(_root, null);

        // Return object
        return object;
    }

    /**
     * Writes the given document to XML.
     */
    public XMLElement writeDocumentToXml(RMDocument aDoc)
    {
        XMLElement xml = writeObjectToXml(aDoc);

        // Archive resources
        for (Resource resource : getResources()) {
            XMLElement resourceXML = new XMLElement("resource");
            resourceXML.add("name", resource.name());
            resourceXML.setValueBytes(resource.bytes());
            xml.add(resourceXML);
        }

        return xml;
    }

    /**
     * Writes the given shapes list to XML.
     */
    public XMLElement writeShapesToXml(List<RMShape> shapesList)
    {
        XMLElement shapesXml = new XMLElement(RM_SHAPES_LIST_NAME);
        shapesList.forEach(shape -> shapesXml.add(writeObjectToXml(shape)));
        return shapesXml;
    }

    /**
     * Writes the given object to XML.
     */
    public XMLElement writeObjectToXml(Archivable anObj)
    {
        return writeObjectToXml(anObj, null);
    }

    /**
     * Writes the given object to XML.
     */
    public XMLElement writeObjectToXml(Archivable anObj, Archivable anOwner)
    {
        return anObj.toXML(this);
    }

    /**
     * Returns an object unarchived from the given element.
     */
    public Object readObjectFromXml(XMLElement anElement, Object anOwner)
    {
        return readObjectFromXmlForClass(anElement, null, anOwner);
    }

    /**
     * Returns an object unarchived from the given element by instantiating the given class.
     */
    public <T> T readObjectFromXmlForClass(XMLElement anElement, Class<T> aClass, Object anOwner)
    {
        // See if anElement has already been read
        Object readObject = _readElements.get(anElement);
        if (readObject != null && (aClass == null || aClass.isInstance(readObject)))
            return (T) readObject;

        // If root element and owner is same class, set read object to owner
        if (anElement == _root && getRootObject() != null)
            readObject = getRootObject();

        // If class was provided, try to instantiate
        else {

            // Get class
            Class<?> cls = aClass;
            if (cls == null)
                cls = getClassForXML(anElement);

            // If no class, throw exception
            if (cls == null)
                throw new RuntimeException("RMArchiver: Can't find class for element: " + anElement.getName());

            // Create new object
            readObject = newInstance(cls);
        }

        // If couldn't create new instance, return null (should throw exception instead, I think)
        if (readObject == null) {
            System.err.println("RMArchiver.fromXML: Couldn't find class for: " + anElement.getName());
            return null;
        }

        // Add new instance to readElement's map
        _readElements.put(anElement, readObject);

        // Call fromXML on object
        Object obj = ((Archivable) readObject).fromXML(this, anElement);

        // If fromXML returned a different object, swap it in
        if (obj != readObject)
            _readElements.put(anElement, readObject = obj);

        // Return read object
        return (T) readObject;
    }

    /**
     * Returns the class for a given element.
     */
    protected Class<?> getClassForXML(XMLElement anElement)
    {
        // If ClassName attribute is present, try that
        String className = anElement.getAttributeValue("ClassName");
        if (className != null) {
            Class<?> cls = getClassForName(className);
            if (cls != null)
                return cls;
        }

        // Get class for element name
        String xmlName = anElement.getName();
        Class<?> classForXml = getClassForName(xmlName);

        // If element has type, see if there is class for type-name
        String typeName = anElement.getAttributeValue("type");
        if (typeName != null) {
            Class<?> typeClass = getClassForName(typeName + "-" + xmlName);
            if (typeClass != null)
                classForXml = typeClass;
        }

        // Return
        return classForXml;
    }

    /**
     * Returns the class for a given element name.
     */
    public Class<?> getClassForName(String aName)  { return getClassMap().get(aName); }

    /**
     * Returns a reference id for the given object (used in archival).
     */
    public int getReference(Object anObj)
    {
        return getReference(anObj, true);
    }

    /**
     * Returns a reference id for given object if in references list with option to add if absent (used in archival).
     */
    public int getReference(Object anObj, boolean add)
    {
        // If object is in list (or if not asked to add) return its current index
        int index = ListUtils.indexOfId(_references, anObj);
        if (index >= 0 || !add)
            return index;

        // Add object to references and return id
        _references.add(anObj);
        return _references.size() - 1;
    }

    /**
     * Returns an object for a given reference (used in unarchival).
     */
    public Object getReference(String aName, XMLElement anElement)
    {
        // Get xref id and recursively search for element that contains it
        int xref = anElement.getAttributeIntValue(aName, -1);
        if (xref < 0) return null;
        return getReference(xref, _root);
    }

    /**
     * Returns an object unarchived from element that has the given xref id.
     */
    private Object getReference(int xref, XMLElement anElement)
    {
        // If anElement has matching xref attribute/id, return unarchived object
        if (anElement.getAttributeIntValue("xref", -1) == xref)
            return readObjectFromXml(anElement, null);

        // Iterate over element's children and recurse
        for (int i = 0, iMax = anElement.size(); i < iMax; i++) {
            XMLElement e = anElement.get(i);
            Object obj = getReference(xref, e);
            if (obj != null)
                return obj;
        }

        // If xref not found it this element or its children, return null
        return null;
    }

    /**
     * Returns the index of the first child element with the given name.
     */
    public int indexOf(XMLElement anElement, Class<?> aClass)
    {
        return indexOf(anElement, aClass, 0);
    }

    /**
     * Returns the index of the first child element with the given name at or beyond the given index.
     */
    public int indexOf(XMLElement anElement, Class<?> aClass, int startIndex)
    {
        // Iterate over element children from start index, and if child has matching class, return its index
        for (int i = startIndex, iMax = anElement.size(); i < iMax; i++) {
            XMLElement childXML = anElement.get(i);
            Class<?> childClass = getClassForXML(childXML);
            if (childClass != null && aClass.isAssignableFrom(childClass))
                return i;
        }
        return -1; // Return -1 since element name not found
    }

    /**
     * Returns the list of objects of the given name and/or class (either can be null) unarchived from the given element.
     */
    public List fromXMLList(XMLElement anElement, String aName, Class<?> aClass, Object anOwner)
    {
        // Declare variable for list
        List<Object> list = new ArrayList<>();

        // If name is provided, iterate over elements, unarchive and add to list
        if (aName != null) {
            for (XMLElement e : anElement.getElements(aName)) {
                Object obj = readObjectFromXmlForClass(e, aClass, anOwner);
                list.add(obj);
            }
        }

        // Iterate over elements, unarchive, and if class, add to list
        else for (int i = 0, iMax = anElement.size(); i < iMax; i++) {
            XMLElement xml = anElement.get(i);
            Object obj = readObjectFromXml(xml, anOwner);
            if (aClass.isInstance(obj))
                list.add(obj);
        }

        // Return to list
        return list;
    }

    /**
     * Returns a copy of the given object using archival.
     */
    public <T extends Archivable> T copy(T anObj)
    {
        XMLElement xml = writeObjectToXml(anObj);
        if (isIgnoreCase())
            xml.setIgnoreCase(true);
        return (T) readObjectFromXml(xml, null);
    }

    /**
     * Returns the list of optional resources associated with this archiver.
     */
    public List<Resource> getResources()  { return _resources; }

    /**
     * Returns an individual resource associated with this archiver, by index.
     */
    public Resource getResource(int anIndex)  { return _resources.get(anIndex); }

    /**
     * Returns an individual resource associated with this archiver, by name.
     */
    public byte[] getResource(String aName)
    {
        for (int i = 0, iMax = _resources.size(); i < iMax; i++)
            if (getResource(i).name.equals(aName))
                return getResource(i).bytes;
        return null;
    }

    /**
     * Adds a byte array resource to this archiver (only if absent).
     */
    public String addResource(byte[] bytes, String aName)
    {
        // If resource has already been added, just return it's name
        for (Resource resource: _resources)
            if (Arrays.equals(resource.bytes, bytes))
                return resource.name;

        // If new resource, add it
        _resources.add(new Resource(aName, bytes));

        // Return given name
        return aName;
    }

    /**
     * Reads resources from {@literal <resource>} elements in given xml (top-level) element, converts from ASCII encoding and
     * adds to archiver.
     */
    protected void getResources(XMLElement anElement)
    {
        // Get resources from top level <resource> tags
        for (int i = anElement.indexOf("resource"); i >= 0; i = anElement.indexOf("resource", i)) {

            // Get/remove current resource element
            XMLElement e = anElement.removeElement(i);

            // Get resource name and bytes
            String name = e.getAttributeValue("name");
            byte[] bytes = e.getValueBytes();

            // Add resource bytes for name
            addResource(bytes, name);
        }
    }

    /**
     * Returns a new instance of an object given a class.
     */
    private static Object newInstance(Class<?> aClass)
    {
        try { return aClass.getConstructor().newInstance(); }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) { throw new RuntimeException(e); }
    }

    /**
     * An interface for objects that are archivable.
     */
    public interface Archivable {

        /**
         * Archival.
         */
        XMLElement toXML(RMArchiver anArchiver);

        /**
         * Unarchival.
         */
        Object fromXML(RMArchiver anArchiver, XMLElement anElement);
    }

    /**
     * This record represents a named resource associated with an archiver.
     */
    public record Resource(String name, byte[] bytes) { }
}