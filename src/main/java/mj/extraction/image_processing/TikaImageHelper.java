package mj.extraction.image_processing;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

import mj.extraction.result.document.MetaData;
import mj.file_handler.utils.FileTools;
import mj.tools.StringTools;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.SAXException;

public class TikaImageHelper {

  private List<String> imageText;
  private final ImageTextExtractor ocrEngine;
  private String filePath;

  /**
   *
   */
  public TikaImageHelper(Metadata metadata) {
    this.imageText = new ArrayList<String>();
    this.ocrEngine = new ImageTextExtractorTess4j();
    if (metadata != null) {
      this.filePath = metadata.get(MetaData.FILE_PATH) + File.separator
          + metadata.get(MetaData.FILE_NAME);
    }
  }

  /**
   *
   *
   */
  public void close() {
    if (this.ocrEngine != null) {
      this.ocrEngine.close();
    }
  }

  /**
   *
   *
   * @param imageText
   */
  public void addImage(BufferedImage image) {
    if (image != null) {
      String extractedText = ocrEngine.extractText(image, this.filePath);
      this.addImageText(extractedText);
    }
  }

  /**
   *
   *
   * @param imageText
   */
  public void addImage(File image) {
    if (image != null) {
      String extractedText = ocrEngine.extractText(image, this.filePath);
      this.addImageText(extractedText);
    }
  }

  /**
   *
   *
   * @param extractedText
   */
  public void addImageText(String extractedText) {
    if (extractedText != null && extractedText.length() > 5) {
      this.imageText.add(extractedText);
    }
  }

  /**
   *
   *
   * @param handler
   *
   * @throws SAXException
   */
  public void addTextToHandler(XHTMLContentHandler handler) throws SAXException {
    this.addTextToHandler(handler, null, null);
  }

  /**
   *
   *
   * @param imageText
   * @param page
   * @param allPagesCount
   * @throws SAXException
   */
  public void addTextToHandler(XHTMLContentHandler handler, Integer page,
      Integer allPagesCount) throws SAXException {
    if (!this.imageText.isEmpty()) {
      handler.startElement("div", "class", "image_container");
      for (String text : this.imageText) {
        text = StringTools.removeRareCharacters(text);
        if (text.length() > 5) {
          // TODO: pagination
          handler.startElement("span", "class", "ocr_image");
          //
          if (page != null && allPagesCount != null) {
            handler.startElement("span", "class", "page_no");
            handler.characters(page + "/" + allPagesCount);
            handler.endElement("span");
          }
          //
          handler.characters(text);
          handler.endElement("span");
        }
      }
      handler.endElement("div");
    }
    this.imageText.clear();
  }

  /**
   *
   *
   * @param stream
   * @param zipentry
   * @return
   */
  public static File saveZipEntryToTemp(InputStream stream, ZipEntry zipentry) {
    try {
      File outputFile = FileTools.getRandomTempFile("ocraptor_zip", new File(
          zipentry.getName()).getName());

      byte[] buf = new byte[1024];
      if (zipentry != null) {
        int n;
        FileOutputStream fileoutputstream;
        fileoutputstream = new FileOutputStream(outputFile);
        while ((n = stream.read(buf, 0, 1024)) > -1)
          fileoutputstream.write(buf, 0, n);

        fileoutputstream.close();
        return outputFile;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
