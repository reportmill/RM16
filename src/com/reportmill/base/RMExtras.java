/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import com.reportmill.graphics.*;
import com.reportmill.out.RMPDFWriter;
import com.reportmill.shape.*;
import java.util.*;
import snap.geom.Rect;
import snap.util.*;
import snap.web.WebURL;

/**
 * This file is just meant to hold various utility methods that customers have asked for.
 */
public class RMExtras {

    /**
     * Returns the Hollywood db URL.
     */
    public static WebURL getHollywoodURL()
    {
        return WebURL.getResourceUrl(RMExtras.class, "/com/reportmill/examples/HollywoodDB.xml");
    }

    /**
     * Returns the Movies Rpt URL.
     */
    public static WebURL getMoviesURL()
    {
        return WebURL.getResourceUrl(RMExtras.class, "/com/reportmill/examples/Movies.rpt");
    }

    /**
     * Iterates over all document (or shape) text and replaces occurrences of the first string with the second.
     */
    public static void replaceText(RMShape aShape, String aString1, String aString2)
    {
        // Handle RMTextShape
        if (aShape instanceof RMTextShape text) {
            RMXString xstring = text.getXString();
            String string = xstring.getText();
            for (int i = string.indexOf(aString1); i >= 0; i = string.indexOf(aString1, i)) {
                xstring.replaceChars(aString2, i, i + aString1.length());
                string = xstring.getText();
                i += aString1.length();
            }
        }

        // Handle anything else
        else for (int i = 0, iMax = aShape.getChildCount(); i < iMax; i++) {
            RMShape child = aShape.getChild(i);
            replaceText(child, aString1, aString2);
        }
    }

    /**
     * Replaces any dataset key in template that matches first given dataset key with the second given dataset key.
     */
    public static void replaceDatasetKey(RMShape aShape, String aKey1, String aKey2)
    {
        // If shape is table, check table dataset key, and replace if found
        if (aShape instanceof RMTable table) {
            if (table.getDatasetKey().equals(aKey1))
                table.setDatasetKey(aKey2);
        }

        // If shape is document, recursively call replace on pages
        else if (aShape instanceof RMDocument doc) {
            for (int i = 0; i < doc.getPageCount(); i++)
                replaceDatasetKey(doc.getPage(i), aKey1, aKey2);
        }

        // If shape is a table group, recursively call all top-level tables
        else if (aShape instanceof RMTableGroup tableGroup) {
            for (int i = 0; i < tableGroup.getChildTableCount(); i++)
                replaceDatasetKey(tableGroup.getChildTable(i), aKey1, aKey2);
        }

        // If shape is anything else, recursively call replaceSort on children
        else for (int i = 0; i < aShape.getChildCount(); i++)
                replaceDatasetKey(aShape.getChild(i), aKey1, aKey2);
    }

    /**
     * Replaces any grouping key in the template that matches first given key with the second given key.
     */
    public static void replaceGroupingKey(RMShape aShape, String aKey1, String aKey2)
    {
        // If shape is table, check table grouping's sorts for sort, and replace if found
        if (aShape instanceof RMTable table) {

            // Iterate over table groupings
            for (int i = 0; i < table.getGroupingCount(); i++) {
                RMGrouping grouping = table.getGrouping(i);

                // If grouping key matches given key, rename header, details and summary titles and set new key
                if (grouping.getKey().equals(aKey1)) {
                    table.setTitleForChild(aKey1 + " Header", aKey2 + " Header");
                    table.setTitleForChild(aKey1 + " Details", aKey2 + " Details");
                    table.setTitleForChild(aKey1 + " Summary", aKey2 + " Summary");
                    grouping.setKey(aKey2);
                }
            }
        }

        // If shape is document, recursively call replace on pages
        else if (aShape instanceof RMDocument doc) {
            for (int i = 0; i < doc.getPageCount(); i++)
                replaceGroupingKey(doc.getPage(i), aKey1, aKey2);
        }

        // If shape is a table group, recursively call all top-level tables
        else if (aShape instanceof RMTableGroup tableGroup) {
            for (int i = 0; i < tableGroup.getChildTableCount(); i++)
                replaceGroupingKey(tableGroup.getChildTable(i), aKey1, aKey2);
        }

        // If shape is anything else, recursively call replaceSort on children
        else for (int i = 0; i < aShape.getChildCount(); i++)
                replaceGroupingKey(aShape.getChild(i), aKey1, aKey2);
    }

    /**
     * Replaces any sort in the template that matches first given sort with the second given sort.
     */
    public static void replaceSort(RMShape aShape, String aSort1, String aSort2)
    {
        // If shape is table, check table grouping's sorts for sort, and replace if found
        if (aShape instanceof RMTable table) {

            // Iterate over table groupings, and grouping sorts - if sort key matches, replace
            for (int i = 0; i < table.getGroupingCount(); i++) {
                RMGrouping grouping = table.getGrouping(i);
                for (int j = 0; j < grouping.getSortCount(); j++) {
                    RMSort sort = grouping.getSort(j);
                    if (sort.getKey().equals(aSort1))
                        grouping.getSorts().set(j, new RMSort(aSort2));
                }
            }
        }

        // If shape is document, recursively call replace on pages
        else if (aShape instanceof RMDocument doc) {
            for (int i = 0; i < doc.getPageCount(); i++)
                replaceSort(doc.getPage(i), aSort1, aSort2);
        }

        // If shape is a table group, recursively call all top-level tables
        else if (aShape instanceof RMTableGroup tableGroup) {
            for (int i = 0; i < tableGroup.getChildTableCount(); i++)
                replaceSort(tableGroup.getChildTable(i), aSort1, aSort2);
        }

        // If shape is anything else, recursively call replaceSort on children
        else for (int i = 0; i < aShape.getChildCount(); i++)
                replaceSort(aShape.getChild(i), aSort1, aSort2);
    }

    /**
     * Replaces a format.
     */
    public static void replaceFormat(RMShape aShape, RMFormat aFormat)
    {
        // Handle document
        if (aShape instanceof RMDocument document) {
            for (RMPage page : document.getPages())
                replaceFormat(page, aFormat);
        }

        // Handle anything with children
        else if (aShape.getChildCount() > 0)
            for (RMShape child : aShape.getChildren())
                replaceFormat(child, aFormat);

            // Handle Text
        else if (aShape instanceof RMTextShape text) {
            RMXString xstring = text.getXString();
            for (int i = 0, iMax = xstring.getRunCount(); i < iMax; i++) {
                RMXStringRun run = xstring.getRun(i);
                if (run.getFormat() != null && run.getFormat().getClass() == aFormat.getClass())
                    xstring.setAttribute(RMTextStyle.FORMAT_KEY, aFormat, run.start(), run.end());
            }
        }

        // Handle anything else
        else if (aShape.getFormat() != null && aShape.getFormat().getClass() == aFormat.getClass())
            aShape.setFormat(aFormat);
    }

    /**
     * Returns a StringBuffer image map for a given shape.
     */
    public static StringBuffer getImageMap(RMShape aShape, StringBuffer aSB)
    {
        // Get string buffer (create if missing)
        StringBuffer buffer = aSB != null ? aSB : new StringBuffer("<MAP>");

        // If URL, add map area
        if (aShape.getURL() != null) {
            Rect bounds = aShape.localToParent(aShape.getBoundsInside(), aShape.getPageShape()).getBounds();
            buffer.append("<AREA SHAPE=RECT COORDS=\"");
            buffer.append((int) bounds.x + "," + (int) bounds.y + "," + (int) bounds.width + "," + (int) bounds.height + "\" ");
            buffer.append("HREF=\"").append(aShape.getURL()).append("\" ");
            buffer.append(">\n");
        }

        // Recurse into children
        for (int i = 0; i < aShape.getChildCount(); i++)
            getImageMap(aShape.getChild(i), buffer);

        // If top-level, close map
        if (aSB == null) {
            buffer.append("</MAP>");
            System.out.println(buffer);
        }

        // Return buffer
        return buffer;
    }

    /**
     * Prints name of every shape in a template hierarchy.
     */
    public static void printNames(RMShape aShape)
    {
        // Print shape name
        System.out.println(aShape.getClass().getName() + ": " + aShape.getName());

        // If shape is document, recursively call replace on pages
        if (aShape instanceof RMDocument doc) {
            for (int i = 0; i < doc.getPageCount(); i++)
                printNames(doc.getPage(i));
        }

        // If shape is a table group, recursively call all top-level tables
        else if (aShape instanceof RMTableGroup)
            printNames((RMTableGroup) aShape, null);

            // If shape is anything else, recursively call replaceSort on children
        else for (int i = 0; i < aShape.getChildCount(); i++)
                printNames(aShape.getChild(i));
    }

    /**
     * Prints name of every shape in table group hierarchy.
     */
    public static void printNames(RMTableGroup aTableGroup, RMTable aTable)
    {
        // Get table or table group
        RMShape tableOrTableGroup = aTable == null ? aTableGroup : aTable;

        // Print table (or tableGroup) name
        System.out.println(tableOrTableGroup.getClass().getName() + ": " + tableOrTableGroup.getName());

        // Iterate over child tables for given table and recurse into standard printNames and table group print names
        for (int i = 0; i < aTableGroup.getChildTableCount(aTable); i++) {
            RMTable table = aTableGroup.getChildTable(aTable, i);
            printNames(table);
            printNames(aTableGroup, table);
        }
    }

    /**
     * Adds the contents from second document to the bottom of the first page of first document.
     */
    public static void addToPage(RMDocument aDoc1, RMDocument aDoc2)
    {
        // Get page 1 & 2 from doc 1 & 2
        RMPage page1 = aDoc1.getPage(0);
        RMPage page2 = aDoc2.getPage(0);

        // Get copy of page 2 child list
        RMShape[] children = page2.getChildArray();

        // Iterate over page 2 children, add to page 1 and shift to bottom of page
        for (RMShape child : children) {
            child.setY(child.getY() + page1.getHeight());
            page1.addChild(child);
        }

        // Extend page 1 to height required for page 2
        page1.setHeight(page1.getHeight() + page2.getHeight());
    }

    /**
     * An example of adding a static page between all pages (and at the end) to effectively provide a page back when
     * printing front and back.
     */
    public static void addPageBetweenPages()
    {
        // Get template
        RMDocument template = new RMDocument(getMoviesURL());

        // Get objects
        Map map = new RMXMLReader().readObjectFromXmlUrl(getHollywoodURL(), template.getDataSourceSchema());

        // Generate report
        RMDocument report = template.generateReport(map);

        // Resolve page references
        report.resolvePageReferences();

        // Create notice page
        RMPage page = report.createPage();
        RMTextShape text = new RMTextShape("General Billing Information");
        text.setBounds(72, 72, 300, 100);
        page.addChild(text);

        // Iterate over report
        for (int i = report.getPageCount(); i > 0; i--)
            report.addPage((RMPage) page.cloneDeep(), i);

        // Write report
        report.write("/tmp/Report.pdf");
    }

    /**
     * Sets the time zone for a document.
     */
    public static void setTimeZone(RMShape aShape, TimeZone aTimeZone)
    {
        // Handle RMDocument
        if (aShape instanceof RMDocument doc) {
            for (int i = 0, iMax = doc.getPageCount(); i < iMax; i++)
                setTimeZone(doc.getPage(i), aTimeZone);
        }

        // Handle RMText - iterate over xstring runs and reset date format time zones
        else if (aShape instanceof RMTextShape text) {
            RMXString xstring = text.getXString();
            for (int i = 0, iMax = xstring.getRunCount(); i < iMax; i++) {
                RMXStringRun run = xstring.getRun(i);
                if (run.getFormat() instanceof RMDateFormat dateFormat)
                    dateFormat.setTimeZone(aTimeZone);
            }
        }

        // Handle anything else
        else {

            // If shape has format, reset timezone
            if (aShape.getFormat() instanceof RMDateFormat dateFormat)
                dateFormat.setTimeZone(aTimeZone);

            // Recurse
            for (int i = 0, iMax = aShape.getChildCount(); i < iMax; i++)
                setTimeZone(aShape.getChild(i), aTimeZone);
        }
    }

    /**
     * Set PDF password.
     */
    public static void passwordReport()
    {
        Map<?,?> dataSet = new RMXMLReader().readObjectFromXmlUrl(getHollywoodURL(), null);
        RMDocument template = new RMDocument(getMoviesURL());
        RMDocument report = template.generateReport(dataSet);

        RMPDFWriter pdfWriter = new RMPDFWriter(); //rm.setUnmodifiable("Test");
        pdfWriter.setAccessPermissions("fluffy", "bunny", 2052);  // PDFEncryptor.PRINTING_ALLOWED, MAXIMUM_RESOLUTION_PRINTING
        try { FileUtils.writeBytes(new java.io.File("/Users/jeff/Desktop/Test2.pdf"), pdfWriter.getBytes(report)); }
        catch (java.io.IOException e) { throw new RuntimeException(e); }
    }

    /**
     * A studio class.
     */
    public record Studio(String name, float budget) { }

    /**
     * A movie class.
     */
    public record Movie(String title, Category category, Date showDate, String rating, double revenue, Studio studio, List<MovieRole> movieRoles) { }

    /**
     * Movie category constant type.
     */
    public enum Category { Adventure, Comedy, Drama, Thriller }

    /**
     * A movie role class.
     */
    public record MovieRole(String name, String actorName) { }

    /**
     * Returns a list of movies.
     */
    public static List<Movie> getMovies()
    {
        Studio studio = new Studio("Paramount Pictures", 500000);
        MovieRole m1r1 = new MovieRole("Woody", "Tom Hanks");
        MovieRole m1r2 = new MovieRole("Buzz Lightyear", "Tim Allen");
        Movie m1 = new Movie("Toy Story", Category.Comedy, new Date(), "G", 1_195_000, studio, List.of(m1r1, m1r2));
        MovieRole m2r1 = new MovieRole("Luke Skywalker", "Mark Hamill");
        MovieRole m2r2 = new MovieRole("Princess Leia", "Carrie Fisher");
        MovieRole m2r3 = new MovieRole("Han Solo", "Harrison Ford");
        Movie m2 = new Movie("Star Wars", Category.Adventure, new Date(), "PG", 1_345_000, studio, List.of(m2r1, m2r2, m2r3));
        return List.of(m1, m2);
    }

    /**
     * Writes a movie dataset to file.
     */
    static void writeMoviesDataset()
    {
        new RMXMLWriter().writeObject(getMovies(), "/tmp/Dataset.xml");
    }

    /**
     * Generates a sample report.
     */
    public static RMDocument generateSampleMoviesReport()
    {
        // Load template
        Object templateSource = getMoviesURL();
        RMDocument template = new RMDocument(templateSource);

        // Load dataset
        Object dataSet = getMovies();

        // Generate report and write
        return template.generateReport(dataSet);
    }

    /**
     * Generates a sample report and writes to PDF file.
     */
    public static void generateAndWriteSampleMoviesReport()
    {
        RMDocument report = generateSampleMoviesReport();
        report.write("/tmp/MoviesReport.pdf");
    }

    /**
     * Writes a movie dataset to file.
     */
    public static void main(String[] args)
    {
        writeMoviesDataset();
//        com.reportmill.app.RMViewerPane viewerPane = new com.reportmill.app.RMViewerPane();
//        RMDocument report = generateSampleMoviesReport();
//        viewerPane.getViewer().setDoc(report);
//        viewerPane.setWindowVisible(true);
    }
}