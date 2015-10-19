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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigBool;
import mj.ocraptor.configuration.properties.ConfigInteger;
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

import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.Parser;
import org.xml.sax.SAXException;

public class TextExtractorTools {

  private Config cfg = null;

  /**
   *
   */
  public TextExtractorTools() {
    this.cfg = Config.inst();
  }

  /**
   *
   *
   * @param file
   * @return
   * @throws SAXException
   * @throws TikaException
   * @throws IOException
   */
  public boolean hasAvailableParsers(final File file) throws IOException, TikaException,
      SAXException {
    return getAvailableParsers(file, false).size() > 0 ? true : false;
  }

  /**
   *
   *
   * @param file
   * @return
   *
   * @throws IOException
   * @throws TikaException
   * @throws SAXException
   */
  public Map<MediaType, Parser> getAvailableParsers(final File file) throws IOException,
      TikaException, SAXException {
    return getAvailableParsers(file, true);
  }

  /**
   *
   *
   * @param file
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws TikaException
   */
  private Map<MediaType, Parser> getAvailableParsers(final File file, final boolean createParser)
      throws IOException, TikaException, SAXException {
    final Map<MediaType, Parser> availableParsers = new HashMap<MediaType, Parser>();

    // ------------------------------------------------ //
    // -- PDF
    // ------------------------------------------------ //
    if (this.cfg.getProp(ConfigBool.INCLUDE_PDF_FILES) && FileType.is(file, FileType.PDF, true)) {
      availableParsers.put(FileType.PDF.getMediaType(), createParser ? new PDFParser() : null);
    }

    // ------------------------------------------------ //
    // -- Xournal
    // ------------------------------------------------ //
    if (this.cfg.getProp(ConfigBool.INCLUDE_XOURNAL_FILES) && FileType.is(file, FileType.XOJ, true)) {
      availableParsers.put(FileType.XOJ.getMediaType(), createParser ? new XojParser() : null);
    }

    // ------------------------------------------------ //
    // -- WORD
    // ------------------------------------------------ //
    OfficeParser msOfficeParser = createParser ? new OfficeParser() : null;
    OOXMLParser msOfficeOOXMLParser = createParser ? new OOXMLParser() : null;

    if (this.cfg.getProp(ConfigBool.INCLUDE_MS_WORD_FILES)
        && (FileType.is(file, FileType.MS_WORD) || FileType.is(file, FileType.MS_WORD_OXML))) {
      availableParsers.put(FileType.MS_WORD.getMediaType(), msOfficeParser);
      availableParsers.put(FileType.MS_WORD_OXML.getMediaType(), msOfficeOOXMLParser);
    }

    // ------------------------------------------------ //
    // -- POWERPOINT
    // ------------------------------------------------ //
    final boolean includePPT = this.cfg.getProp( //
        ConfigBool.INCLUDE_MS_POWERPOINT_FILES);

    if (includePPT && (FileType.is(file, FileType.MS_POWERPOINT) //
        || FileType.is(file, FileType.MS_POWERPOINT_OXML))) {
      availableParsers.put(FileType.MS_POWERPOINT.getMediaType(), msOfficeParser);
      availableParsers.put(FileType.MS_POWERPOINT_OXML.getMediaType(), msOfficeOOXMLParser);
    }

    // ------------------------------------------------ //
    // -- EXCEL
    // ------------------------------------------------ //
    final boolean includeExcel = this.cfg.getProp( //
        ConfigBool.INCLUDE_MS_EXCEL_FILES);

    if (includeExcel && (FileType.is(file, FileType.MS_EXCEL) //
        || FileType.is(file, FileType.MS_EXCEL_OXML))) {
      availableParsers.put(FileType.MS_EXCEL.getMediaType(), msOfficeParser);
      availableParsers.put(FileType.MS_EXCEL_OXML.getMediaType(), msOfficeOOXMLParser);
    }

    // ------------------------------------------------ //
    // -- CHM
    // ------------------------------------------------ //

    // final boolean includeChm = this.cfg.getProp( //
    // ConfigBool.INCLUDE_MS_CHM_FILES);
    // if (includeChm && FileType.is(file, FileType.MS_CHM)) {
    // availableParsers.put(FileType.MS_CHM.getMediaType(), new ChmParser());
    // }

    // ------------------------------------------------ //
    // -- RTF
    // ------------------------------------------------ //

    final boolean includeRTF = this.cfg.getProp( //
        ConfigBool.INCLUDE_MS_RTF_FILES);
    if (includeRTF && FileType.is(file, FileType.MS_RTF)) {
      availableParsers.put(FileType.MS_RTF.getMediaType(), createParser ? new RTFParser() : null);
    }

    // ------------------------------------------------ //
    // -- HTML
    // ------------------------------------------------ //

    // TODO: ocr images
    final boolean includeHTML_XHTML = this.cfg.getProp( //
        ConfigBool.INCLUDE_HTML_FILES);
    HtmlParser htmlParser = createParser ? new HtmlParser() : null;

    if (includeHTML_XHTML
        && (FileType.is(file, FileType.CODE_HTML) || FileType.is(file, FileType.CODE_XHTML))) {
      availableParsers.put(FileType.CODE_HTML.getMediaType(), htmlParser);
      availableParsers.put(FileType.CODE_XHTML.getMediaType(), htmlParser);
    }

    // ------------------------------------------------ //
    // -- XML
    // ------------------------------------------------ //

    // TODO: ocr images
    final boolean includeXML = this.cfg.getProp( //
        ConfigBool.INCLUDE_XML_FILES);
    if (includeXML && (FileType.isValidXmlFile(file))) {
      availableParsers.put(FileType.CODE_XML.getMediaType(), createParser ? new XMLParser() : null);
    }

    // ------------------------------------------------ //
    // -- XPS
    // ------------------------------------------------ //

    final boolean includeXPS = this.cfg.getProp( //
        ConfigBool.INCLUDE_MS_XPS_FILES);
    if (includeXPS && FileType.is(file, FileType.MS_OXPS)) {
      availableParsers.put(FileType.MS_OXPS.getMediaType(), createParser ? new XPSParser() : null);
    }

    // ------------------------------------------------ //
    // -- LIBRE OFFICE
    // ------------------------------------------------ //
    final OpenDocumentParser odfParser = createParser ? new OpenDocumentParser() : null;

    if (this.cfg.getProp(ConfigBool.INCLUDE_LO_WRITER_FILES) && FileType.is(file, LO_WRITER, true)) {
      availableParsers.put(FileType.LO_WRITER.getMediaType(), odfParser);
    }
    if (this.cfg.getProp(ConfigBool.INCLUDE_LO_CALC_FILES) && FileType.is(file, LO_CALC, true)) {
      availableParsers.put(FileType.LO_CALC.getMediaType(), odfParser);
    }
    if (this.cfg.getProp(ConfigBool.INCLUDE_LO_IMPRESS_FILES)
        && FileType.is(file, LO_IMPRESS, true)) {
      availableParsers.put(FileType.LO_IMPRESS.getMediaType(), odfParser);
    }

    // ------------------------------------------------ //
    // -- APPLE
    // ------------------------------------------------ //

    final IWorkPackageParser appleParser = createParser ? new IWorkPackageParser() : null;
    if (this.cfg.getProp(ConfigBool.INCLUDE_APPLE_PAGES_FILES)
        && FileType.is(file, APPLE_PAGES, true)) {
      availableParsers.put(APPLE_PAGES.getMediaType(), appleParser);
    }
    if (this.cfg.getProp(ConfigBool.INCLUDE_APPLE_NUMBERS_FILES)
        && FileType.is(file, APPLE_NUMBERS, true)) {
      availableParsers.put(APPLE_NUMBERS.getMediaType(), appleParser);
    }
    if (this.cfg.getProp(ConfigBool.INCLUDE_APPLE_KEY_FILES) && FileType.is(file, APPLE_KEY, true)) {
      availableParsers.put(APPLE_KEY.getMediaType(), appleParser);
    }

    // ------------------------------------------------ //
    // -- IMAGE FILES
    // ------------------------------------------------ //
    final boolean includeImages = this.cfg.getProp(ConfigBool.INCLUDE_STANDALONE_IMAGE_FILES);
    if (includeImages && FileType.isValidImageFile(file)) {
      final ImageParser imageParser = createParser ? new ImageParser() : null;
      availableParsers.put(FileType.IMAGE_PNG.getMediaType(), imageParser);
      availableParsers.put(FileType.IMAGE_JPEG.getMediaType(), imageParser);
      availableParsers.put(FileType.IMAGE_GIF.getMediaType(), imageParser);
      availableParsers.put(FileType.IMAGE_TIFF.getMediaType(), imageParser);
      availableParsers.put(FileType.IMAGE_BMP.getMediaType(), imageParser);
    }

    // ------------------------------------------------ //
    // -- POSTSCRIPT FILES
    // ------------------------------------------------ //
    if (this.cfg.getProp(ConfigBool.INCLUDE_POSTSCRIPT_FILES) && FileType.is(file, PS, true)) {
      availableParsers.put(FileType.PS.getMediaType(), null);
      availableParsers.put(FileType.PDF.getMediaType(), createParser ? new PDFParser() : null);
    }

    // ------------------------------------------------ //
    // -- EPUB FILES
    // ------------------------------------------------ //
    if (this.cfg.getProp(ConfigBool.INCLUDE_EPUB_FILES) && FileType.is(file, EPUB, true)) {
      availableParsers.put(EPUB.getMediaType(), createParser ? new EpubParser() : null);
    }

    // ------------------------------------------------ //
    // -- TEXT FILES
    // ------------------------------------------------ //

    if (this.cfg.getProp(ConfigBool.INCLUDE_TEXT_FILES) && FileType.isValidTextFile(file)) {
      Boolean validSize = validSize(cfg.getProp(ConfigInteger.MAX_TEXT_SIZE_IN_KB), file);

      if (validSize == null || validSize) {
        availableParsers.put(FileType.TEXT.getMediaType(), createParser ? new TXTParser() : null);
      }
    }
    return availableParsers;
  }

  /**
   *
   *
   * @param maxSize
   * @param currentFile
   * @return
   */
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

}
