package com.reportmill.pdf;
import com.reportmill.pdf.read.PDFPagePainter;
import com.reportmill.pdf.write.PDFEncryptor;
import snap.geom.Rect;
import snap.gfx.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * A class to allow certain functionality to be pluggable depending on platform (desktop/web).
 */
public class PDFEnv {

    // The shared instance
    private static PDFEnv _shared = new PDFEnv();

    /**
     * Generates and returns a unique file identifier.
     */
    public byte[] getFileID(PDFFile aFile)
    {
        // In order to be unique to file's contents, fileID is generated with an MD5 digest of contents of info dictionary.
        // The pdf spec suggests using the file size and the file name, but we don't have access to those here.
        // The spec suggests using current time, but that's already present in info dict as value of /CreationDate key.
        try {

            // Create MessageDigest and add InfoDict.values
            MessageDigest md = MessageDigest.getInstance("MD5");
            Map<String, String> infoDict = aFile.getInfoDict();
            for (String str : infoDict.values())
                md.update(str.getBytes());

            // Rather than adding file size, which we won't have until everything gets dumped out, add number
            // of objects in xref table (as 4 bytes). This is probably going to be the same for everyone.
            for (int i = 0, size = aFile._xtable.getEntryCount(); i < 4; i++) {
                md.update((byte) (size & 0xff));
                size >>= 8;
            }

            // Get the digest and cache it - MD5 is defined to return a 128 bit (16 byte) digest
            byte[] digest_bytes = md.digest();

            // This should never happen, so this is here just in case something goes wrong.
            if (digest_bytes.length > 16)
                digest_bytes = Arrays.copyOf(digest_bytes, 16);
            return digest_bytes;
        }

        // If the md5 fails, just create a fileID with random bytes
        catch (java.security.NoSuchAlgorithmException nsae) {
            byte[] fileId = new byte[16];
            new Random().nextBytes(fileId);
            return fileId;
        }
    }

    /**
     * Set everything to the default implementations and return an Image for this page.
     */
    public Image getImage(PDFPage aPage)
    {
        // Get page bounds
        Rect mediaBox = aPage.getMediaBox();
        Rect cropBox = aPage.getCropBox();
        Rect imageBounds = mediaBox.getIntersectRect(cropBox);
        int imageW = (int) Math.round(imageBounds.width);
        int imageH = (int) Math.round(imageBounds.height);

        // Create PDF painter that renders into an image
        Image imageForPdf = Image.getImageForSize(imageW, imageH, false);
        Painter ipntr = imageForPdf.getPainter();
        ipntr.setColor(Color.WHITE);
        ipntr.fillRect(0, 0, imageW, imageH);

        // Create PDF painter that renders into an image
        PDFPagePainter pagePainter = new PDFPagePainter(aPage);
        pagePainter.paint(ipntr, null, null, null);

        return imageForPdf;
    }

    /**
     * Paints the page to given painter, scaled to fit given rectangle.
     */
    public void paint(PDFPage aPage, Painter aPntr, Rect aRect)
    {
        PDFPagePainter pagePainter = new PDFPagePainter(aPage);
        pagePainter.paint(aPntr, null, aRect, null);
    }

    /**
     * Creates a new PDF encryptor. Both the owner and user passwords are optional.
     */
    public PDFCodec newEncryptor(byte[] fileID, String ownerPwd, String userPwd, int permissionFlags)
    {
        return new PDFEncryptor(fileID, ownerPwd, userPwd, permissionFlags);
    }

    /**
     * Returns an instance of the appropriate PDFCodec.
     */
    public PDFCodec getEncryptor(Map encryptionDict, List<String> fileIds, double pdfversion)
    {
        return PDFSecurityHandler.getInstance(encryptionDict, fileIds, pdfversion);
    }

    /**
     * Returns the shared instance.
     */
    public static PDFEnv getEnv()  { return _shared; }
}
