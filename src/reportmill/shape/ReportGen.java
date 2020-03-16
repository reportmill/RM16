package reportmill.shape;
import reportmill.util.RMKey;
import reportmill.util.RMKeyChain;
import rmdraw.shape.*;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.RichText;
import snap.util.SnapUtils;
import snap.util.StringUtils;
import snap.view.Binding;

/**
 * A class to add Report generation to RMShape classes.
 */
public class ReportGen <T extends RMShape> {

    // The shape
    private T _shape;

    /**
     * Interface for shapes that do their own RPG.
     */
    public interface RPG <T extends RMShape> {

        /**
         * Generate report with report owner.
         */
        default RMShape rpgAll(ReportOwner anRptOwner, RMShape aParent)
        {
            return rpgAllSuperFor((RMShape)this, anRptOwner, aParent);
        }

        /**
         * Generate report with report owner.
         */
        default T rpgShape(ReportOwner anRptOwner, RMShape aParent)
        {
            return (T)rpgShapeSuperFor((RMShape)this, anRptOwner, aParent);
        }

        /**
         * Generate report with report owner.
         */
        default RMShape rpgChildren(ReportOwner anRptOwner, RMParentShape aParent)
        {
            return rpgChildrenSuperFor((RMShape)this, anRptOwner, aParent);
        }

        /**
         * Report generation for URL and bindings.
         */
        default void rpgBindings(ReportOwner anRptOwner, RMShape aShapeRPG)
        {
            rpgBindingsSuperFor((RMShape)this, anRptOwner, aShapeRPG);
        }

        /**
         * Replaces all @Page@ style keys with their actual values for this shape and it's children.
         */
        default void resolvePageReferences(ReportOwner aRptOwner, Object userInfo)
        {
            resolvePageReferencesSuperFor((RMShape)this, aRptOwner, userInfo);
        }
    }

    /**
     * Returns the shape.
     */
    public T getShape()
    {
        return _shape;
    }

    /**
     * Generate report with report owner.
     */
    public RMShape rpgAll(ReportOwner anRptOwner, RMShape aParent)
    {
        T cell = getShape();
        RMShape clone = rpgShapeFor(cell, anRptOwner, aParent);
        rpgBindingsFor(cell, anRptOwner, clone);
        return clone;
    }

    /**
     * Generate report with report owner.
     */
    protected RMShape rpgShape(ReportOwner anRptOwner, RMShape aParent)
    {
        return _shape.clone();
    }

    /**
     * Generate report with report owner.
     */
    protected RMShape rpgChildren(ReportOwner anRptOwner, RMParentShape aParent)
    {
        throw new RuntimeException("ReportGen.rpgChildren: Shouldn't get called");
    }

    /**
     * Report generation for URL and bindings.
     */
    public void rpgBindings(ReportOwner anRptOwner, RMShape aShapeRPG)
    {
        // Clone URL
        String urls = _shape.getURL();
        if(urls!=null && urls.length()>0 && urls.indexOf('@')>=0) {
            RichText url = new RichText(urls);
            url = anRptOwner.rpgCloneRichText(url, null, aShapeRPG, false);
            aShapeRPG.setURL(url.getString());
        }

        // Iterate over bindings and evaluate
        for(int i=0; i<_shape.getBindingCount(); i++) { Binding binding = _shape.getBinding(i);

            // Get PropertyName, Key and Value (just continue if empty key)
            String pname = binding.getPropertyName(); if(pname==null) continue;
            String key = binding.getKey(); if(key==null || key.length()==0) continue;
            Object value = RMKeyChain.getValue(anRptOwner, key);

            // Handle Font
            if(pname.equals("Font")) {

                // Get value as string (if zero length, just continue)
                String fs = value instanceof String? (String)value : null; if(fs==null || fs.length()==0) continue;

                // If string has underline in it, underline and delete
                if(StringUtils.indexOfIC(fs, "Underline")>=0) {
                    aShapeRPG.setUnderlined(true); fs = StringUtils.deleteIC(fs, "Underline").trim(); }

                // Get size from string (if found, strip size from string)
                int sizeIndex = fs.lastIndexOf(" ");
                double size = sizeIndex<0 ? 0 : SnapUtils.floatValue(fs.substring(sizeIndex+1));
                if(size>0) fs = fs.substring(0, Math.max(sizeIndex, 0)).trim();
                else size = _shape.getFont()==null? 12 : _shape.getFont().getSize();

                // Get root font (use default font if not found), and modified font
                Font font = _shape.getFont();
                if(font==null) font = Font.getDefaultFont();
                if (fs.equalsIgnoreCase("Bold"))
                    font = font.getBold();
                else if (fs.equalsIgnoreCase("Italic"))
                    font = font.getItalic();
                else if(fs.length()>0) // If there is anything in string, try to parse font name
                    font = new Font(fs, size);

                // Get font at right size and apply it
                font = font.deriveFont(size);
                aShapeRPG.setFont(font);
            }

            // Handle FillColor, StrokeColor, TextColor
            else if(pname.equals("FillColor")) { Color color = Color.get(value);
                if(color!=null) aShapeRPG.setFillColor(color); }
            else if(pname.equals("StrokeColor")) { Color color = Color.get(value);
                if(color!=null) aShapeRPG.setBorderColor(color); }
            else if(pname.equals("TextColor")) { Color color = Color.get(value);
                if(color!=null) aShapeRPG.setTextColor(color); }

            // Handle others: X, Y, Width, Height, Visible, URL
            else RMKey.setValueSafe(aShapeRPG, pname, value);
        }
    }

    /**
     * Replaces all @Page@ style keys with their actual values for this shape and it's children.
     */
    protected void resolvePageReferences(ReportOwner aRptOwner, Object userInfo)
    {
        // If URL has @-sign, do rpg clone in case it is page reference
        String urls = _shape.getURL();
        if(urls!=null && urls.length()>0 && urls.indexOf('@')>=0) {
            RichText rtext = new RichText(urls);
            RichText url = aRptOwner.rpgCloneRichText(rtext, userInfo, null, false);
            _shape.setURL(url.getString());
        }
    }

    /**
     * Returns the appropriate ReportGen for given cell.
     */
    public static ReportGen getRPG(RMShape aCell)
    {
        ReportGen rpg;
        if (aCell instanceof RMImageShape) rpg = new ReportGens.ImageCellRPG();
        else if (aCell instanceof RMPage) rpg = new ReportGens.PageCellRPG();
        else if (aCell instanceof RMTextShape) rpg = new ReportGens.TextCellRPG();
        else if (aCell instanceof RMParentShape) rpg = new ReportGens.ParentCellRPG();
        else rpg = new ReportGen();
        rpg._shape = aCell;
        return rpg;
    }

    /**
     * Calls rpgAll for given cell.
     */
    public static RMShape rpgAllFor(RMShape aCell, ReportOwner anRptOwner, RMShape aParent)
    {
        if (aCell instanceof RPG)
            return ((RPG)aCell).rpgAll(anRptOwner, aParent);
        return rpgAllSuperFor(aCell, anRptOwner, aParent);
    }

    /**
     * Calls rpgAll for given shape.
     */
    public static RMShape rpgAllSuperFor(RMShape aCell, ReportOwner anRptOwner, RMShape aParent)
    {
        ReportGen rgen = getRPG(aCell);
        return rgen.rpgAll(anRptOwner, aParent);
    }

    /**
     * Calls rpgAll for given cell.
     */
    public static RMShape rpgShapeFor(RMShape aCell, ReportOwner anRptOwner, RMShape aParent)
    {
        if (aCell instanceof RPG)
            return ((RPG)aCell).rpgShape(anRptOwner, aParent);
        return rpgShapeSuperFor(aCell, anRptOwner, aParent);
    }

    /**
     * Calls rpgAll for given cell.
     */
    public static RMShape rpgShapeSuperFor(RMShape aCell, ReportOwner anRptOwner, RMShape aParent)
    {
        ReportGen rgen = getRPG(aCell);
        return rgen.rpgShape(anRptOwner, aParent);
    }

    /**
     * Calls rpgChildrenFor for given cell.
     */
    public static RMShape rpgChildrenFor(RMShape aCell, ReportOwner anRptOwner, RMParentShape aParent)
    {
        if (aCell instanceof RPG)
            return ((RPG)aCell).rpgChildren(anRptOwner, aParent);
        return rpgChildrenSuperFor(aCell, anRptOwner, aParent);
    }

    /**
     * Calls rpgChildrenFor for given cell.
     */
    public static RMShape rpgChildrenSuperFor(RMShape aCell, ReportOwner anRptOwner, RMParentShape aParent)
    {
        ReportGen rgen = getRPG(aCell);
        return rgen.rpgChildren(anRptOwner, aParent);
    }

    /**
     * Calls rpgBindingsFor for given cell.
     */
    public static void rpgBindingsFor(RMShape aCell, ReportOwner anRptOwner, RMShape aShapeRPG)
    {
        if (aCell instanceof RPG)
            ((RPG)aCell).rpgBindings(anRptOwner, aShapeRPG);
        else rpgBindingsSuperFor(aCell, anRptOwner, aShapeRPG);
    }

    /**
     * Calls rpgBindingsFor for given cell.
     */
    public static void rpgBindingsSuperFor(RMShape aCell, ReportOwner anRptOwner, RMShape aShapeRPG)
    {
        ReportGen rgen = getRPG(aCell);
        rgen.rpgBindings(anRptOwner, aShapeRPG);
    }

    /**
     * Calls resolvePageReferencesFor for given cell.
     */
    public static void resolvePageReferencesFor(RMShape aCell, ReportOwner aRptOwner, Object userInfo)
    {
        if (aCell instanceof RPG)
            ((RPG)aCell).resolvePageReferences(aRptOwner, userInfo);
        else resolvePageReferencesSuperFor(aCell, aRptOwner, userInfo);
    }

    /**
     * Calls resolvePageReferencesFor for given cell.
     */
    public static void resolvePageReferencesSuperFor(RMShape aCell, ReportOwner aRptOwner, Object userInfo)
    {
        ReportGen rgen = getRPG(aCell);
        rgen.resolvePageReferences(aRptOwner, userInfo);
    }
}
