/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.util.*;
import snap.util.*;
import snap.web.WebURL;

/**
 * This class creates an object graph of collections (Map/List) and core Java types from a given XML source.
 * This works best when an RMSchema is provided or an <RMSchema> tag is present in the xml (otherwise one is generated).
 */
public class RMXMLReader {

    // The schema of the read data (either loaded from RMSchema tag, reverse engineered, or provided to readObject)
    private Schema _schema;

    // A cache of lists for specific entity names
    private Map<String, List<Map<String,Object>>> _readObjectsForEntityNames = new LinkedHashMap<>();

    /**
     * Constructor.
     */
    public RMXMLReader()
    {
    }

    /**
     * Returns a hierarchy of RMEntity objects describing the XML.
     */
    public Schema getSchema()  { return _schema; }

    /**
     * Returns a map loaded from the given XML source with the given XML schema.
     */
    public Map<String,Object> readObjectFromXmlUrl(WebURL sourceUrl, Schema aSchema)
    {
        XMLElement rootXML = XMLElement.readXmlFromUrl(sourceUrl);
        return readObjectFromXmlAndSchema(rootXML, aSchema);
    }

    /**
     * Returns a map loaded from the given XML source with the given XML schema.
     */
    public Map<String,Object> readObjectFromXmlBytesAndSchema(byte[] sourceBytes, Schema aSchema)
    {
        XMLElement rootXML = XMLElement.readXmlFromBytes(sourceBytes);
        return readObjectFromXmlAndSchema(rootXML, aSchema);
    }

    /**
     * Returns a map loaded from the given XML source with the given XML schema.
     */
    private Map<String,Object> readObjectFromXmlAndSchema(XMLElement rootXML, Schema aSchema)
    {
        // If root is null, return null
        if (rootXML == null)
            return null;

        // Get root name
        String rootXmlName = rootXML.getName();

        // Get schema element if present (and remove it from root)
        XMLElement schema = rootXML.get("RMSchema");
        if (schema != null)
            rootXML.removeElement(schema);

        // If schema is provided, set it
        if (aSchema != null)
            _schema = aSchema;

        // Otherwise, if schema element is available, create and read schema
        else if (schema != null)
            _schema = new Schema(rootXmlName).fromXML(null, schema);

        // Otherwise, reverse engineer it from element
        else _schema = new RMSchemaMaker().getSchema(rootXML);

        // Make sure schema has root entity
        _schema.getRootEntity();

        // Create root map
        Map<String,Object> rootMap = new LinkedHashMap<>();

        // Read rootMap from root element (recursively reads everything)
        readObjectFromXmlAndEntity(rootXML, rootMap, rootXmlName);

        // Return root map
        return rootMap;
    }

    /**
     * Loads given map with collections & core types from given XML element, according to schema.
     */
    private void readObjectFromXmlAndEntity(XMLElement anElement, Map<String,Object> readObject, String entityName)
    {
        // Get entity, return if not found (should never happen, but I've seen it - maybe somehow with core types?)
        Entity entity = _schema.getEntity(entityName);
        if (entity == null) {
            System.err.println("RMXMLReader: Couldn't find entity named " + entityName);
            return;
        }

        // Iterate over entity properties
        for (int i = 0, iMax = entity.getPropertyCount(); i < iMax; i++) {
            Property prop = entity.getProperty(i);

            // If property is plain attribute, get string for property, convertToType and put in Map
            if (prop.isAttribute()) {

                // Get property name and value string
                String propName = prop.getName();
                String valueStr = anElement.getAttributeValue(propName);

                // If null, see if there is a child xml element
                if (valueStr == null) {
                    XMLElement valueEle = anElement.get(propName);
                    valueStr = valueEle == null ? null : valueEle.getValue();
                }

                // Get property value for string and add to map
                Object value = prop.convertValue(valueStr);
                if (value != null)
                    readObject.put(propName, value);
            }

            // Handle Relations
            else readRelation(anElement, readObject, prop);
        }
    }

    /**
     * Loads given map with collections & core types from given XML element, according to schema.
     */
    private void readRelation(XMLElement anElement, Map<String,Object> aMap, Property aRelation)
    {
        // Get property name and property relation entity name
        String propertyName = aRelation.getName();
        String relationEntityName = aRelation.getRelationEntityName();
        if (relationEntityName == null || relationEntityName.startsWith("[")) // Just return if null or Array class
            return;

        // Handle to-many relation: get children and iterate over them to recursively read and add to to-many list
        if (aRelation.isToMany()) {

            // Declare variable for list
            List<Map<String,Object>> readObjectsForRelation = null;

            // Iterate over child elements with name key
            for (int j = anElement.indexOf(propertyName); j >= 0; j = anElement.indexOf(propertyName, j + 1)) {

                // Create list if needed
                if (readObjectsForRelation == null)
                    aMap.put(propertyName, readObjectsForRelation = new ArrayList<>());

                // Get child xml element
                XMLElement child = anElement.get(j);

                // Get unique map for child xml element
                Map<String,Object> readObject = getUniqueMap(child, relationEntityName);

                // Add to list
                readObjectsForRelation.add(readObject);

                // Recurse into read
                readObjectFromXmlAndEntity(child, readObject, relationEntityName);
            }
        }

        // Handle to-one relation: get Map for child and recurse
        else {

            // Get xml element for to-one relation property
            XMLElement childXML = anElement.get(propertyName);

            // If no child element, but there is a child attribute, create a bogus element
            if (childXML == null && anElement.hasAttribute(propertyName)) {
                Entity relationEntity = aRelation.getRelationEntity();
                Property primary = relationEntity != null ? relationEntity.getPrimary() : null;
                String primaryName = primary != null ? primary.getName() : "ID";
                String primaryValue = anElement.getAttributeValue(propertyName);
                childXML = new XMLElement(propertyName);
                childXML.add(primaryName, primaryValue);
            }

            // If xml element found, get unique map for child xml, add to parent and recurse
            if (childXML != null) {
                Map<String,Object> map = getUniqueMap(childXML, relationEntityName);
                aMap.put(propertyName, map);
                readObjectFromXmlAndEntity(childXML, map, aRelation.getRelationEntityName());
            }
        }
    }

    /**
     * Returns a unique map for the given xml element and entity name using primary keys
     */
    private Map<String,Object> getUniqueMap(XMLElement anElement, String entityName)
    {
        // Get read objects for entity name
        List<Map<String,Object>> readObjects = _readObjectsForEntityNames.computeIfAbsent(entityName, k -> new LinkedList<>());

        // Get entity and list of primaries
        Entity entity = getSchema().getEntity(entityName);
        List<? extends Property> primaries = entity.getPrimaries();

        // Create map with primary key values from element
        Map<String,Object> primaryKeyValues = new LinkedHashMap<>();

        // Add primary key values from element
        for (Property property : primaries) {

            // Get primary value string (just return new map if any primary key is null)
            String valueString = anElement.getAttributeValue(property.getName());
            if (valueString == null)
                return primaryKeyValues;

            // Get value and add to map
            Object value = property.convertValue(valueString);
            if (value != null)
                primaryKeyValues.put(property.getName(), value);
        }

        // Iterate over entity maps
        if (!primaries.isEmpty()) {
            for (Map<String,Object> map2 : readObjects) {

                // Iterate over primary properties to see if maps' primary property(s) match
                for (int j = 0, jMax = primaries.size(); j < jMax && map2 != null; j++) {
                    Property property = primaries.get(j);

                    // Get primary property values and if they differ, break
                    Object map1PrimaryValue = primaryKeyValues.get(property.getName());
                    Object map2PrimaryValue = map2.get(property.getName());
                    if (!Objects.equals(map1PrimaryValue, map2PrimaryValue))
                        map2 = null;
                }

                // If loop map matched all primaries for element, return it
                if (map2 != null)
                    return map2;
            }
        }

        // Add read object to entity maps and return it
        readObjects.add(primaryKeyValues);
        return primaryKeyValues;
    }
}