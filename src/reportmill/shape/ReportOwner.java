/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package reportmill.shape;
import reportmill.util.*;

import java.util.*;
import reportmill.util.RMHTMLParser;
import reportmill.util.RMRTFParser;
import rmdraw.scene.SGDoc;
import rmdraw.scene.SGParent;
import rmdraw.scene.SGView;
import snap.text.*;
import snap.util.*;
import snap.web.WebURL;

/**
 * A base class that loads and runs reports.
 */
public class ReportOwner implements RMKeyChain.Get {

    // The template
    SGDoc _template;
    
    // The string used to represent null values
    String           _nstring;

    // Whether to paginate
    boolean          _paginate = true;
    
    // The main model object
    Map              _model = new HashMap();
    
    // Objects from user's dataset to be queried for key substitution
    List             _dataStack = new ArrayList();
    
    // The list of objects passed to generateReport, if called with just a list
    List             _defaultList;
    
    // Shapes that contain page keys
    List             _pageRefShapes = new ArrayList();
    
    // Provides a hook for didFillShape notification
    ReportMill.Listener  _listener;

/**
 * Returns the template.
 */
public SGDoc getTemplate()
{
    return _template!=null? _template : (_template=createTemplate());
}

/**
 * Creates the template.
 */
protected SGDoc createTemplate()
{
    WebURL url = WebURL.getURL(getClass(), getClass().getSimpleName() + ".rpt");
    return SGDoc.getDocFromSource(url);
}

/**
 * Sets the template.
 */
public void setTemplate(SGDoc aDoc)
{
    _template = aDoc;
}

/**
 * Returns the string used to represent null values.
 */
public String getNullString()  { return _nstring; }

/**
 * Sets the string used to represent null values.
 */
public void setNullString(String aString)  { _nstring = aString; }

/**
 * Returns whether this reportmill paginates.
 */
public boolean getPaginate()  { return _paginate; }

/**
 * Sets whether this reportmill paginates.
 */
public void setPaginate(boolean aFlag)  { _paginate = aFlag; }

/**
 * Returns the model object.
 */
public Object getModel()  { return _model; }

/**
 * Sets objects in this reportmill.
 */
public void addModelObject(Object anObj)
{
    // Convert object to standard type
    Object obj = convertToStandardType(anObj); if(obj==null) return;
    
    // if object is ResultSet, convert to List of Maps
    if(obj instanceof java.sql.ResultSet) obj = RMSQLUtils.getMaps((java.sql.ResultSet)obj, 0);

    // If object is List, make it DefaultList
    if(obj instanceof List) //_model.put("RMDefaultObjectList", obj);
        _defaultList = (List)obj;
        
    // If object is Map, replace any ResultSets with List
    else if(obj instanceof Map) { Map map = (Map)obj; map = RMSQLUtils.getMapsDeep(map, 2);
        _model.putAll(map); }
    
    // If ReportMill.Listener set Listener
    else if(obj instanceof ReportMill.Listener) _listener = (ReportMill.Listener)obj;
        
    // Add objects and userInfo to DataStack
    else if(obj!=null)
        pushDataStack(obj);
    
    // If model map has value, make sure is at front of stack
    if(_model.size()>0 && (_dataStack.size()==0 || _dataStack.get(0)!=_model)) _dataStack.add(0, _model);
}

/**
 * Adds a data object to the data object list.
 */
public void pushDataStack(Object anObj)  { _dataStack.add(anObj); }

/**
 * Removes a specific object at given index in list.
 */
public Object popDataStack()  { return _dataStack.remove(_dataStack.size()-1); }

/**
 * Returns the last data object in the data objects list.
 */
public Object peekDataStack()  { return ListUtils.getLast(_dataStack); }

/**
 * Generates the report.
 */
public RMDoc generateReport()
{
    // If objects and user info is null, add a bogus object so keychain assignments will work (probably silly)
    if (_dataStack.size()==0)
        addModelObject(new Object());

    // Generate report and return
    SGDoc doc = getTemplate();
    RMDoc report = (RMDoc)rpg(doc, null);
    return report;
}

/**
 * Performs RPG on a given shape.
 */
public SGView rpg(SGView aShape, SGView aParent)
{
    SGView rpg = ReportGen.rpgAllFor(aShape, this, aParent);
    if(_listener!=null) _listener.didFillShape(aShape, rpg);
    return rpg;
}

/**
 * Returns the list of page reference shapes.
 */
public List <SGView> getPageReferenceShapes()  { return _pageRefShapes; }

/**
 * Sets the list of page reference shapes.
 */
public void setPageReferenceShapes(List aList)  { _pageRefShapes = aList; }

/**
 * Registers a shape with a page key in it.
 */
public void addPageReferenceShape(SGView aShape)  { ListUtils.addUniqueId(_pageRefShapes, aShape); }

/**
 * RMKeyChain.Get implementation to run against DataStack.
 */
public Object getKeyChainValue(Object aRoot, RMKeyChain aKeyChain)
{
    // If Op is Key, Chain or FunctionCall, evaluate against DataStack objects
    RMKeyChain.Op op = aKeyChain.getOp();
    if(op==RMKeyChain.Op.Key || op==RMKeyChain.Op.Chain || op==RMKeyChain.Op.FunctionCall) {

        // If FunctionCall, try it on 'this' first to keep broader scoping (works for static functions only)
        if(op==RMKeyChain.Op.FunctionCall) {
            Object val = RMKeyChain.getValueImpl(aRoot, this, aKeyChain);
            if(val!=null)
                return val;
        }
        
        // Check for "Root" key (evaluates key remainder on DataStack root)
        if(op== RMKeyChain.Op.Chain) { String key = aKeyChain.getChildString(0);
            if(key.equals("Root")) {
                Object dso = _dataStack.get(0), val = RMKeyChain.getValue(aRoot, dso, aKeyChain.subchain(1));
                if(val!=null)
                    return val;
            }
        }
        
        // Try to evaluate KeyChain against DataStack objects
        for(int i=_dataStack.size()-1; i>=0; i--) { Object dso = _dataStack.get(i);
            Object val = RMKeyChain.getValue(aRoot, dso, aKeyChain);
            if(val!=null)
                return val;
        }

        // If Key wasn't evaluated above, see if it is a special ReportMill key
        if(op==RMKeyChain.Op.Key) { String key = aKeyChain.getValueString();
            if(key.equals("Date")) return new Date();  // Date key
            else if(key.equals("PageBreakPage")) System.err.println("Need to fix PageBreakPage"); // PageBreak keys
            else if(key.startsWith("RM")) return getRMKey(key); // RM Keys
        }
        
        // Return null since KeyChain value not found
        return null;
    }
    
    // If Op not a Key or FunctionCall, do normal version
    return RMKeyChain.getValueImpl(aRoot, this, aKeyChain);
}

/**
 * Returns a list for the given keychain.
 */
public List getKeyChainListValue(String aKeyChain)
{
    // Call RMKeyChain.getListValue on DataStack first item
    List list = RMKeyChain.getListValue(peekDataStack(), aKeyChain);
    
    // If list not found, call RMKeyChain.getListValue on Model
    if(list==null && peekDataStack()!=_model)
        list = RMKeyChain.getListValue(_model, aKeyChain);
        
    // If list not found, use DefaultList (might also be null). Return list.
    if(list==null)
        list = _defaultList;
    return list;
}

/**
 * Called by various objects to convert objects to generic types.
 */
protected Object convertToStandardType(Object anObj)
{
    if(ReportMill.appServer!=null) return ReportMill.appServer.convertFromAppServerType(anObj);
    if(anObj instanceof Set) return new ArrayList((Set)anObj); // If object is Set, return List
    else if(anObj instanceof Object[]) return Arrays.asList((Object[])anObj); // If object is array, return List
    return anObj; // Return object
}

/**
 * Performs page substitutions on any text fields that were identified as containing @Page@ keys.
 */
public void resolvePageReferences()
{
    // Iterate over page reference shapes and have them resolve
    List <SGView> prshapes = getPageReferenceShapes();
    for(int i=0, iMax=prshapes.size(); i<iMax; i++) { SGView shape = prshapes.get(i);
        
        // Create page info map
        Map info = new HashMap();
        info.put("Page", shape.page());
        info.put("PageMax", shape.pageMax());
        info.put("PageBreak", shape.getPageBreak());
        info.put("PageBreakMax", shape.getPageBreakMax());
        info.put("PageBreakPage", shape.getPageBreakPage());
        info.put("PageBreakPageMax", shape.getPageBreakPageMax());
        
        // Resolve page references with page info map
        ReportGen.resolvePageReferencesFor(shape, this, info);
    }
    
    // Clear page reference shapes list
    prshapes.clear();
}

/**
 * Clones a RichText.
 */
public RichText rpgCloneRichText(RichText aRichText, Object userInfo, SGView aShape, boolean doCopy)
{
    return rpgCloneRichText(aRichText, this, userInfo, aShape, doCopy);
}

/**
 * Clones a RichText.
 */
public RichText rpgCloneRichText(RichText aRichText, ReportOwner anRptOwner, Object userInfo, SGView aShape, boolean doCopy)
{
    // Declare local variable for resulting out-rtext and for whether something requested a recursive RPG run
    RichText outString = aRichText;
    boolean redo = false;

    // If userInfo provided, plug it into ReportMill
    if (userInfo!=null && anRptOwner!=null)
        anRptOwner.pushDataStack(userInfo);

    // Get range for first key found in string
    Range totalKeyRange = nextKeyRangeAfterIndex(outString,0, new Range());

    // While the inString still contains @key@ constructs, do substitution
    while(totalKeyRange.length() > 0) {

        // Get key start location (after @-sign) and length
        int keyLoc = totalKeyRange.start + 1;
        int keyLen = totalKeyRange.length() - 2;
        Object valString = null;

        // Get the run at the given location
        RichTextLine keyLine = outString.getLineAt(keyLoc);
        RichTextRun keyRun = outString.getRunAt(keyLoc);

        // If there is a key between the @-signs, evaluate it for substitution string
        if (keyLen > 0) {

            // Get actual key string
            String keyString = outString.subSequence(keyLoc, keyLoc + keyLen).toString();

            // Get key string as key chain
            RMKeyChain keyChain = RMKeyChain.getKeyChain(keyString);

            // If keyChain hasPageReference, tell reportMill and skip this key
            if (aShape!=null && keyChain.hasPageReference()) {
                anRptOwner.addPageReferenceShape(aShape);
                nextKeyRangeAfterIndex(outString, totalKeyRange.end, totalKeyRange);
                continue;
            }

            // Get keyChain value
            Object val = RMKeyChain.getValue(anRptOwner, keyChain);

            // If val is list, replace with first value (or null)
            if (val instanceof List) { List list = (List)val;
                val = list.size()>0? list.get(0) : null; }

            // If we found a String, then we'll just use it for key sub (although we to see if it's a KeyChain literal)
            if (val instanceof String) {

                // Set string value to be substitution string
                valString = val;

                // If keyChain has a string literal, check to see if val is that string literal
                if (keyChain.hasOp(RMKeyChain.Op.Literal) && !StringUtils.startsWithIC((String)val, "<html")) {
                    String string = val.toString();
                    int index = keyString.indexOf(string);

                    // If val is that string literal, get original RichText substring (with attributes)
                    if (index>0 && keyString.charAt(index-1)=='"' && keyString.charAt(index+string.length())=='"') {
                        int start = index + keyLoc;
                        valString = outString.subtext(start, start + string.length());
                        redo = redo || string.contains("@");
                    }
                }
            }

            // If we found an RichText, then we'll just use it for key substitution
            else if (val instanceof RichText)
                valString = val;

                // If we found a keyChain, add @ signs and redo (this feature lets developers return an RMKeyChain)
            else if (val instanceof RMKeyChain) {
                valString = "@" + val.toString() + "@"; redo = true; }

            // If val is Number, get format and change val to string (verify format type)
            else if (val instanceof Number) {
                TextFormat format = keyRun.getFormat();
                if (!(format instanceof RMNumberFormat))
                    format = RMNumberFormat.PLAIN;
                valString = format.format(val);
                TextStyle style = format.formatStyle(val);
                if (style!=null)
                    valString = new RichText((String)valString, style.getColor());
            }

            // If val is Date, get format and change val to string (verify format type)
            else if (val instanceof Date) {
                TextFormat format = keyRun.getFormat();
                if (!(format instanceof RMDateFormat))
                    format = RMDateFormat.defaultFormat;
                valString = format.format(val);
            }

            // If value is null, either use current format's or Document's NullString
            else if (val==null) {
                TextFormat fmt = keyRun.getFormat();
                if (fmt != null)
                    valString = fmt.format(val);
            }

            // If object is none of standard types (Str, Num, Date, XStr or null), see if it will provide bytes
            else {

                // Ask object for "bytes" method or attribute
                Object bytes = RMKey.getValue(val, "bytes");

                // If bytes is byte array, just set it
                if (bytes instanceof byte[])
                    valString = new String((byte[])bytes);

                    // If value is List, reset it so we don't get potential hang in toString
                else if (val instanceof List)
                    valString = "<List>";

                    // If value is Map, reset to "Map" so we don't get potential hang in toString
                else if (val instanceof Map)
                    valString = "<Map>";

                    // Set substitution value to string representation of provided object
                else valString = val.toString();
            }

            // If substitution string is still null, replace it with document null-string
            if (valString == null)
                valString = anRptOwner.getNullString()!=null? anRptOwner.getNullString() : "";
        }

        // If there wasn't a key between '@' signs, assume they wanted '@'
        else valString = "@";

        // If substitution string was found, perform substitution
        if (valString != null) {

            // If this is the first substitution, get a copy of outString
            if (outString==aRichText && doCopy)
                outString = aRichText.clone();

            // If substitution string was raw string, perform replace (and possible rtf/html evaluation)
            if (valString instanceof String) { String string = (String)valString;

                // If string is HTML formatted text, parse into RichText
                if (StringUtils.startsWithIC(string, "<html"))
                    valString = RMHTMLParser.parse(string, keyRun.getFont(), keyLine.getLineStyle());

                    // If string is RTF formatted text, parse into RichText
                else if (string.startsWith("{\\rtf"))
                    valString = RMRTFParser.parse(string, keyRun.getFont());

                    // If string is normal string, just perform replace and update key range
                else {
                    outString.replaceChars(string, totalKeyRange.start, totalKeyRange.end);
                    totalKeyRange.setLength(((String)valString).length());
                }
            }

            // If substitution string is RichText, just do RichText replace
            if (valString instanceof RichText) { RichText rtext = (RichText)valString;
                outString.replaceText(rtext, totalKeyRange.start, totalKeyRange.end);
                totalKeyRange.setLength(rtext.length());
            }
        }

        // Get next totalKeyRange
        nextKeyRangeAfterIndex(outString, totalKeyRange.end, totalKeyRange);
    }

    // If userInfo was provided, remove it from ReportMill
    if (userInfo!=null)
        anRptOwner.popDataStack();

    // If something requested a recursive RPG run, do it
    if (redo)
        outString = rpgCloneRichText(outString, anRptOwner, userInfo, aShape, false);

    // Return RPG string
    return outString;
}

/**
 * Returns the range of the next occurrence of @delimited@ text.
 */
private static Range nextKeyRangeAfterIndex(RichText rtext, int anIndex, Range aRange)
{
    // Get length of string (return bogus range if null)
    int length = rtext.length();
    if (length<2) return aRange.set(-1, -1);

    // Get start of key (return if it is the last char)
    int startIndex = rtext.indexOf("@", anIndex);
    if (startIndex==length-1)
        return aRange.set(startIndex, startIndex+1);

    // If startRange of key was found, look for end
    if (startIndex>=0) {
        int nextIndex = startIndex;
        while (++nextIndex < length) { char c = rtext.charAt(nextIndex);
            if (c=='"')
                while ((++nextIndex<length) && (rtext.charAt(nextIndex)!='"'));
            else if (c=='@')
                return aRange.set(startIndex, nextIndex+1);
        }
    }

    // Set bogus range and return
    return aRange.set(-1, -1);
}

/**
 * A range class.
 */
private static class Range {
    int start, end;
    public int length()  { return end - start; }
    public void setLength(int aLength)  { end = start + aLength; }
    public Range set(int aStart, int anEnd)  { start = aStart; end = anEnd; return this; }
}

/**
 * Returns a value for some silly RM defined keys.
 */
private Object getRMKey(String key)
{
    if (key.equals("RMRandom"))
        return MathUtils.randomInt();
    if (key.equals("RMVersion"))
        return String.format("ReportMill %f (Build Date: %s)", ReportMill.getVersion(), ReportMill.getBuildInfo());
    if (key.equals("RMUser")) return System.getProperty("user.name");
    if (key.equals("RMUserHome")) return System.getProperty("user.home");
    if (key.equals("RMProps")) return System.getProperties().toString();
    if (key.equals("RMJeff")) return "Jeffrey James Martin";
    if (key.equals("RMLogo")) return "http://mini.reportmill.com/images/RM-Logo.gif";
    if (key.equals("RMHTML")) return "<html><b>Howdy Doody</b></html>";
    if (key.equals("RMJapanese"))
        return "\u3053\u3093\u306b\u3061\u306f\u3001\u4e16\u754c\u306e\u4eba\u3005\uff01";
    if (key.equals("RMFlowers"))
        return WebURL.getURL(getClass(), "/snap/viewx/pkg.images/tulips.jpg");
    return null;
}

/**
 * A shape class to represent multiple pages of shapes.
 */
public static class ShapeList extends SGParent {
    public int removeChild(SGView aChild)  { return -1; }
}

}