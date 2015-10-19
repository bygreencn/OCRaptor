package mj.ocraptor.extraction.tika.parser.djvu;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mj.ocraptor.extraction.image_processing.TikaImageHelper;
import mj.ocraptor.extraction.tika.parser.xoj.format.Loader;
import mj.ocraptor.extraction.tika.parser.xoj.format.Page;
import mj.ocraptor.extraction.tika.parser.xoj.format.PageGenerator;
import mj.ocraptor.file_handler.filter.FileType;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class DjVuParser extends AbstractParser {

  /**
   *
   */
  private static final long serialVersionUID = -2559814349908800531L;

  private static String CONTENT_TYPE_DJVU = "image/vnd.djvu ";

  private static final Set<MediaType> SUPPORTED_TYPES = Collections
      .unmodifiableSet(new HashSet<MediaType>(Arrays.asList(FileType.XOJ.getMediaType())));

  public Set<MediaType> getSupportedTypes(ParseContext context) {
    return SUPPORTED_TYPES;
  }

  private String tmpPath = null;
  private String djVuTextPath = null;

  public void parse(InputStream stream, ContentHandler handler, Metadata metadata,
      ParseContext context) throws IOException, SAXException, TikaException {
    String type = metadata.get(Metadata.CONTENT_TYPE);

    if (type != null) {

      TikaImageHelper helper = null;
      try {
        helper = new TikaImageHelper(metadata);

        XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
        xhtml.startDocument();

        Loader loader = new Loader();
        // TODO: temp files
        Document xojDocument = loader.load(stream);
        PageGenerator pageGen = new PageGenerator(xojDocument);
        List<Page> pages = pageGen.paginate();

        for (Page page : pages) {
          final List<String> snippets = page.getTextSnippets();
          xhtml.startElement("div", "class", "page");
          for (String snippet : snippets) {
            xhtml.startElement("p");
            xhtml.characters(snippet);
            xhtml.endElement("p");
          }
          xhtml.endElement("div");
        }

        for (int i = 0; i < pages.size(); i++) {
          final List<BufferedImage> images = pages.get(i).getImageFiles();
          for (BufferedImage image : images) {
            helper.addImage(image);
          }
          helper.addTextToHandler(xhtml, i + 1, pages.size());
        }
        xhtml.endDocument();

      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (helper != null) {
          helper.close();
        }
      }
    }
  }
}
