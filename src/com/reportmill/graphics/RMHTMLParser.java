/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import java.io.*;
import java.util.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;
import snap.geom.HPos;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.TextLineStyle;
import snap.text.TextModel;
import snap.text.TextStyle;
import snap.util.*;

/**
 * This class provides methods to create an xstring from HTML and RTF.
 */
public class RMHTMLParser {

    // A map of color names & their hex values - used for translating common color names to RGB
    private static Map<String,String> _colorNames;

    // An array of symbol maps for unicode mapping
    private static SymMap[] _symMap = SymMap.symMap();

    // An array of symbol chars
    private static char[] _symChars = SymMap.symChars();

    /**
     * Returns an xstring for the given html string and a default font.
     */
    public static TextModel parse(String html, Font baseFont, TextLineStyle textLineStyle)
    {
        // Get HTML String from HTMLParser
        TextModel textModel = new HTMLParser(html, baseFont, textLineStyle)._textModel;

        // Find the start of any trailing whitespace characters in string HTML string
        int whitespaceStart = textModel.length();
        while (whitespaceStart > 0 && Character.isWhitespace(textModel.charAt(whitespaceStart - 1)))
            whitespaceStart--;

        // If there are trailing whitespace characters in string, strip them from xstring
        if (whitespaceStart < textModel.length())
            textModel.removeChars(whitespaceStart, textModel.length());

        // Return string
        return textModel;
    }

    /**
     * An inner class to turn HTML into RMXString (Uses Swing's HTMLEditorKit.Parser).
     */
    private static class HTMLParser extends HTMLEditorKit.ParserCallback {

        // The text model resulting from parsing HTML
        TextModel _textModel = TextModel.createDefaultTextModel(true);

        // The current text style
        TextStyle _textStyle = TextStyle.DEFAULT;

        // The current line style attributes
        TextLineStyle _textLineStyle;

        // The current stack of fonts (during parsing)
        List<Font> _fontStack = new ArrayList<>();

        // The number of list elements that we have parsed into (during parsing)
        int _listLevel = 0;

        // Whether we are currently parsing symbol characters (during parsing)
        boolean _isSymbol = false;

        /**
         * Creates a new parser for an html string and a default font.
         */
        public HTMLParser(String aString, Font baseFont, TextLineStyle textLineStyle)
        {
            // Initialize attributes map, FontStack and PGraph
            if (!_textStyle.getFont().equals(baseFont))
                _textStyle = _textStyle.copyForStyleValue(baseFont);
            _fontStack.add(baseFont);
            _textLineStyle = textLineStyle != null ? textLineStyle : TextLineStyle.DEFAULT;

            // Convert known character entity references to unicode chars
            String s2 = aString;
            if (aString.indexOf(';') > aString.indexOf('&')) {
                for (SymMap symMap : _symMap) {
                    aString = aString.replace(symMap.ce, symMap.str);
                    if (aString != s2) {
                        if (aString.indexOf(';') > aString.indexOf('&'))
                            s2 = aString;
                        else break;
                    }
                }
            }

            // Parse html
            try { new ParserDelegator().parse(new StringReader(aString), this, true); }

            // Catch exceptions
            catch (Exception e) { e.printStackTrace(); }
        }

        /**
         * Called for simple HTML tags (like <BR>).
         */
        public void handleSimpleTag(HTML.Tag aTag, MutableAttributeSet anAttributeSet, int pos)
        {
            // Handle line break (<br>)
            if (aTag.equals(HTML.Tag.BR))
                _textModel.addChars("\n");
        }

        /**
         * Called when new HTML tag start is encountered.
         */
        public void handleStartTag(HTML.Tag aTag, MutableAttributeSet anAttributeSet, int pos)
        {
            // Handle Bold (<B> and <STRONG>)
            if (aTag.equals(HTML.Tag.B) || aTag.equals(HTML.Tag.STRONG)) {
                Font font = _textStyle.getFont();
                Font boldFont = font.getBold() == null ? font : font.getBold();
                _textStyle = _textStyle.copyForStyleValue(boldFont);
                _fontStack.add(boldFont);
            }

            // Handle Italic (<I> and <EM>)
            if (aTag.equals(HTML.Tag.I) || aTag.equals(HTML.Tag.EM)) {
                Font font = _textStyle.getFont();
                Font italicFont = font.getItalic() == null ? font : font.getItalic();
                _textStyle = _textStyle.copyForStyleValue(italicFont);
                _fontStack.add(italicFont);
            }

            // Handle Underline (<U>)
            if (aTag.equals(HTML.Tag.U))
                _textStyle = _textStyle.copyForStyleKeyValue(TextStyle.Underline_Prop, 1);

            // Handle List start (<UL> and <OL>)
            if (aTag.equals(HTML.Tag.UL) || aTag.equals(HTML.Tag.OL)) {
                _listLevel++;
                if (!_textModel.getString().endsWith("\n"))
                    _textModel.addChars("\n");
                Font font = _textStyle.getFont();
                double firstIndent = _textLineStyle.getTab(_listLevel - 1);
                double leftIndent = firstIndent + font.getStringAdvance(((char) 8226) + " ");
                _textLineStyle = _textLineStyle.copyForIndents(firstIndent, leftIndent, _textLineStyle.getRightIndent());
            }

            // Handle List item (<LI>)
            if (aTag.equals(HTML.Tag.LI))
                _textModel.addCharsWithStyle((char) 8226 + " ", _textStyle);

            // Handle FONT (<FONT face="Times, Arial" size=3 color=#FF00FF00>)
            if (aTag.equals(HTML.Tag.FONT)) {

                // Get base font
                Font font = _textStyle.getFont();

                // Iterate over Tag attributes to get new font
                for (Enumeration<?> e = anAttributeSet.getAttributeNames(); e.hasMoreElements(); ) {
                    Object attr = e.nextElement();
                    String name = attr.toString();
                    String string = anAttributeSet.getAttribute(attr).toString();

                    // Handle font color
                    if (name.equalsIgnoreCase("color")) {
                        if (!string.startsWith("#"))
                            string = colorNames().get(string.toLowerCase());

                        if (string != null) {
                            float r = Integer.decode("0x" + string.substring(1, 3)) / 255f;
                            float g = Integer.decode("0x" + string.substring(3, 5)) / 255f;
                            float b = Integer.decode("0x" + string.substring(5, 7)) / 255f;
                            Color color = new Color(r, g, b);
                            _textStyle = _textStyle.copyForStyleValue(color);
                        }
                    }

                    // Handle font size
                    if (name.equalsIgnoreCase("size")) {
                        int size = Math.abs(StringUtils.intValue(string));
                        float sizeFactor = size == 1 ? .5f : size == 2 ? .75f : size == 3 ? 1 : size == 4 ? 1.2f :
                                size == 5 ? 1.4f : size == 6 ? 1.6f : size == 7 ? 2 : 1;
                        font = font.copyForScale(sizeFactor);
                    }

                    // Handle font face
                    if (name.equalsIgnoreCase("face")) {
                        List<String> names = StringUtils.separate(string, ",");
                        for (String propName : names) {
                            Font font1 = new Font(propName, font.getSize());
                            if (!font1.isSubstitute()) {
                                if (font1.getNameEnglish().startsWith("Symbol"))
                                    _isSymbol = true; //else
                                font = font1;
                                break;
                            }
                        }
                    }
                }

                // Install new font
                _textStyle = _textStyle.copyForStyleValue(font);
                _fontStack.add(font);
            }

            // Handle paragraph start (make sure it's two lines below last text)
            if (aTag.equals(HTML.Tag.P) && !_textModel.isEmpty()) {
                if (!_textModel.getString().endsWith("\n"))
                    _textModel.addChars("\n");
                if (!_textModel.getString().endsWith("\n\n"))
                    _textModel.addChars("\n");
            }

            // Handle CENTER
            if (aTag.equals(HTML.Tag.CENTER))
                _textLineStyle = _textLineStyle.copyForAlign(HPos.CENTER);

            // Handle super-scripting
            if (aTag.equals(HTML.Tag.SUP))
                _textStyle = _textStyle.copyForStyleKeyValue(TextStyle.Scripting_Prop, 1);

            // Handle subscripting
            if (aTag.equals(HTML.Tag.SUB))
                _textStyle = _textStyle.copyForStyleKeyValue(TextStyle.Scripting_Prop, -1);
        }

        /**
         * Called when HTML tag end is encountered.
         */
        public void handleEndTag(HTML.Tag htmlTag, int pos)
        {
            // Handle Bold end (</B> and </STRONG>), Italic end (</I> and </EM>), Font end (</FONT>)
            if (htmlTag.equals(HTML.Tag.B) || htmlTag.equals(HTML.Tag.STRONG) ||
                    htmlTag.equals(HTML.Tag.I) || htmlTag.equals(HTML.Tag.EM) || htmlTag.equals(HTML.Tag.FONT)) {
                if (_fontStack.size() > 1) ListUtils.removeLast(_fontStack);
                Font font = ListUtils.getLast(_fontStack);
                _textStyle = _textStyle.copyForStyleValue(font);
                if (htmlTag.equals(HTML.Tag.FONT))
                    _isSymbol = false;
            }

            // Handle Underline (<U>)
            if (htmlTag.equals(HTML.Tag.U))
                _textStyle = _textStyle.copyForStyleKeyValue(TextStyle.Scripting_Prop, 0);

            // Handle paragraph end (</P>)
            if (htmlTag.equals(HTML.Tag.P))
                _textModel.addChars("\n\n");

            // Handle List item end
            if (htmlTag.equals(HTML.Tag.LI))
                _textModel.addChars("\n");

            // Handle List start (<UL> and <OL>)
            if (htmlTag.equals(HTML.Tag.UL) || htmlTag.equals(HTML.Tag.OL)) {
                _listLevel = Math.max(0, _listLevel - 1);
                if (!_textModel.getString().endsWith("\n")) _textModel.addChars("\n");
                Font font = _textStyle.getFont();
                double firstIndent = _listLevel == 0 ? 0 : _textLineStyle.getTab(_listLevel - 1);
                double leftIndent = _listLevel == 0 ? 0 : firstIndent + font.getStringAdvance(((char) 8226) + " ");
                _textLineStyle = _textLineStyle.copyForIndents(firstIndent, leftIndent, _textLineStyle.getRightIndent());
            }

            // Handle CENTER
            if (htmlTag.equals(HTML.Tag.CENTER)) {
                if (!_textModel.getString().endsWith("\n"))
                    _textModel.addChars("\n");
                _textLineStyle = _textLineStyle.copyForAlign(HPos.LEFT);
            }

            // Handle super-scripting
            if (htmlTag.equals(HTML.Tag.SUP))
                _textStyle = _textStyle.copyForStyleKeyValue(TextStyle.Scripting_Prop, null);

            // Handle subscripting
            if (htmlTag.equals(HTML.Tag.SUB))
                _textStyle = _textStyle.copyForStyleKeyValue(TextStyle.Scripting_Prop, null);
        }

        /**
         * Called to handle text.
         */
        public void handleText(char[] data, int pos)
        {
            // If _isSymbol font, map chars from Symbol char set to unicode
            if (_isSymbol)
                for (int i = 0; i < data.length; i++)
                    data[i] = getSymbolChar(data[i]);

            // Add String to text
            String str = new String(data);
            int len = _textModel.length();
            _textModel.addCharsWithStyle(str, _textStyle);
            _textModel.setLineStyle(_textLineStyle, len, len + str.length());
        }

        /**
         * Returns a symbol char for a given ASCII char.
         */
        private char getSymbolChar(char aChar)
        {
            return aChar < 256 ? _symChars[aChar] : aChar;
        }
    }

    /**
     * Returns map of color names and their hex strings. used for translating common color names to RGB.
     */
    private static Map<String,String> colorNames()
    {
        if (_colorNames == null) {
            _colorNames = new HashMap<>();
            _colorNames.put("black", "#000000");
            _colorNames.put("silver", "#C0C0C0");
            _colorNames.put("gray", "#808080");
            _colorNames.put("white", "#FFFFFF");
            _colorNames.put("maroon", "#800000");
            _colorNames.put("red", "#FF0000");
            _colorNames.put("purple", "#800080");
            _colorNames.put("fushia", "#FF00FF");
            _colorNames.put("green", "#008000");
            _colorNames.put("lime", "#00FF00");
            _colorNames.put("olive", "#808000");
            _colorNames.put("yellow", "#FFFF00");
            _colorNames.put("navy", "#000080");
            _colorNames.put("blue", "#0000FF");
            _colorNames.put("teal", "#008080");
            _colorNames.put("aqua", "#00FFFF");
        }
        return _colorNames;
    }

    /**
     * A class that maps a character in the Symbol character set to the Unicode character set.
     */
    private static class SymMap {

        // A character in the symbol character set
        char sym;

        // The same character in the unicode character set
        char map;
        String str;

        // Character entity representation (eg., "&whatever;")
        String ce;

        /**
         * Returns a mapping for a given symbol/unicode character and its name and character entity representation.
         */
        public SymMap(int aSym, int aMap, String aName, String charEnt)
        {
            sym = (char) aSym;
            map = (char) aMap;
            str = String.valueOf(map);
            ce = charEnt;
        }

        /**
         * Returns an array of symbol characters.
         */
        public static char[] symChars()
        {
            // Create array of 256 chars
            char[] chars = new char[256];

            // Initialize to char index
            for (char i = 0; i < 256; i++)
                chars[i] = i;

            // Re-initialize symbol characters explicitly defined below
            for (SymMap symMap : _symMap) chars[symMap.sym] = symMap.map;

            // Return char array
            return chars;
        }

        /**
         * Returns an array of character mappings.
         * Uses a symbol<->Unicode mapping I found at http://www.alanwood.net/demos/symbol.html.
         */
        public static SymMap[] symMap()
        {
            return new SymMap[]{

                    // Latin-1 Supplement: Unicode U+0080 - U+00FF (128-255)
                    new SymMap(177, 177, "plus minus", "&plusmn;"),
                    new SymMap(180, 215, "multiply", "&times;"),
                    new SymMap(184, 247, "divide", "&divide;"),
                    new SymMap(210, 174, "registered (serif)", "&reg;"),
                    new SymMap(211, 169, "copyright (serif)", "&copy;"),
                    new SymMap(216, 172, "not", "&not;"),
                    new SymMap(226, 174, "registered (sans serif)", "&reg;"),
                    new SymMap(227, 169, "copyright (sans serif)", "&copy;"),

                    // Latin Extended-B: Unicode U+0180 - U+024F (384-591)
                    new SymMap(166, 402, "Florin or Guilder", "&fnof;"),

                    // Greek: Unicode U+0370 - U+03FF (880-1023)
                    new SymMap('A', 913, "capital Alpha", "&Alpha;"),
                    new SymMap('B', 914, "capital Beta", "&Beta;"),
                    new SymMap('G', 915, "capital Gamma", "&Gamma;"),
                    new SymMap('D', 916, "capital Delta", "&Delta;"),
                    new SymMap('E', 917, "capital Epsilon", "&Epsilon;"),
                    new SymMap('Z', 918, "capital, Zeta", "&Zeta;"),
                    new SymMap('H', 919, "capital Eta", "&Eta;"),
                    new SymMap('Q', 920, "capital Theta", "&Theta;"),
                    new SymMap('I', 921, "capital Iota", "&Iota;"),
                    new SymMap('K', 922, "capital Kappa", "&Kappa;"),
                    new SymMap('L', 923, "capital Lambda", "&Lambda;"),
                    new SymMap('M', 924, "capital Mu", "&Mu;"),
                    new SymMap('N', 925, "capital Nu", "&Nu;"),
                    new SymMap('X', 926, "capital Xi", "&Xi;"),
                    new SymMap('O', 927, "capital Omicron", "&Omicron;"),
                    new SymMap('P', 928, "capital Pi", "&Pi;"),
                    new SymMap('R', 929, "capital Rho", "&Rho;"),
                    new SymMap('S', 931, "capital Sigma", "&Sigma;"),
                    new SymMap('T', 932, "capital Tau", "&Tau;"),
                    new SymMap('U', 933, "capital Upsilon", "&Upsilon;"),
                    new SymMap('F', 934, "capital Phi", "&Phi;"),
                    new SymMap('C', 935, "capital Chi", "&Chi;"),
                    new SymMap('Y', 936, "capital Psi", "&Psi;"),
                    new SymMap('W', 937, "capital Omega", "&Omega;"),
                    new SymMap('a', 945, "lower case alpha", "&alpha;"),
                    new SymMap('b', 946, "lower case beta", "&beta;"),
                    new SymMap('g', 947, "lower case gamma", "&gamma;"),
                    new SymMap('d', 948, "lower case delta", "&delta;"),
                    new SymMap('e', 949, "lower case epsilon", "&epsilon;"),
                    new SymMap('z', 950, "lower case zeta", "&zeta;"),
                    new SymMap('h', 951, "lower case eta", "&eta;"),
                    new SymMap('q', 952, "lower case theta", "&theta;"),
                    new SymMap('i', 953, "lower case iota", "&iota;"),
                    new SymMap('k', 954, "lower case kappa", "&kappa;"),
                    new SymMap('l', 955, "lower case lambda", "&lambda;"),
                    new SymMap('m', 956, "lower case mu", "&mu;"),
                    new SymMap('n', 957, "lower case nu", "&nu;"),
                    new SymMap('x', 958, "lower case xi", "&xi;"),
                    new SymMap('o', 959, "lower case omicron", "&omicron;"),
                    new SymMap('p', 960, "lower case pi", "&pi;"),
                    new SymMap('r', 961, "lower case rho", "&rho;"),
                    new SymMap('V', 962, "lower case sigma (terminal)", "&sigmaf;"),
                    new SymMap('s', 963, "lower case sigma", "&sigma;"),
                    new SymMap('t', 964, "lower case tau", "&tau;"),
                    new SymMap('u', 965, "lower case upsilon", "&upsilon;"),
                    new SymMap('f', 966, "lower case phi", "&phi;"),
                    new SymMap('c', 967, "lower case chi", "&chi;"),
                    new SymMap('y', 968, "lower case psi", "&psi;"),
                    new SymMap('w', 969, "lower case omega", "&omega;"),
                    new SymMap('J', 977, "theta symbol", "&thetasym;"),
                    new SymMap('j', 981, "phi symbol", "&xxx;"),
                    new SymMap('v', 982, "pi symbol", "&piv;"),
                    new SymMap(161, 978, "upsilon with hook symbol", "&upsih;"),

                    // General Punctuation: Unicode U+2000 - U+206F (8192-8303)
                    new SymMap(162, 8242, "prime or minutes or feet", "&prime;"),
                    new SymMap(164, 8260, "figure slash (fraction)", "&frasl;"),
                    new SymMap(178, 8243, "double prime or seconds", "&Prime;"),
                    new SymMap(188, 8230, "ellipsis", "&hellip;"),

                    // Letterlike Symbols: Unicode U+2100 - U+214F (8448-8527)
                    new SymMap(192, 8501, "alef symbol", "&alefsym;"),
                    new SymMap(193, 8465, "I fraktur", "&image;"),
                    new SymMap(194, 8476, "R fraktur", "&real;"),
                    new SymMap(195, 8472, "Weierstrass", "&weierp;"),
                    new SymMap(212, 8482, "trade mark (serif)", "&trade;"),
                    new SymMap(228, 8482, "trade mark (sans serif)", "&trade;"),

                    // Currency Symbols: Unicode U+20A0 - U+20CF (8352-8399)
                    new SymMap(240, 8364, "euro sign", "&euro;"),

                    // Arrows: Unicode U+2190 - U+21FF (8592-8703)
                    new SymMap(171, 8596, "arrow left and right", "&harr;"),
                    new SymMap(172, 9592, "arrow left", "&larr;"),
                    new SymMap(173, 8593, "arrow up", "&uarr;"),
                    new SymMap(174, 8594, "arrow right", "&rarr;"),
                    new SymMap(175, 8595, "arrow down", "&darr;"),
                    new SymMap(191, 8629, "carriage return", "&crarr;"),
                    new SymMap(219, 8660, "double arrow left and right", "&hArr;"),
                    new SymMap(220, 8656, "double arrow left (implied by)", "&lArr;"),
                    new SymMap(221, 8657, "double arrow up", "&uArr;"),
                    new SymMap(222, 8658, "double arrow right (implies)", "&rArr;"),
                    new SymMap(223, 8659, "double arrow down", "&dArr;"),

                    // Mathematical Operators: Unicode U+2200 - U+22FF (8704-8959)
                    new SymMap(34, 8704, "for all", "&forall;"),
                    new SymMap(36, 8707, "there exists", "&exist;"),
                    new SymMap(39, 8717, "such that, small contains as member", "&xxx;"),
                    new SymMap(42, 8727, "asterisk operator", "&lowast;"),
                    new SymMap(45, 8722, "minus", "&minus;"),
                    new SymMap(64, 8773, "approximately equal to congruent", "&cong;"),
                    new SymMap(92, 8756, "therefore", "&there4;"),
                    new SymMap(94, 8869, "perpendicular", "&perp;"),
                    new SymMap(126, 8764, "similar, tilde operator", "&sim;"),
                    new SymMap(163, 8804, "less than or equal to", "&le;"),
                    new SymMap(165, 8734, "infinity", "&infin;"),
                    new SymMap(179, 8805, "greater than or equal to", "&ge;"),
                    new SymMap(181, 8733, "proportional", "&prop;"),
                    new SymMap(182, 8706, "partial derivative / partial differential", "&part;"),
                    new SymMap(183, 8729, "bullet operator", "&xxx;"),
                    new SymMap(185, 8800, "not equal", "&ne;"),
                    new SymMap(186, 8801, "equivalent", "&equiv;"),
                    new SymMap(187, 8776, "almost equal to, asymptotic to", "&asymp;"),
                    new SymMap(196, 8855, "circle multiply", "&otimes;"),
                    new SymMap(197, 8853, "circle plus", "&oplus;"),
                    new SymMap(198, 8709, "empty set", "&empty;"),
                    new SymMap(199, 8745, "intersection", "&cap;"),
                    new SymMap(200, 8746, "union", "&cup;"),
                    new SymMap(201, 8835, "proper superset, superset of", "&sup;"),
                    new SymMap(202, 8839, "reflex superset (contains or equals), superset of or equal to", "&supe;"),
                    new SymMap(203, 8836, "not subset", "&nsub;"),
                    new SymMap(204, 8834, "proper subset, subset of", "&sub;"),
                    new SymMap(205, 8838, "reflex subset (contained in or equals), subset of or equal to", "&sube;"),
                    new SymMap(206, 8712, "member (element)", "&isin;"),
                    new SymMap(207, 8713, "not a member (not an element)", "&notin;"),
                    new SymMap(208, 8736, "angle", "&ang;"),
                    new SymMap(209, 8711, "nabla or gradient", "&nabla;"),
                    new SymMap(213, 8719, "product of", "&prod;"),
                    new SymMap(214, 8730, "square root", "&radic;"),
                    new SymMap(215, 8901, "dot operator", "&sdot;"),
                    new SymMap(217, 8743, "logical and", "&and;"),
                    new SymMap(218, 8744, "logical or", "&or;"),
                    new SymMap(229, 8721, "sum of", "&sum;"),
                    new SymMap(242, 8747, "integral", "&int;"),

                    // Miscellaneous Technical: Unicode U+2300 - U+23FF (8960-9215)
                    new SymMap(190, 9135, "horizontal arrow extender", "&xxx;"),
                    new SymMap(225, 9001, "left angle bracker (< is less than)", "&lang;"),
                    new SymMap(230, 9115, "large left parenthesis (top)", "&xxx;"),
                    new SymMap(231, 9116, "large left parenthesis (extender)", "&xxx;"),
                    new SymMap(232, 9117, "large left parenthesis (bottom)", "&xxx;"),
                    new SymMap(233, 9121, "large left bracket (top)", "&xxx;"),
                    new SymMap(234, 9122, "large left bracket (extender)", "&xxx;"),
                    new SymMap(235, 9123, "large left bracket (bottom)", "&xxx;"),
                    new SymMap(236, 9127, "large left brace (top)", "&xxx;"),
                    new SymMap(237, 9128, "large left brace (middle)", "&xxx;"),
                    new SymMap(238, 9129, "large left brace (bottom)", "&xxx;"),
                    new SymMap(239, 9130, "large brace extender", "&xxx;"),
                    new SymMap(241, 9002, "right angle bracket (> is greater than)", "&rang;"),
                    new SymMap(243, 8992, "large integral, top", "&xxx;"),
                    new SymMap(245, 8993, "large integral, bottom", "&xxx;"),
                    new SymMap(244, 9134, "large integral, extender", "&xxx;"),
                    new SymMap(246, 9118, "large right parenthesis (top)", "&xxx;"),
                    new SymMap(247, 9119, "large right parenthesis (extender)", "&xxx;"),
                    new SymMap(248, 9120, "large right parenthesis (bottom)", "&xxx;"),
                    new SymMap(249, 9124, "large right bracket (top)", "&xxx;"),
                    new SymMap(250, 9125, "large right bracket (extender)", "&xxx;"),
                    new SymMap(251, 9126, "large right bracket (bottom)", "&xxx;"),
                    new SymMap(252, 9131, "large right brace (top)", "&xxx;"),
                    new SymMap(253, 9132, "large right brace (middle)", "&xxx;"),
                    new SymMap(254, 9133, "large right brace (bottom)", "&xxx;"),
                    new SymMap(189, 9168, "vertical arrow extender", "&xxx;"),

                    // Geometric Shapes: Unicode U+25A0 - U+25FF (9632-9727)
                    new SymMap(224, 9674, "lozenge", "&loz;"),

                    // Miscellaneous Symbols: Unicode U+2600 - U+26FF (9728-9983)
                    new SymMap(167, 9827, "clubs", "&clubs;"),
                    new SymMap(168, 9830, "diamonds", "&diams;"),
                    new SymMap(169, 9829, "hearts", "&hearts;"),
                    new SymMap(170, 9824, "spades", "&spades;")
            };
        }
    }
}