/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.util;
import reportmill.shape.RMDoc;
import reportmill.shape.RMTable;
import reportmill.shape.RMTableGroup;
import reportmill.out.RMPDFWriter;
import rmdraw.scene.*;
import snap.geom.Rect;
import snap.text.*;
import snap.util.FileUtils;
import snap.util.MapUtils;
import snap.web.WebURL;
import java.util.*;

/**
 * This file is just meant to hold various utility methods that customers have asked for.
 */
public class RMExtras {
    
/**
 * Returns the Hollywood db URL.
 */
public static WebURL getHollywoodURL()
{
    return WebURL.getURL(RMExtras.class, "/reportmill/examples/HollywoodDB.xml");
}

/**
 * Returns the Movies Rpt URL.
 */
public static WebURL getMoviesURL()
{
    return WebURL.getURL(RMExtras.class, "/reportmill/examples/Movies.rpt");
}

/**
 * Iterates over all document (or shape) text and replaces occurrences of the first string with the second.
 */
public static void replaceText(SGView aShape, String aString1, String aString2)
{
    // Handle RMTextShape
    if(aShape instanceof SGText) { SGText text = (SGText)aShape;
        RichText richText = text.getRichText();
        String string = richText.getString();
        for(int i=string.indexOf(aString1); i>=0; i=string.indexOf(aString1, i)) {
            richText.replaceChars(aString2, i, i+aString1.length());
            string = richText.getString(); i += aString1.length();
        }
    }
    
    // Handle anything else
    else for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++) { SGView child = aShape.getChild(i);
        replaceText(child, aString1, aString2); }
}

/**
 * Replaces any dataset key in template that matches first given dataset key with the second given dataset key.
 */
public static void replaceDatasetKey(SGView aShape, String aKey1, String aKey2)
{
    // If shape is table, check table dataset key, and replace if found
    if(aShape instanceof RMTable) { RMTable table = (RMTable)aShape;
        if(table.getDatasetKey().equals(aKey1))
            table.setDatasetKey(aKey2); }

    // If shape is document, recursively call replace on pages
    else if(aShape instanceof SGDoc) { SGDoc doc = (SGDoc)aShape;
        for(int i=0; i<doc.getPageCount(); i++)
            replaceDatasetKey(doc.getPage(i), aKey1, aKey2); }
    
    // If shape is a table group, recursively call all top-level tables
    else if(aShape instanceof RMTableGroup) { RMTableGroup tableGroup = (RMTableGroup)aShape;
        for(int i=0; i<tableGroup.getChildTableCount(); i++)
            replaceDatasetKey(tableGroup.getChildTable(i), aKey1, aKey2); }
    
    // If shape is anything else, recursively call replaceSort on children
    else for(int i=0; i<aShape.getChildCount(); i++)
        replaceDatasetKey(aShape.getChild(i), aKey1, aKey2);
}

/**
 * Replaces any grouping key in the template that matches first given key with the second given key.
 */
public static void replaceGroupingKey(SGView aShape, String aKey1, String aKey2)
{
    // If shape is table, check table grouping's sorts for sort, and replace if found
    if(aShape instanceof RMTable) { RMTable table = (RMTable)aShape;

        // Iterate over table groupings
        for(int i=0; i<table.getGroupingCount(); i++) { RMGrouping grouping = table.getGrouping(i);
            
            // If grouping key matches given key, rename header, details and summary titles and set new key
            if(grouping.getKey().equals(aKey1)) {
                table.setTitleForChild(aKey1 + " Header", aKey2 + " Header");
                table.setTitleForChild(aKey1 + " Details", aKey2 + " Details");
                table.setTitleForChild(aKey1 + " Summary", aKey2 + " Summary");
                grouping.setKey(aKey2);
            }
        }
    }

    // If shape is document, recursively call replace on pages
    else if(aShape instanceof SGDoc) { SGDoc doc = (SGDoc)aShape;
        for(int i=0; i<doc.getPageCount(); i++)
            replaceGroupingKey(doc.getPage(i), aKey1, aKey2); }
    
    // If shape is a table group, recursively call all top-level tables
    else if(aShape instanceof RMTableGroup) { RMTableGroup tableGroup = (RMTableGroup)aShape;
        for(int i=0; i<tableGroup.getChildTableCount(); i++)
            replaceGroupingKey(tableGroup.getChildTable(i), aKey1, aKey2); }
    
    // If shape is anything else, recursively call replaceSort on children
    else for(int i=0; i<aShape.getChildCount(); i++)
        replaceGroupingKey(aShape.getChild(i), aKey1, aKey2);
}

/**
 * Replaces any sort in the template that matches first given sort with the second given sort.
 */
public static void replaceSort(SGView aShape, String aSort1, String aSort2)
{
    // If shape is table, check table grouping's sorts for sort, and replace if found
    if(aShape instanceof RMTable) { RMTable table = (RMTable)aShape;

        // Iterate over table groupings, and grouping sorts - if sort key matches, replace
        for(int i=0; i<table.getGroupingCount(); i++) { RMGrouping grouping = table.getGrouping(i);
            for(int j=0; j<grouping.getSortCount(); j++) { RMSort sort = grouping.getSort(j);
                if(sort.getKey().equals(aSort1))
                    grouping.getSorts().set(j, new RMSort(aSort2)); }
        }
    }

    // If shape is document, recursively call replace on pages
    else if(aShape instanceof SGDoc) { SGDoc doc = (SGDoc)aShape;
        for(int i=0; i<doc.getPageCount(); i++)
            replaceSort(doc.getPage(i), aSort1, aSort2); }
    
    // If shape is a table group, recursively call all top-level tables
    else if(aShape instanceof RMTableGroup) { RMTableGroup tableGroup = (RMTableGroup)aShape;
        for(int i=0; i<tableGroup.getChildTableCount(); i++)
            replaceSort(tableGroup.getChildTable(i), aSort1, aSort2); }
    
    // If shape is anything else, recursively call replaceSort on children
    else for(int i=0; i<aShape.getChildCount(); i++)
        replaceSort(aShape.getChild(i), aSort1, aSort2);
}

/**
 * Replaces a format.
 */
public static void replaceFormat(SGView aShape, TextFormat aFormat)
{
    // Handle document
    if(aShape instanceof SGDoc) { SGDoc document = (SGDoc)aShape;
        for(SGPage page : document.getPages())
            replaceFormat(page, aFormat); }
    
    // Handle anything with children
    else if(aShape.getChildCount()>0)
        for(SGView child : aShape.getChildren())
            replaceFormat(child, aFormat);
    
    // Handle Text
    else if(aShape instanceof SGText) { SGText text = (SGText)aShape;
        RichText richText = text.getRichText();
        for (RichTextLine line : richText.getLines())
            for (RichTextRun run : line.getRuns())
                if (run.getFormat()!=null && run.getFormat().getClass()==aFormat.getClass())
                    richText.setStyleValue(TextStyle.FORMAT_KEY, aFormat, run.getStart(), run.getEnd());
    }
    
    // Handle anything else
    else if(aShape.getFormat()!=null && aShape.getFormat().getClass()==aFormat.getClass())
        aShape.setFormat(aFormat);
}

/**
 * Returns a StringBuffer image map for a given shape.
 */
public static StringBuffer getImageMap(SGView aShape, StringBuffer aSB)
{
    // Get string buffer (create if missing)
    StringBuffer sb = aSB!=null? aSB : new StringBuffer("<MAP>");

    // If URL, add map area
    if(aShape.getURL()!=null) {
        Rect bounds = aShape.localToParent(aShape.getBoundsLocal(), aShape.getPage()).getBounds();
        sb.append("<AREA SHAPE=RECT COORDS=\"");
        sb.append(String.format("%d,%d,%d,%d\" ", (int)bounds.x, (int)bounds.y, (int)bounds.width, (int)bounds.height));
        sb.append(String.format("HREF=\"%s\" ", aShape.getURL()));
        sb.append(">\n");
    }

    // Recurse into children
    for(int i=0; i<aShape.getChildCount(); i++)
        getImageMap(aShape.getChild(i), sb);

    // If top-level, close map
    if(aSB==null) {
        sb.append("</MAP>");
        System.out.println(sb);
    }

    // Return buffer
    return sb;
}

/**
 * Prints name of every shape in a template hierarchy.
 */
public static void printNames(SGView aShape)
{
    // Print shape name
    System.out.println(aShape.getClass().getName() + ": " + aShape.getName());
        
    // If shape is document, recursively call replace on pages
    if(aShape instanceof SGDoc) { SGDoc doc = (SGDoc)aShape;
        for(int i=0; i<doc.getPageCount(); i++)
            printNames(doc.getPage(i)); }
    
    // If shape is a table group, recursively call all top-level tables
    else if(aShape instanceof RMTableGroup)
        printNames((RMTableGroup)aShape, null);
    
    // If shape is anything else, recursively call replaceSort on children
    else for(int i=0; i<aShape.getChildCount(); i++)
        printNames(aShape.getChild(i));
}

/**
 * Prints name of every shape in table group hierarchy.
 */
public static void printNames(RMTableGroup aTableGroup, RMTable aTable)
{
    // Get table or table group
    SGView tableOrTableGroup = aTable==null ? aTableGroup : aTable;
    
    // Print table (or tableGroup) name
    System.out.println(tableOrTableGroup.getClass().getName() + ": " + tableOrTableGroup.getName());
    
    // Iterate over child tables for given table and recurse into standard printNames and table group print names
    for(int i=0; i<aTableGroup.getChildTableCount(aTable); i++) {
        RMTable table = aTableGroup.getChildTable(aTable, i);
        printNames(table);
        printNames(aTableGroup, table);
    }
}

/**
 * Adds the contents from second document to the bottom of the first page of first document.
 */
public static void addToPage(SGDoc aDoc1, SGDoc aDoc2)
{
    // Get page 1 & 2 from doc 1 & 2
    SGPage page1 = aDoc1.getPage(0);
    SGPage page2 = aDoc2.getPage(0);
    
    // Get copy of page 2 child list
    SGView children[] = page2.getChildArray();
    
    // Iterate over page 2 children, add to page 1 and shift to bottom of page
    for(SGView child : children) {
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
    RMDoc template = RMDoc.getDocFromSource(getMoviesURL());
    
    // Get objects
    RMDataSource dataSource = template.getDataSource();
    Schema schema = dataSource!=null ? dataSource.getSchema() : null;
    Map map = new RMXMLReader().readObject(getHollywoodURL(), schema);
    
    // Generate report
    SGDoc report = template.generateReport(map);
    
    // Resolve page references
    report.resolvePageReferences();
    
    // Create notice page
    SGPage page = report.createPage();
    SGText text = new SGText("General Billing Information");
    text.setBounds(72,72,300,100);
    page.addChild(text);
    
    // Iterate over report
    for(int i=report.getPageCount(); i>0; i--)
        report.addPage((SGPage)page.cloneDeep(), i);
    
    // Write report
    report.write("/tmp/Report.pdf");
}

/**
 * Sets the time zone for a document.
 */
public static void setTimeZone(SGView aShape, TimeZone aTimeZone)
{
    // Handle Doc
    if(aShape instanceof SGDoc) { SGDoc doc = (SGDoc)aShape;
        for(int i=0, iMax=doc.getPageCount(); i<iMax; i++)
            setTimeZone(doc.getPage(i), aTimeZone); }
    
    // Handle RMText - iterate over xstring runs and reset date format time zones
    else if(aShape instanceof SGText) { SGText text = (SGText)aShape;
        RichText richText = text.getRichText();
        for (RichTextLine line : richText.getLines())
            for (RichTextRun run : line.getRuns())
                if (run.getFormat() instanceof RMDateFormat)
                    ((RMDateFormat)run.getFormat()).setTimeZone(aTimeZone);
    }
    
    // Handle anything else
    else {
        
        // If shape has format, reset timezone
        if(aShape.getFormat() instanceof RMDateFormat)
            ((RMDateFormat)aShape.getFormat()).setTimeZone(aTimeZone);
        
        // Recurse
        for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++)
            setTimeZone(aShape.getChild(i), aTimeZone);
    }
}

/**
 * Set PDF password.
 */
public static void passwordReport()
{
    Map dset = new RMXMLReader().readObject(getHollywoodURL());
    RMDoc template = RMDoc.getDocFromSource(getMoviesURL());
    RMDoc report = template.generateReport(dset);
    
    RMPDFWriter rm = new RMPDFWriter(); //rm.setUnmodifiable("Test");
    rm.setAccessPermissions("fluffy", "bunny", 2052);  // PDFEncryptor.PRINTING_ALLOWED, MAXIMUM_RESOLUTION_PRINTING
    try {
        byte bytes[] = rm.getBytes(report);
        FileUtils.writeBytes(new java.io.File("/Users/jeff/Desktop/Test2.pdf"), bytes);
    }
    catch(java.io.IOException e) { throw new RuntimeException(e); }
}

/** A movie class. */
public static class Studio {
    String name;
    float budget;
    public Studio(String aName, float aBudget)  { name = aName; budget = aBudget; }
    public String getName()  { return name; }
    public float getBudget()  { return budget; }
}

/** A movie class. */
public static class Movie {
    String title;
    Date showDate;
    String rating;
    Category category;
    Studio studio;
    List <MovieRole> movieRoles;
    public Movie(String aTitle, Category aCat, Date aDate, String aRat, Studio aStudio, List<MovieRole> theRoles) {
        title = aTitle; category = aCat; showDate = aDate; rating = aRat; studio = aStudio; movieRoles = theRoles; }
    public String getTitle()  { return title; }
    public Category getCategory()  { return category; }
    public Date getShowDate()  { return showDate; }
    public String getRating()  { return rating; }
    public Studio getStudio()  { return studio; }
    public List getStudio2()  { return Arrays.asList(MapUtils.newMap("Studio", studio, "ListType", "XJ-27")); }
    public List <MovieRole> getMovieRoles()  { return movieRoles; }
}

/** Movie category constant type. */
public enum Category { Adventure, Comedy, Drama, Thriller }

/** A movie role class. */
public static class MovieRole {
    String name;
    String actorName;
    public MovieRole(String aName, String anActorName)  { name = aName; actorName = anActorName; }
    public String getName()  { return name; }
    public String getActorName()  { return actorName; }
}

/** Returns a list of movies. */
public static List <Movie> getMovies()
{
    Studio studio = new Studio("Paramount Pictures", 500000);
    MovieRole m1r1 = new MovieRole("Woody", "Tom Hanks");
    MovieRole m1r2 = new MovieRole("Buzz Lightyear", "Tim Allen");
    Movie m1 = new Movie("Toy Story", Category.Comedy, new Date(), "G", studio, Arrays.asList(m1r1, m1r2));
    MovieRole m2r1 = new MovieRole("Luke Skywalker", "Mark Hamill");
    MovieRole m2r2 = new MovieRole("Princess Leia", "Carrie Fisher");
    MovieRole m2r3 = new MovieRole("Han Solo", "Harrison Ford");
    Movie m2 = new Movie("Star Wars", Category.Adventure, new Date(), "PG", studio, Arrays.asList(m2r1, m2r2, m2r3));
    return Arrays.asList(m1, m2);
}

/** Writes a movie dataset to file. */
static void writeMoviesDataset() { new RMXMLWriter().writeObject(getMovies(), "/tmp/Dataset.xml"); }

/** Generates a report. */
static void genReport()
{
    RMDoc doc = RMDoc.getDocFromSource("/tmp/junk.rpt");
    RMDoc rep = doc.generateReport(getMovies());
    rep.write("/tmp/junk.pdf");
}

/** Writes a movie dataset to file. */
public static void main(String args[])
{
    writeMoviesDataset();
}

}