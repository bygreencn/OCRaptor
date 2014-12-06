package mj.file_handler;

import static mj.file_handler.filter.FileType.APPLE_KEY;
import static mj.file_handler.filter.FileType.APPLE_NUMBERS;
import static mj.file_handler.filter.FileType.APPLE_PAGES;
import static mj.file_handler.filter.FileType.EPUB;
import static mj.file_handler.filter.FileType.LO_CALC;
import static mj.file_handler.filter.FileType.LO_IMPRESS;
import static mj.file_handler.filter.FileType.LO_WRITER;
import static mj.file_handler.filter.FileType.PS;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import mj.MainController;
import mj.configuration.Config;
import mj.configuration.properties.ConfigBool;
import mj.configuration.properties.ConfigInteger;
import mj.configuration.properties.ConfigString;
import mj.database.DBFileStatus;
import mj.database.DBManager;
import mj.events.EventManager;
import mj.extraction.result.ExtractionResult;
import mj.extraction.result.document.FileEntry;
import mj.extraction.result.document.FileEntryDao;
import mj.extraction.result.document.MetaData;
import mj.extraction.tika.parser.chm.ChmParser;
import mj.extraction.tika.parser.epub.EpubParser;
import mj.extraction.tika.parser.html.HtmlParser;
import mj.extraction.tika.parser.image.ImageParser;
import mj.extraction.tika.parser.iwork.IWorkPackageParser;
import mj.extraction.tika.parser.microsoft.OfficeParser;
import mj.extraction.tika.parser.microsoft.XPSParser;
import mj.extraction.tika.parser.microsoft.ooxml.OOXMLParser;
import mj.extraction.tika.parser.odf.OpenDocumentParser;
import mj.extraction.tika.parser.pdf.PDFParser;
import mj.extraction.tika.parser.rtf.RTFParser;
import mj.extraction.tika.parser.txt.TXTParser;
import mj.extraction.tika.parser.xml.XMLParser;
import mj.extraction.tika.parser.xoj.XojParser;
import mj.file_handler.filter.FileType;
import mj.tools.AsciiTools;
import mj.tools.StringTools;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextExtractorThread implements Runnable {
  private DBManager indexDB;
  private File currentFile;
  private static Long fileCount;
  private static Long processedCount;
  private String[] textFileExtensionsFromUser;
  private Config cfg;

  private DBFileStatus currentFileStatus;
  private FileEntry currentDBFileEntry;
  private String currentFileMD5Hash;
  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private FileType fileType;
  private FileEntryDao fileEntryDao;
  private Connection connection;

  /**
   *
   *
   * @param indexDB
   * @param file
   * @param properties
   */
  public TextExtractorThread(DBManager indexDB, File file) {
    this.indexDB = indexDB;
    this.currentFile = file;
  }

  /**
   * {@inheritDoc}
   *
   * @see Runnable#run()
   */
  public void run() {
    long threadId = Thread.currentThread().getId();
    MainController mainController = MainController.inst();
    try {
      if (Config.inst().isShutdown())
        return;

      while (Config.inst().isPaused())
        Thread.sleep(200);

      if (!mainController.getCurrentFileWorkers().contains(currentFile)) {
        mainController.getCurrentFileWorkers().put(threadId, currentFile);
      }

      this.init();
      this.fileType = FileType.get(currentFile);

      if (mainController.showInitialCPUList()) {
        Thread.sleep(10);
        showProgress(null, currentFileStatus);
        mainController.setShowInitialCPUList(false);
      }

      ExtractionResult resultFromTika = extractTextTika(currentFile);
      processedCount++;

      if ((resultFromTika == null && this.currentFileStatus == DBFileStatus.UP_TO_DATE)
          || resultFromTika != null) {
        showProgress(currentFile, currentFileStatus);
      } else {
        showProgress(null, currentFileStatus);
      }

      if (resultFromTika != null && !this.cfg.isShutdown()) {
        this.saveToDatabase(resultFromTika);
      }

      // this.indexDB.getH2DB().printTables();
    } catch (RuntimeException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (mainController.getCurrentFileWorkers().containsKey(threadId)) {
        mainController.getCurrentFileWorkers().remove(threadId);
      }
      if (this.connection != null) {
        try {
          this.connection.close();
          mainController.fileProcessed();
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   *
   *
   * @param metadata
   * @param file
   */
  private void addStandardMetadata(Metadata metadata, File file) {
    if (metadata != null && file != null) {
      if (metadata.get(Metadata.CONTENT_TYPE) == null) {
        metadata.set(Metadata.CONTENT_TYPE, fileType.getMimeString());
      }

      metadata.set(MetaData.FILE_NAME, file.getName());
      metadata.set(MetaData.FILE_PATH, file.getParent());

      // metadata.set(Metadata.CONTENT_ENCODING, "utf-8");
      // metadata.add(Metadata.CONTENT_ENCODING, "utf-8");
    }
  }

  /**
   *
   *
   * @param fileCount
   * @param verbose
   */
  public static synchronized void setFileCount(long fileCount, boolean verbose) {
    if (TextExtractorThread.fileCount == null) {
      TextExtractorThread.fileCount = fileCount;
      verbose = false;
    }
  }

  /**
   *
   *
   */
  public static synchronized void resetCount() {
    TextExtractorThread.fileCount = null;
    TextExtractorThread.processedCount = null;
  }

  /**
   *
   *
   * @param file
   * @return
   */
  private boolean hasUnknownFileHash(File file) {
    currentFileStatus = getFileStatus(file);
    if (currentFileStatus == DBFileStatus.NOT_FOUND || currentFileStatus == DBFileStatus.MODIFIED) {
      return true;
    }
    return false;
  }

  /**
   *
   *
   * @param file
   * @return
   * @throws IOException
   */
  private static String calculateMD5FromFile(File file) {
    String md5 = null;
    try {
      FileInputStream fis = new FileInputStream(file);
      md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return md5;
  }

  /**
   *
   *
   * @param file
   * @param valid
   */
  public static synchronized void showProgress(File file, DBFileStatus status) {
    if (status == null) {
      return;
    }

    EventManager eventManager = EventManager.instance();
    eventManager.printProcess(file, fileCount, processedCount, false, status);
  }

  /**
   *
   *
   * @param file
   * @return
   */
  public DBFileStatus getFileStatus(File file) {
    this.currentFileMD5Hash = calculateMD5FromFile(file);
    this.currentDBFileEntry = indexDB.findMD5Hash(file.getAbsolutePath());
    if (this.currentDBFileEntry == null) {
      return DBFileStatus.NOT_FOUND;
    }

    String md5FromDB = this.currentDBFileEntry.getHash();
    if (md5FromDB == null || md5FromDB.trim().isEmpty()) {
      return DBFileStatus.NOT_FOUND;
    } else if (!md5FromDB.equals(currentFileMD5Hash)) {
      return DBFileStatus.MODIFIED;
    }
    return DBFileStatus.UP_TO_DATE;
  }

  /**
   *
   *
   * @param sizeRestriction
   * @return
   */
  private Boolean validSize(Integer maxSize) {
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

  /**
   *
   *
   */
  private void init() {
    this.cfg = Config.inst();
    this.connection = indexDB.getConnection();
    if (cfg != null) {
      String textFileExtensionsFromProperties = cfg.getProp(ConfigString.TEXT_FILE_EXTENSIONS);
      if (textFileExtensionsFromProperties != null) {
        textFileExtensionsFromUser = textFileExtensionsFromProperties.split(";");
      }
    }
    if (processedCount == null) {
      processedCount = 0L;
    }

    this.fileEntryDao = new FileEntryDao();
  }

  /**
   *
   *
   * @param file
   * @return
   */
  private ExtractionResult extractTextTika(final File file) {

    InputStream inputStream = null;
    StringWriter stringWriter = null;
    ExtractionResult result = null;
    File tempFile = null;

    // System.out.println(file + " " + FileType.getMimeFromFile(file));

    try {
      this.currentFileStatus = DBFileStatus.NOT_SUPPORTED;

      // ------------------------------------------------ //
      // -- do not index the given database-folder
      // ------------------------------------------------ //

      if (file.getAbsolutePath().startsWith(new File(indexDB.getDatabaseDir()).getAbsolutePath())) {
        return null;
      }

      final TikaConfig config = new TikaConfig(Config.inst().getTikaMimeFile());
      final AutoDetectParser autoDetectParser = new AutoDetectParser(config);
      final Map<MediaType, Parser> availableParsers = new HashMap<MediaType, Parser>();

      // ------------------------------------------------ //
      // -- PDF
      // ------------------------------------------------ //
      if (this.cfg.getProp(ConfigBool.INCLUDE_PDF_FILES) && FileType.is(file, FileType.PDF, true)) {
        availableParsers.put(FileType.PDF.getMediaType(), new PDFParser());
      }

      // ------------------------------------------------ //
      // -- Xournal
      // ------------------------------------------------ //
      if (this.cfg.getProp(ConfigBool.INCLUDE_XOURNAL_FILES)
          && FileType.is(file, FileType.XOJ, true)) {
        availableParsers.put(FileType.XOJ.getMediaType(), new XojParser());
      }

      // ------------------------------------------------ //
      // -- WORD
      // ------------------------------------------------ //
      OfficeParser msOfficeParser = new OfficeParser();
      OOXMLParser msOfficeOOXMLParser = new OOXMLParser();

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

      final boolean includeChm = this.cfg.getProp( //
          ConfigBool.INCLUDE_MS_CHM_FILES);
      if (includeChm && FileType.is(file, FileType.MS_CHM)) {
        availableParsers.put(FileType.MS_CHM.getMediaType(), new ChmParser());
      }

      // ------------------------------------------------ //
      // -- RTF
      // ------------------------------------------------ //

      final boolean includeRTF = this.cfg.getProp( //
          ConfigBool.INCLUDE_MS_RTF_FILES);
      if (includeRTF && FileType.is(file, FileType.MS_RTF)) {
        availableParsers.put(FileType.MS_RTF.getMediaType(), new RTFParser());
      }

      // ------------------------------------------------ //
      // -- HTML
      // ------------------------------------------------ //

      // TODO: ocr images
      final boolean includeHTML_XHTML = this.cfg.getProp( //
          ConfigBool.INCLUDE_HTML_FILES);
      HtmlParser htmlParser = new HtmlParser();

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
      XMLParser xmlParser = new XMLParser();
      if (includeXML && (FileType.is(file, FileType.CODE_XML))) {
        availableParsers.put(FileType.CODE_XML.getMediaType(), xmlParser);
      }

      // ------------------------------------------------ //
      // -- XPS
      // ------------------------------------------------ //

      final boolean includeXPS = this.cfg.getProp( //
          ConfigBool.INCLUDE_MS_XPS_FILES);
      if (includeXPS && FileType.is(file, FileType.MS_OXPS)) {
        availableParsers.put(FileType.MS_OXPS.getMediaType(), new XPSParser());
      }

      // ------------------------------------------------ //
      // -- LIBRE OFFICE
      // ------------------------------------------------ //
      final OpenDocumentParser odfParser = new OpenDocumentParser();
      if (this.cfg.getProp(ConfigBool.INCLUDE_LO_WRITER_FILES)
          && FileType.is(file, LO_WRITER, true)) {
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

      final IWorkPackageParser appleParser = new IWorkPackageParser();
      if (this.cfg.getProp(ConfigBool.INCLUDE_APPLE_PAGES_FILES)
          && FileType.is(file, APPLE_PAGES, true)) {
        availableParsers.put(APPLE_PAGES.getMediaType(), appleParser);
      }
      if (this.cfg.getProp(ConfigBool.INCLUDE_APPLE_NUMBERS_FILES)
          && FileType.is(file, APPLE_NUMBERS, true)) {
        availableParsers.put(APPLE_NUMBERS.getMediaType(), appleParser);
      }
      if (this.cfg.getProp(ConfigBool.INCLUDE_APPLE_KEY_FILES)
          && FileType.is(file, APPLE_KEY, true)) {
        availableParsers.put(APPLE_KEY.getMediaType(), appleParser);
      }

      // ------------------------------------------------ //
      // -- IMAGE FILES
      // ------------------------------------------------ //
      final boolean includeImages = this.cfg.getProp(ConfigBool.INCLUDE_STANDALONE_IMAGE_FILES);
      if (includeImages && FileType.isValidImageFile(file)) {
        availableParsers.put(FileType.IMAGE_PNG.getMediaType(), new ImageParser());
        availableParsers.put(FileType.IMAGE_JPEG.getMediaType(), new ImageParser());
        availableParsers.put(FileType.IMAGE_GIF.getMediaType(), new ImageParser());
        availableParsers.put(FileType.IMAGE_TIFF.getMediaType(), new ImageParser());
        availableParsers.put(FileType.IMAGE_BMP.getMediaType(), new ImageParser());
      }

      // ------------------------------------------------ //
      // -- POSTSCRIPT FILES
      // ------------------------------------------------ //
      if (this.cfg.getProp(ConfigBool.INCLUDE_POSTSCRIPT_FILES) && FileType.is(file, PS, true)) {
        availableParsers.put(FileType.PS.getMediaType(), null);
        availableParsers.put(FileType.PDF.getMediaType(), new PDFParser());
      }

      // ------------------------------------------------ //
      // -- EPUB FILES
      // ------------------------------------------------ //
      if (this.cfg.getProp(ConfigBool.INCLUDE_EPUB_FILES) && FileType.is(file, EPUB, true)) {
        availableParsers.put(EPUB.getMediaType(), new EpubParser());
      }

      // ------------------------------------------------ //
      // -- TEXT FILES
      // ------------------------------------------------ //
      if (this.cfg.getProp(ConfigBool.INCLUDE_TEXT_FILES)
          && FileType.isValidTextFile(file, textFileExtensionsFromUser)) {
        Boolean validSize = validSize(cfg.getProp(ConfigInteger.MAX_TEXT_SIZE_IN_KB));

        if (validSize == null || validSize) {
          availableParsers.put(FileType.TEXT.getMediaType(), new TXTParser());
        }
      }

      // ------------------------------------------------ //
      // --
      // ------------------------------------------------ //
      final List<MediaType> supportedFileTypes = new ArrayList<MediaType>(availableParsers.keySet());

      if (!supportedFileTypes.contains(fileType.getMediaType())) {
        return null;
      }

      boolean unknownFile = hasUnknownFileHash(file);

      if (!unknownFile) {
        return null;
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
        LOG.error("Problematic file: \"" + file.getAbsolutePath() + "\"", e);
      }
      // ------------------------------------------------ //

      String xhtml = stringWriter.toString();

      // TODO:
      if (xhtml != null && !xhtml.trim().isEmpty()) {
        metadata = normalizeMetadata(metadata);

        xhtml = xhtml.replace("xmlns=\"http://www.w3.org/1999/xhtml\"", "");
        StringBuilder builder = new StringBuilder();
        builder.append(xhtml);

        builder.append("<div class=\"metadata\">");
        for (String md : metadata.names()) {
          if (md != null && !md.trim().isEmpty()) {
            String value = metadata.get(md);
            if (value != null && !value.trim().isEmpty()) {
              builder.append("<p> Meta=( " + md + " | " + value + " ) </p>");
            }
          }
        }
        builder.append("</div>");
        xhtml = builder.toString();
        xhtml = StringTools.normalizeDocumentText(xhtml);

        result = new ExtractionResult(file);
        result.addElementsFromXhtml(xhtml);
        if (this.cfg.getProp(ConfigBool.INCLUDE_METADATA)) {
          result.addMetaData(metadata);
        }
        // System.out.println(xhtml);
      }
    } catch (Exception e) {
      LOG.error("Problematic file: \"" + file.getAbsolutePath() + "\"", e);
      // TODO:
      e.printStackTrace();
      if (result == null) {
        result = new ExtractionResult(file);
        result.addElementsFromXhtml("ERROR");
      }
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

  /**
   *
   *
   * @param metadata
   * @return
   */
  private Metadata normalizeMetadata(Metadata metadata) {
    Metadata filteredMetadata = new Metadata();
    for (String key : metadata.names()) {
      if (key != null && !key.trim().isEmpty()) {
        key = StringTools.normalizeDocumentText(key);
        key = StringTools.stripHtmlTags(key);
        String value = metadata.get(key);
        if (value != null && !value.trim().isEmpty()) {
          value = StringTools.normalizeDocumentText(value);
          value = StringTools.stripHtmlTags(value);
          value = value.replaceAll("\\s", " ");
          filteredMetadata.add(key, value);
        }
      }
    }
    return filteredMetadata;
  }

  /**
   *
   *
   * @param file
   * @param xhtml
   */
  private void saveToDatabase(ExtractionResult result) {
    try {
      hasUnknownFileHash(currentFile);

      if (currentFileStatus == null) {
        // TODO:
        throw new IllegalArgumentException();
      }

      if (currentFileStatus == DBFileStatus.NOT_SUPPORTED
          || currentFileStatus == DBFileStatus.UP_TO_DATE) {
        return;
      }

      // TODO: process result string before db insert???
      if (currentFileStatus == DBFileStatus.NOT_FOUND) {
        fileEntryDao.insert(result.generateEntry(), connection);
      }

      if (currentFileStatus == DBFileStatus.MODIFIED) {
        FileEntry entryDoUpdate = currentDBFileEntry;
        fileEntryDao.update(result.generateEntry(entryDoUpdate), connection);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
