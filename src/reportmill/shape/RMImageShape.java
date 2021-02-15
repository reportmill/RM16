package reportmill.shape;
import rmdraw.scene.SGImage;
import snap.geom.HPos;
import snap.geom.Pos;
import snap.geom.VPos;
import snap.gfx.ImagePaint;
import snap.gfx.ImageRef;
import snap.gfx.Paint;
import snap.util.ArrayUtils;
import snap.util.XMLArchiver;
import snap.util.XMLElement;

/**
 * SGImage subclass for legacy support.
 */
public class RMImageShape extends SGImage {

    /**
     * XML unarchival.
     */
    public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
    {
        // If Image resource and resource bytes is PDF, replace with RMPDFShape
        String rsrcId = anElement.getAttributeValue("resource");
        if (rsrcId != null) {
            byte bytes[] = anArchiver.getResource(rsrcId);
            if (RMPDFData.canRead(bytes))
                return new RMPDFShape().fromXML(anArchiver, anElement);
        }

        // If ImageFill image resource and resource bytes is PDF, replace with RMPDFShape
        for (int i = anArchiver.indexOf(anElement, Paint.class); i>=0; i=-1) {
            XMLElement fillXML = anElement.get(i);
            rsrcId = fillXML.getAttributeValue("resource");
            if (rsrcId != null) {
                byte bytes[] = anArchiver.getResource(rsrcId);
                if (RMPDFData.canRead(bytes))
                    return new RMPDFShape(bytes);
            }
        }

        // Unarchive basic shape attributes
        super.fromXML(anArchiver, anElement);

        // Legacy alignment (RM14)
        if (anElement.hasAttribute("Alignment")) {
            String as = anElement.getAttributeValue("Alignment");
            String s[] = { "TopLeft", "TopCenter", "TopRight", "CenterLeft", "Center", "CenterRight",
                    "BottomLeft", "BottomCenter", "BottomRight" };
            int i = ArrayUtils.indexOf(s, as);
            if (i>=0) setAlign(Pos.values()[i]);
        }

        // Legacy: If Fill is ImageFill and no ImageRef+Key or ImageFill.ImageRef, set ImageRef from IFill and clear fill
        if (getFill() instanceof ImagePaint) {
            ImagePaint ifill = (ImagePaint) getFill();
            ImageRef iref = ImageRef.getImageRef(ifill.getImage()); //ifill.getImageRef();
            XMLElement fill = anElement.get("fill");

            if (getImageRef()==null && !ifill.isTiled()) { // && getKey()==null) {
                int fs = fill.getAttributeIntValue("fillstyle", 0); // Stretch=0, Tile=1, Fit=2, FitIfNeeded=3
                if (fs==0) { setImageRef(iref); setFill(null); setGrowToFit(true); setPreserveRatio(false); }
                else if (fs==2) { setImageRef(iref); setFill(null); setGrowToFit(true); setPreserveRatio(true); }
                else if (fs==3) { setImageRef(iref); setFill(null); setGrowToFit(false); setPreserveRatio(true); }
                double x = fill.getAttributeFloatValue("x");
                double y = fill.getAttributeFloatValue("y");
                if (x!=0) setAlignX(x<0 ? HPos.LEFT : HPos.RIGHT);
                if (y!=0) setAlignY(y<0 ? VPos.TOP : VPos.BOTTOM);
            }
            else if (iref==null) setFill(null);
            setPadding(fill.getAttributeIntValue("margin"));
        }

        // Unarchive basic shape attributes
        return this;
    }
}
