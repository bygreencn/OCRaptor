package mj.ocraptor.extraction.image_processing;

import static mj.ocraptor.database.dao.ResultError.KILLED_FORCED;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.file_handler.utils.FileTools;
import mj.ocraptor.rmi_client.RMIClientImpl;
import mj.ocraptor.rmi_server.RMIServer;
import mj.ocraptor.tools.St;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.SAXException;

public class TikaImageHelper {

  public static final String IMAGE_CONTAINER_CLASS = "imageContainer";

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
      this.filePath = metadata.get(Config.META_FILE_PATH) + File.separator
          + metadata.get(Config.META_FILE_NAME);
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
      if (!Config.inst().isClientDelayedShutdown()) {
        final String extractedText = ocrEngine.extractText(image, this.filePath);
        this.addImageText(extractedText);

        // ------------------------------------------------ //
        final RMIClientImpl client = RMIClientImpl.instance();
        final RMIServer server = client.getServer();
        try {
          server.incrementImageCount(client);
        } catch (RemoteException e) {
          e.printStackTrace();
        }
        // ------------------------------------------------ //
      } else {
        this.addImageText(KILLED_FORCED.getErrorCode());
      }
    }
  }

  /**
   *
   *
   * @param imageText
   */
  public void addImage(File image) {
    if (image != null) {
      if (!Config.inst().isClientDelayedShutdown()) {
        final String extractedText = ocrEngine.extractText(image, this.filePath);
        this.addImageText(extractedText);

        // ------------------------------------------------ //
        final RMIClientImpl client = RMIClientImpl.instance();
        final RMIServer server = client.getServer();
        try {
          server.incrementImageCount(client);
        } catch (RemoteException e) {
          e.printStackTrace();
        }
        // ------------------------------------------------ //
      } else {
        this.addImageText(KILLED_FORCED.getErrorCode());
      }
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
  public void addTextToHandler(XHTMLContentHandler handler, Integer page, Integer allPagesCount)
      throws SAXException {
    if (!this.imageText.isEmpty()) {
      // handler.startElement("p", "class", "page_indicator");
      // // TODO: text
      // handler.characters("Images on page: " + page);
      // handler.endElement("p");

      boolean endImageContainer = false;
      for (int i = 0; i < this.imageText.size(); i++) {
        String text = this.imageText.get(i);
        text = St.removeRareCharacters(text);

        if (text.length() > 5) {
          if (i == 0) {
            handler.startElement("div", "class", IMAGE_CONTAINER_CLASS);
            endImageContainer = true;
          }
          // TODO: pagination
          if (page != null && allPagesCount != null) {
            handler.startElement("span", "page", page + ":" + allPagesCount);
            handler.characters(" ");
            handler.endElement("span");
          }

          handler.characters(text);

          // --- //
          if (i >= 0 && i != this.imageText.size() - 1) {
            handler.startElement("span", "class", "imageDivider");
            handler.characters(" ");
            handler.endElement("span");
          }
        }
      }
      if (endImageContainer) {
        handler.characters(" ");
        handler.endElement("div");
      }
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
      File outputFile = FileTools.getTempFile("ocraptor_zip", new File(zipentry.getName())
          .getName(), true);

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
