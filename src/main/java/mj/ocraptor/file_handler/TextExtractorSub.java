package mj.ocraptor.file_handler;

import static mj.ocraptor.file_handler.filter.FileType.APPLE_KEY;
import static mj.ocraptor.file_handler.filter.FileType.APPLE_NUMBERS;
import static mj.ocraptor.file_handler.filter.FileType.APPLE_PAGES;
import static mj.ocraptor.file_handler.filter.FileType.EPUB;
import static mj.ocraptor.file_handler.filter.FileType.LO_CALC;
import static mj.ocraptor.file_handler.filter.FileType.LO_IMPRESS;
import static mj.ocraptor.file_handler.filter.FileType.LO_WRITER;
import static mj.ocraptor.file_handler.filter.FileType.PS;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigBool;
import mj.ocraptor.configuration.properties.ConfigInteger;
import mj.ocraptor.database.dao.FileEntry;
import mj.ocraptor.database.dao.ResultError;
import mj.ocraptor.database.search.TextProcessing;
import mj.ocraptor.extraction.tika.parser.epub.EpubParser;
import mj.ocraptor.extraction.tika.parser.html.HtmlParser;
import mj.ocraptor.extraction.tika.parser.image.ImageParser;
import mj.ocraptor.extraction.tika.parser.iwork.IWorkPackageParser;
import mj.ocraptor.extraction.tika.parser.microsoft.OfficeParser;
import mj.ocraptor.extraction.tika.parser.microsoft.XPSParser;
import mj.ocraptor.extraction.tika.parser.microsoft.ooxml.OOXMLParser;
import mj.ocraptor.extraction.tika.parser.odf.OpenDocumentParser;
import mj.ocraptor.extraction.tika.parser.pdf.PDFParser;
import mj.ocraptor.extraction.tika.parser.rtf.RTFParser;
import mj.ocraptor.extraction.tika.parser.txt.TXTParser;
import mj.ocraptor.extraction.tika.parser.xml.XMLParser;
import mj.ocraptor.extraction.tika.parser.xoj.XojParser;
import mj.ocraptor.file_handler.filter.FileType;
import mj.ocraptor.rmi_client.RMIClientImpl;
import mj.ocraptor.rmi_server.RMIServerImpl;
import mj.ocraptor.tools.St;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;

public class TextExtractorSub {
  private FileType fileType;
  private Config cfg;
  private TextExtractorTools tools;

  /**
   *
   */
  public TextExtractorSub() {
    this.cfg = Config.inst();
    this.tools = new TextExtractorTools();
  }

  /**
   *
   *
   * @param file
   * @return
   * @throws Exception
   */
  public FileEntry extractTextTika(final File file) throws Exception {
    this.fileType = FileType.get(file);

    InputStream inputStream = null;
    StringWriter stringWriter = null;
    FileEntry result = null;
    File tempFile = null;

    try {

      // ------------------------------------------------ //
      // -- do not index the given database-folder
      // ------------------------------------------------ //

      final TikaConfig config = new TikaConfig(Config.inst().getTikaMimeFile());
      final AutoDetectParser autoDetectParser = new AutoDetectParser(config);
      final Map<MediaType, Parser> availableParsers = tools.getAvailableParsers(file);

      // ------------------------------------------------ //
      // --
      // ------------------------------------------------ //
      final List<MediaType> supportedFileTypes = new ArrayList<MediaType>(availableParsers.keySet());

      if (!supportedFileTypes.contains(fileType.getMediaType())) {
        result = new FileEntry(file);
        // not supported filetype --> don't make a db entry
        result.setError(ResultError.NOT_SUPPORTED);
        return result;
      }

      // ------------------------------------------------ //
      // --
      // ------------------------------------------------ //

      autoDetectParser.setParsers(availableParsers);

      Metadata metadata = new Metadata();
      this.addStandardMetadata(metadata, file);

      if (FileType.is(file, FileType.PS)) {
        tempFile = PDFParser.convertPostScriptToPDF(file);
        inputStream = new FileInputStream(tempFile);
      } else {
        inputStream = new FileInputStream(file);
      }
      stringWriter = new StringWriter();

      // ------------------------------------------------ //
      // parse document and convert content to xhtml
      SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
      TransformerHandler handler = factory.newTransformerHandler();
      handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
      handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "no");
      handler.setResult(new StreamResult(stringWriter));
      BodyContentHandler bch = new BodyContentHandler(handler);
      try {
        autoDetectParser.parse(inputStream, bch, metadata);
      } catch (Exception e) {
        throw e;
      }

      // ------------------------------------------------ //

      String xhtml = stringWriter.toString();

      // RMIClientImpl.instance().sendDebugErrorToServer(xhtml, null, true);

      // TODO:
      if (xhtml != null && !xhtml.trim().isEmpty()) {
        metadata = normalizeMetadata(metadata);

        String xmlns = " xmlns=\"http://www.w3.org/1999/xhtml\"";
        xhtml = xhtml.replace(xmlns, "");
        xhtml = xhtml.replaceFirst("\\?>", "\\?><div" + xmlns + ">");
        xhtml = xhtml.replaceAll("\\s+", " ");
        StringBuilder builder = new StringBuilder();
        builder.append(xhtml);

        // ------------------------------------------------ //
        if (this.cfg.getProp(ConfigBool.INCLUDE_METADATA)) {
          builder.append("<div class=\"metadata\">");
          for (String md : metadata.names()) {
            if (md != null && !md.trim().isEmpty()) {
              String value = metadata.get(md);
              if (value != null && !value.trim().isEmpty()) {
                builder.append("<p> " + md + "=" + value + " </p>");
              }
            }
          }
          builder.append("</div>");
        }
        // ------------------------------------------------ //

        builder.append("</div>");
        xhtml = builder.toString();
        xhtml = TextProcessing.preProcess(xhtml);

        result = new FileEntry(file);
        result.setFullText(xhtml);
        // System.out.println(xhtml);
      }
    } catch (Exception e) {
      throw e;
    } finally {
      try {
        if (inputStream != null)
          inputStream.close();
        if (stringWriter != null)
          stringWriter.close();
        if (tempFile != null && tempFile.exists())
          tempFile.delete();
      } catch (IOException e) {
      }
    }
    return result;
  }

  private Boolean validSize(Integer maxSize, File currentFile) {
    if (maxSize != null) {
      try {
        long fileSizeInKB = currentFile.length() / 1024;
        if (fileSizeInKB < maxSize)
          return true;
        else
          return false;
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private void addStandardMetadata(Metadata metadata, File file) {
    if (metadata != null && file != null) {
      if (metadata.get(Metadata.CONTENT_TYPE) == null) {
        metadata.set(Metadata.CONTENT_TYPE, fileType.getMimeString());
      }

      // metadata.remove("X-Parsed-By");
      metadata.set(Config.META_FILE_NAME, file.getName());
      metadata.set(Config.META_FILE_PATH, file.getParent());

      // metadata.set(Metadata.CONTENT_ENCODING, "utf-8");
      // metadata.add(Metadata.CONTENT_ENCODING, "utf-8");
    }
  }

  private Metadata normalizeMetadata(Metadata metadata) {
    String[] ignoredMetadata = new String[] { "x-parsed-by" };
    Metadata filteredMetadata = new Metadata();
    for (String key : metadata.names()) {
      if (key != null && !key.trim().isEmpty()) {
        key = St.normalizeDocumentText(key);
        key = St.stripHtmlTags(key);
        String value = metadata.get(key);

        boolean skipMetadata = false;
        for (String ignoreMd : ignoredMetadata) {
          if (key.toLowerCase().equals(ignoreMd)) {
            skipMetadata = true;
            break;
          }
        }

        if (value != null && !value.trim().isEmpty() && !skipMetadata) {
          value = St.normalizeDocumentText(value);
          value = St.stripHtmlTags(value);
          value = value.replaceAll("\\s", " ");
          filteredMetadata.add(key, value);
        }
      }
    }
    return filteredMetadata;
  }
}
