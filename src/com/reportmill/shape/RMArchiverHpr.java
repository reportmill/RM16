package com.reportmill.shape;
import com.reportmill.base.RMDateFormat;
import com.reportmill.base.RMNumberFormat;
import snap.geom.HPos;
import snap.gfx.*;
import snap.text.*;
import snap.util.XMLElement;
import java.util.Objects;

/**
 * This class handles archival for extra classes.
 */
public class RMArchiverHpr {

    /**
     * TextModel archival.
     */
    public static XMLElement textModelToXML(TextModel textModel, RMArchiver anArchiver)
    {
        // Get new element named xstring
        XMLElement xml = new XMLElement("xstring");

        // Declare loop variables for text attributes: TextTyle, LineStyle, Font, Color, Format, Outline, Underline, Scripting, CS
        TextStyle textStyle = textModel.getDefaultTextStyle();
        TextLineStyle lineStyle = textModel.getDefaultLineStyle();
        Font font = textStyle.getFont();
        Color color = textStyle.getColor();
        TextFormat format = textStyle.getFormat();
        Border border = null;
        int scripting = 0;
        double charSpacing = 0;
        boolean underline = false;

        // Iterate over runs
        for (TextLine line : textModel.getLines()) {
            for (TextRun run : line.getRuns()) {

                // If font changed for run, write font element
                if (!Objects.equals(font, run.getFont())) {
                    font = run.getFont();
                    xml.add(fontToXML(font));
                }

                // If color changed for run, write color
                if (!Objects.equals(color, run.getColor())) {
                    color = run.getColor();
                    xml.add(colorToXML(color));
                }

                // If format changed for run, write format
                if (!Objects.equals(format, run.getFormat())) {
                    format = run.getFormat();
                    if (format == null)
                        xml.add(new XMLElement("format"));
                    else if (format instanceof RMArchiver.Archivable archivableFormat)
                        xml.add(anArchiver.writeObjectToXml(archivableFormat));
                    else System.err.println("RMArchiver.textModelToXML: Format not archivable: " + format);
                }

                // If paragraph style changed for run, write paragraph
                if (!Objects.equals(lineStyle, line.getLineStyle())) {
                    lineStyle = line.getLineStyle();
                    xml.add(lineStyleToXML(lineStyle));
                }

                // If underline style changed, write underline
                if (underline != run.isUnderlined()) {
                    underline = run.isUnderlined();
                    xml.add(new XMLElement("underline"));
                    if (!underline)
                        xml.get(xml.size() - 1).add("style", -1);
                }

                // If border changed, write border
                if (!Objects.equals(border, run.getBorder())) {
                    border = run.getBorder();
                    xml.add(new XMLElement("TextBorder"));
                    if (border != null) {
                        if (border.getWidth() != 1)
                            xml.get(xml.size() - 1).add("stroke", border.getWidth());
                        if (border.getColor() != null)
                            xml.get(xml.size() - 1).add("color", "#" + border.getColor().toHexString());
                    }
                    else xml.get(xml.size() - 1).add("off", true);
                }

                // If scripting changed, write scripting
                if (scripting != run.getScripting()) {
                    scripting = run.getScripting();
                    XMLElement scriptingXML = new XMLElement("scripting");
                    if (scripting != 0)
                        scriptingXML.add("val", scripting);
                    xml.add(scriptingXML);
                }

                // If char spacing changed, write char spacing
                if (charSpacing != run.getCharSpacing()) {
                    charSpacing = run.getCharSpacing();
                    XMLElement charSpacingXML = new XMLElement("char-spacing");
                    charSpacingXML.add("value", charSpacing);
                    xml.add(charSpacingXML);
                }

                // Write run string
                if (!run.isEmpty())
                    xml.add(new XMLElement("string", run.getString()));
            }
        }

        // Return
        return xml;
    }

    /**
     * TextModel unarchival.
     */
    public static void textModelFromXML(TextModel textModel, RMArchiver anArchiver, XMLElement anElement)
    {
        // Get map for run attributes
        TextStyle style = textModel.getDefaultTextStyle();
        TextLineStyle lineStyle = null;

        // Iterate over child elements to snag common attributes
        for (XMLElement e : anElement.getElements()) {

            switch (e.getName()) {

                // Unarchive string
                case "string" -> {
                    String str = e.getValue();
                    if (str == null || str.isEmpty()) continue;
                    int len = textModel.length();
                    textModel.addCharsWithStyle(str, style);
                    if (lineStyle != null) {
                        textModel.setLineStyle(lineStyle, len, len + str.length());
                        lineStyle = null;
                    }
                }

                // Unarchive font element
                case "font" -> {
                    Font font = fontFromXML(e);
                    style = style.copyForStyleValue(font);
                }

                // Unarchive color element
                case "color" -> {
                    Color color = colorFromXML(e);
                    style = style.copyForStyleValue(color);
                }

                // If format changed for segment, write format
                case "format" -> {
                    Object fmt = anArchiver.readObjectFromXml(e, null);
                    style = style.copyForStyleKeyValue(TextStyle.Format_Prop, fmt);
                }

                // Unarchive pgraph element
                case "pgraph" -> lineStyle = lineStyleFromXML(e);

                // Unarchive underline element
                case "underline" -> {
                    if (e.getAttributeIntValue("style") < 0)
                        style = style.copyForStyleKeyValue(TextStyle.Underline_Prop, null);
                    else style = style.copyForStyleKeyValue(TextStyle.Underline_Prop, 1);
                }

                // Unarchive outline element
                case "outline" -> {
                    if (e.getAttributeBoolValue("off"))
                        style = style.copyForStyleKeyValue(TextStyle.Border_Prop, null);
                    else {
                        double strokeWidth = e.getAttributeFloatValue("stroke", 1);
                        String colorStr = e.getAttributeValue("color");
                        Color color = Color.get(colorStr);
                        Border border = Border.createLineBorder(style.getColor(), strokeWidth);
                        style = style.copyForStyleValue(border);
                        style = style.copyForStyleValue(color);
                    }
                }

                // Unarchive outline element
                case "TextBorder" -> {
                    double stroke = e.getAttributeFloatValue("stroke", 1);
                    String cstr = e.getAttributeValue("color");
                    Color color = Color.get(cstr);
                    Border border = Border.createLineBorder(color, stroke);
                    style = style.copyForStyleValue(border);
                }

                // Unarchive scripting
                case "scripting" -> {
                    int scripting = e.getAttributeIntValue("val");
                    style = style.copyForStyleKeyValue(TextStyle.Scripting_Prop, scripting);
                }

                // Unarchive char spacing
                case "char-spacing" -> {
                    double cspace = e.getAttributeFloatValue("value");
                    style = style.copyForStyleKeyValue(TextStyle.CharSpacing_Prop, cspace);
                }
            }
        }

        // If no string was read, apply attributes anyway
        // if (textModel.isEmpty()) textModel.getLine(0).getRun(0).setTextStyle(style);
    }

    /**
     * Font XML.
     */
    public static XMLElement fontToXML(Font aFont)
    {
        XMLElement e = new XMLElement("font");
        e.add("name", aFont.getNameEnglish());
        e.add("size", aFont.getSize());
        return e;
    }

    /**
     * Font unarchival.
     */
    public static Font fontFromXML(XMLElement anElement)
    {
        String name = anElement.getAttributeValue("name");
        double size = anElement.getAttributeFloatValue("size");
        return Font.getFont(name, size);
    }

    /**
     * Color archival.
     */
    public static XMLElement colorToXML(Color aColor)
    {
        XMLElement e = new XMLElement("color");
        e.add("value", "#" + aColor.toHexString());
        return e;
    }

    /**
     * Color unarchival.
     */
    public static Color colorFromXML(XMLElement anElement)
    {
        String color = anElement.getAttributeValue("color");
        if (color!=null) return new Color(color);

        String hex = anElement.getAttributeValue("value");
        int start = hex.charAt(0)=='#' ? 1 : 0;
        double red = Integer.decode("0x" + hex.substring(start, start + 2)) / 255f;
        double green = Integer.decode("0x" + hex.substring(start + 2, start + 4)) / 255f;
        double blue = Integer.decode("0x" + hex.substring(start + 4, start + 6)) / 255f;
        double alpha = 1;
        if (hex.length() >= start + 8)
            alpha = Integer.decode("0x" + hex.substring(start + 6, start + 8))/255f;
        return new Color(red, green, blue, alpha);
    }

    /**
     * TextLineStyle archival.
     */
    public static XMLElement lineStyleToXML(TextLineStyle lineStyle)
    {
        // Get new element named pgraph
        XMLElement e = new XMLElement("pgraph");

        // Archive AlignX, FirstIndent, LeftIndent, RightIndent
        HPos align = lineStyle.getAlign();
        String alignStr = lineStyle.isJustify() ? "full" : align.toString().toLowerCase();
        if (!alignStr.equals("left"))
            e.add("align", alignStr);

        double firstIndent = lineStyle.getFirstIndent();
        double leftIndent = lineStyle.getLeftIndent();
        double rightIndent = lineStyle.getRightIndent();
        if (firstIndent != leftIndent) {
            e.add("FirstIndent", firstIndent);
            e.add("left-indent-0", firstIndent);
        }
        if (leftIndent != 0)
            e.add("left-indent", leftIndent);
        if (rightIndent != 0)
            e.add("right-indent", rightIndent);

        // Archive Spacing, SpacingFactor, LineHeightMin, LineHeightMax, ParagraphSpacing
        if (lineStyle.getSpacing() != 0) e.add("line-gap", lineStyle.getSpacing());
        if (lineStyle.getSpacingFactor() != 1) e.add("line-space", lineStyle.getSpacingFactor());
        if (lineStyle.getMinHeight() != 0) e.add("min-line-ht", lineStyle.getMinHeight());
        if (lineStyle.getMaxHeight() != Float.MAX_VALUE) e.add("max-line-ht", lineStyle.getMaxHeight());
        if (lineStyle.getNewlineSpacing() != 0) e.add("pgraph-space", lineStyle.getNewlineSpacing());

        // Archive Tabs
        //double[] tabs = lineStyle.getTabs();
        //char[] tabTypes = lineStyle.getTabTypes();
        //if (!Arrays.equals(tabs, TextLineStyle.DEFAULT_TABS) || !Arrays.equals(tabTypes, TextLineStyle.DEFAULT_TAB_TYPES))
        //    e.add("tabs", lineStyle.getTabsString());

        // Return element
        return e;
    }

    /**
     * TextLineStyle unarchival.
     */
    public static TextLineStyle lineStyleFromXML(XMLElement anElement)
    {
        TextLineStyle lineStyle = TextLineStyle.DEFAULT;

        // Unarchive AlignX, FirstIndent, LeftIndent, RightIndent
        String alignStr = anElement.getAttributeValue("align", "left");
        if (alignStr.equals("full"))
            lineStyle = lineStyle.copyForPropKeyValue(TextLineStyle.Justify_Prop, true);
        else if (!alignStr.equals("left"))
            lineStyle = lineStyle.copyForAlign(HPos.get(alignStr));

        // Unarchive FirstIndent, LeftIndent, RightIndent
        //if (anElement.hasAttribute("FirstIndent"))
        //    _firstIndent = anElement.getAttributeDoubleValue("FirstIndent");
        //else if (anElement.hasAttribute("left-indent-0"))
        //    _firstIndent = anElement.getAttributeFloatValue("left-indent-0");
        //_leftIndent = anElement.getAttributeFloatValue("left-indent");
        //_rightIndent = anElement.getAttributeFloatValue("right-indent");

        // Archive Spacing, SpacingFactor, LineHeightMin, LineHeightMax, ParagraphSpacing
        if (anElement.hasAttribute("line-gap"))
            lineStyle = lineStyle.copyForPropKeyValue(TextLineStyle.Spacing_Prop, anElement.getAttributeFloatValue("line-gap"));
        if (anElement.hasAttribute("line-space"))
            lineStyle = lineStyle.copyForPropKeyValue(TextLineStyle.SpacingFactor_Prop, anElement.getAttributeFloatValue("line-space"));
        if (anElement.hasAttribute("min-line-ht"))
            lineStyle = lineStyle.copyForPropKeyValue(TextLineStyle.MinHeight_Prop, anElement.getAttributeFloatValue("min-line-ht"));
        if (anElement.hasAttribute("max-line-ht"))
            lineStyle = lineStyle.copyForPropKeyValue(TextLineStyle.MaxHeight_Prop, anElement.getAttributeFloatValue("max-line-ht"));
        if (anElement.hasAttribute("pgraph-space"))
            lineStyle = lineStyle.copyForPropKeyValue(TextLineStyle.NewlineSpacing_Prop, anElement.getAttributeFloatValue("pgraph-space"));

        // Unarchive Tabs
        //if (anElement.hasAttribute("tabs"))
        //    setTabsString(anElement.getAttributeValue("tabs"));

        // Return paragraph
        return lineStyle;
    }

    /**
     * A class to unarchive formats as proper subclass based on type attribute.
     */
    public static class RMFormatStub implements RMArchiver.Archivable {

        public XMLElement toXML(RMArchiver anArchive)  { return null; }

        public Object fromXML(RMArchiver anArchiver, XMLElement anElmnt)
        {
            String type = anElmnt.getAttributeValue("type", "");
            if (type.equals("number")) return anArchiver.readObjectFromXmlForClass(anElmnt, RMNumberFormat.class, null);
            if (type.equals("date")) return anArchiver.readObjectFromXmlForClass(anElmnt, RMDateFormat.class, null);
            if (!type.isEmpty()) System.err.println("RMFormatStub: Unknown format type " + type);
            return null;
        }
    }
}
