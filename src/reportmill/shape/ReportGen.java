package reportmill.shape;
import rmdraw.base.RMKey;
import rmdraw.base.RMKeyChain;
import rmdraw.shape.RMShape;
import rmdraw.shape.ReportOwner;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.gfx.RichText;
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
     * Generate report with report owner.
     */
    public RMShape rpgAll(ReportOwner anRptOwner, RMShape aParent)
    {
        RMShape clone = rpgShape(anRptOwner, aParent);
        rpgBindings(anRptOwner, clone);
        return clone;
    }

    /**
     * Generate report with report owner.
     */
    protected T rpgShape(ReportOwner anRptOwner, RMShape aParent)
    {
        return (T)_shape.clone();
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
                if(color!=null) aShapeRPG.setColor(color); }
            else if(pname.equals("StrokeColor")) { Color color = Color.get(value);
                if(color!=null) aShapeRPG.setStrokeColor(color); }
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


}
