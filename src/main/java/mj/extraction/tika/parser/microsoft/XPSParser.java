package mj.extraction.tika.parser.microsoft;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javaaxp.core.service.IXPSAccess;
import javaaxp.core.service.IXPSPageAccess;
import javaaxp.core.service.XPSError;
import javaaxp.core.service.impl.XPSServiceImpl;
import javaaxp.core.service.impl.document.jaxb.CTCanvas;
import javaaxp.core.service.impl.document.jaxb.CTGlyphs;
import javaaxp.core.service.impl.document.jaxb.CTPath;
import javaaxp.core.service.impl.fileaccess.XPSZipFileAccess;
import javaaxp.core.service.model.document.page.IFixedPage;
import mj.configuration.Config;
import mj.configuration.properties.ConfigBool;
import mj.extraction.image_processing.TikaImageHelper;
import mj.file_handler.filter.FileType;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class XPSParser implements Parser {

  private double currentXPosition = 0;

  /**
   *
   */
  private static final long serialVersionUID = -3528366722867144747L;

  private static final Set<MediaType> SUPPORTED_TYPES = Collections
      .singleton(MediaType.application("vnd.ms-xpsdocument"));

  private static final String XPS_MIME_TYPE = "application/vnd.ms-xpsdocument";

  private XHTMLContentHandler fileXHTML;

  public Set<MediaType> getSupportedTypes(ParseContext context) {
    return SUPPORTED_TYPES;
  }

  private Metadata metadata;

  public void parse(InputStream stream, ContentHandler handler,
      Metadata metadata, ParseContext context) throws IOException,
      SAXException, TikaException {
    this.metadata = metadata;
    metadata.set(Metadata.CONTENT_TYPE, XPS_MIME_TYPE);
    fileXHTML = new XHTMLContentHandler(handler, metadata);
    try {
      parseXPS(stream);
    } catch (XPSError e) {
      throw new IOException(e);
    }
    stream.close();
  }

  private void parseXPS(InputStream inputStream) throws XPSError, SAXException {
    TikaInputStream tikaStream = null;
    try {
      tikaStream = TikaInputStream.get(inputStream);
      File xpsFile = tikaStream.getFile();
      IXPSAccess xpsAccess = new XPSServiceImpl().getXPSAccess(xpsFile);
      xhtmlStartDocument();
      int firstDocNum = xpsAccess.getDocumentAccess().getFirstDocNum();
      int lastDocNum = xpsAccess.getDocumentAccess().getLastDocNum();
      for (int i = firstDocNum; i <= lastDocNum; i++) {
        IXPSPageAccess xpsPageAccess = xpsAccess.getPageAccess(i);
        XPSZipFileAccess ac = (XPSZipFileAccess) xpsAccess.getFileAccess();
        ac.getFixedDocument(xpsPageAccess.getDocumentReference());
        ac.getDocumentStructure(xpsPageAccess.getDocumentReference());

        int firstPageNum = xpsPageAccess.getFirstPageNum();
        int lastPageNum = xpsPageAccess.getLastPageNum();
        for (int j = firstPageNum; j <= lastPageNum; j++) {
          IFixedPage fixedPage = xpsPageAccess.getPage(j);
          parseObjs(fixedPage.getPathOrGlyphsOrCanvas());
        }

        if (Config.inst().getProp(
            ConfigBool.ENABLE_IMAGE_OCR)) {
          TikaImageHelper helper = new TikaImageHelper(metadata);
          try {
            // Process the file in turn
            ZipInputStream zip = new ZipInputStream(inputStream);
            ZipEntry entry = zip.getNextEntry();

            while (entry != null) {
              // TODO: images
              String entryExtension = null;
              try {
                entryExtension = FilenameUtils.getExtension(new File(entry
                    .getName()).getName());
              } catch (Exception e) {
                e.printStackTrace();
              }

              if (entryExtension != null
                  && FileType.isValidImageFileExtension(entryExtension)) {
                File imageFile = null;
                try {
                  imageFile = TikaImageHelper.saveZipEntryToTemp(zip, entry);
                  helper.addImage(imageFile);
                } catch (Exception e) {
                  e.printStackTrace();
                } finally {
                  if (imageFile != null) {
                    imageFile.delete();
                  }
                }
              }
              entry = zip.getNextEntry();
            }
            helper.addTextToHandler(fileXHTML);
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            if (helper != null) {
              helper.close();
            }
          }
        }
      }
      xhtmlEndDocument();
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    } finally {
      try {
        if (tikaStream != null) {
          tikaStream.close();
        }
      } catch (IOException e) {
      }
    }
  }

  private void parseObjs(List<Object> objs) throws XPSError, SAXException {
    for (Object o : objs)
      parseObj(o);
  }

  private void parseObj(Object xpsObj) throws XPSError, SAXException {
    if (xpsObj instanceof CTCanvas) {
      CTCanvas c = (CTCanvas) xpsObj;
      xhtmlStartCanvas();
      parseObjs(c.getPathOrGlyphsOrCanvas());
      xhtmlEndCanvas();
    } else if (xpsObj instanceof CTGlyphs) {
      CTGlyphs c = (CTGlyphs) xpsObj;
      if (c.getOriginX() < currentXPosition) {
        fileXHTML.startElement("div");
        fileXHTML.characters(" ");
        fileXHTML.endElement("div");
      }
      String text = c.getUnicodeString();
      xhtmlParagraph(text);
      currentXPosition = c.getOriginX();
    } else if (xpsObj instanceof CTPath) {
    } else {
      System.out.println("Unhandled type : "
          + xpsObj.getClass().getCanonicalName());
    }
  }

  private void xhtmlStartDocument() throws SAXException {
    fileXHTML.startDocument();
  }

  private void xhtmlEndDocument() throws SAXException {
    fileXHTML.endDocument();
  }

  private void xhtmlStartCanvas() throws SAXException {
    fileXHTML.startElement("div");
  }

  private void xhtmlEndCanvas() throws SAXException {
    fileXHTML.endElement("div");
  }

  private void xhtmlParagraph(String text) throws SAXException {
    fileXHTML.startElement("span");
    fileXHTML.characters(text);
    fileXHTML.endElement("span");
  }

  /**
   * @deprecated This method will be removed in Apache Tika 1.0.
   */
  public void parse(InputStream stream, ContentHandler handler,
      Metadata metadata) throws IOException, SAXException, TikaException {
    parse(stream, handler, metadata, new ParseContext());
  }

}
