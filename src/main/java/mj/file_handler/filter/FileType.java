package mj.file_handler.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import mj.configuration.Config;
import mj.configuration.properties.ConfigString;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.xml.sax.helpers.DefaultHandler;

public enum FileType {

  // ------------------------------------------------ //
  // -- image types
  // ------------------------------------------------ //

  // @formatter:off
  IMAGE_PNG   ("image/png:png"),
  IMAGE_JPEG  ("image/jpeg:jpg:jpeg"),
  IMAGE_GIF   ("image/gif:gif"),
  IMAGE_TIFF  ("image/tiff:tif:tiff"),
  IMAGE_BMP   ("image/x-ms-bmp:bmp"),
  // @formatter:on

  // ------------------------------------------------ //
  // -- microsoft office document types
  // ------------------------------------------------ //

  // @formatter:off
  MS_WORD("application/msword:doc"),
  MS_POWERPOINT("application/vnd.ms-powerpoint:ppt"),
  MS_EXCEL("application/vnd.ms-excel:xls"),
  MS_WORD_OXML("application/vnd.openxmlformats-officedocument"
    + ".wordprocessingml.document:docx"),
  MS_POWERPOINT_OXML("application/vnd.openxmlformats-officedocument"
    + ".presentationml.presentation:pptx"),
  MS_EXCEL_OXML("application/vnd.openxmlformats-officedocument"
    + ".spreadsheetml.sheet:xlsx"),
  MS_RTF("application/rtf:rtf"),
  MS_OXPS("application/vnd.ms-xpsdocument:xps"),
  MS_CHM("application/vnd.ms-htmlhelp:chm"),
  MS_ONE("application/vnd.ms-htmlhelp:chm"),
  // @formatter:on
  // ------------------------------------------------ //

  //

  // ------------------------------------------------ //
  // -- libreoffice document types
  // ------------------------------------------------ //
  LO_WRITER("application/vnd.oasis.opendocument.text:odt"), LO_CALC(
      "application/vnd.oasis.opendocument.spreadsheet:ods"), LO_IMPRESS(
      "application/vnd.oasis.opendocument.presentation:odp"),
  // ------------------------------------------------ //

  //

  // ------------------------------------------------ //
  // -- apple document types
  // ------------------------------------------------ //
  APPLE_PAGES("application/vnd.apple.pages:pages"), APPLE_KEY("application/vnd.apple.keynote:key"), APPLE_NUMBERS(
      "application/vnd.apple.numbers:numbers"),
  // ------------------------------------------------ //

  //

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  CODE_JAVA("text/x-java:java"), CODE_HTML("text/plain:html:htm:xhtml"), CODE_XHTML(
      "application/xhtml+xml:html:htm:xhtml"), CODE_BASH("application/x-shellscript:sh"), CODE_PEARL(
      "application/x-perl:pl"), CODE_CMD("text/x-csrc:cmd"), CODE_XSL("application/xslt+xml:xsl"), CODE_XML(
      "application/xml:xml"), CODE_CPP("text/x-c++src:cpp"), CODE_HPP("text/x-c++hdr:hpp"), CODE_PYTHON(
      "text/x-python:py"), CODE_PHP("application/x-php:php"), CODE_CSS("text/css:css"), CODE_JAVASCRIPT(
      "application/javascript:js:javascript"),

  //

  // ------------------------------------------------ //
  // -- misc
  // ------------------------------------------------ //
  UNKNOWN("unknown file type"),
  // ------------------------------------------------ //
  EPUB("application/epub+zip:epub"),
  // ------------------------------------------------ //
  XOJ("application/gzip:xoj"),
  // ------------------------------------------------ //
  PDF("application/pdf:pdf"), PS("application/postscript:ps"),
  // ------------------------------------------------ //
  CSV("text/csv"), MARKDOWN("text/x-markdown"), XML("application/xml"), TEXT("text/plain"), CALENDAR(
      "text/calendar");
  // ------------------------------------------------ //

  private String symbol;

  /**
   *
   *
   * @param symbol
   */
  private FileType(String symbol) {
    this.symbol = symbol;
  }

  /**
   * {@inheritDoc}
   *
   * @see Object#toString()
   */
  public String toString() {
    return getMimeString();
  }

  /**
   *
   *
   * @return
   */
  public String getMimeString() {
    return symbol.split(":")[0];
  }

  /**
   *
   *
   * @return
   */
  public MediaType getMediaType() {
    return MediaType.parse(getMimeString());
  }

  /**
   *
   *
   * @param mime
   * @return
   */
  public static FileType get(String mime) {
    FileType type = FileType.UNKNOWN;

    if (mime != null) {
      for (FileType fileType : FileType.values()) {
        if (mime.trim().equals(fileType.getMimeString())) {
          return fileType;
        }
      }
    }
    return type;
  }

  /**
   *
   *
   * @param file
   * @return
   */
  public static FileType get(File file) {
    if (file.exists()) {
      try {
        String[] textExtensions = Config.inst().getProp(
            ConfigString.TEXT_FILE_EXTENSIONS).split(";");

        if (isValidTextFile(file, textExtensions)) {
          return FileType.TEXT;
        }

        return get(FileType.getMimeFromFile(file));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return FileType.UNKNOWN;
  }

  /**
   *
   *
   * @param file
   * @return
   * @throws IOException
   */
  public static String getMimeFromFile(File file) throws IOException {
    String mime = null;
    InputStream io = null;
    try {
      final TikaConfig config = new TikaConfig(Config.inst().getTikaMimeFile());
      AutoDetectParser parser = new AutoDetectParser(config);
      parser.setParsers(new HashMap<MediaType, Parser>());

      Metadata metadata = new Metadata();
      metadata.add(TikaMetadataKeys.RESOURCE_NAME_KEY, file.getName());

      io = new FileInputStream(file);
      parser.parse(io, new DefaultHandler(), metadata, new ParseContext());
      mime = metadata.get(HttpHeaders.CONTENT_TYPE);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (io != null) {
        io.close();
      }
    }
    return mime;
  }

  /**
   *
   *
   * @param type
   * @param extension
   * @return
   *
   * @throws IOException
   */
  public static boolean isValidExtension(final FileType type, final String extension) {
    final String[] validExtensions = type.getExtensions();
    for (final String validExtension : validExtensions) {
      if (validExtension.equals(extension)) {
        return true;
      }
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
  public static boolean isValidImageFile(File file) throws IOException {
    if (is(file, IMAGE_PNG, true) || is(file, IMAGE_JPEG, true) || is(file, IMAGE_GIF, true)
        || is(file, IMAGE_TIFF, true) || is(file, IMAGE_BMP, true)) {
      return true;
    }
    return false;
  }

  /**
   *
   *
   * @param fileName
   * @return
   *
   * @throws IOException
   */
  public static boolean isValidImageFileExtension(String ext) throws IOException {
    if (isValidExtension(IMAGE_PNG, ext) //
        || isValidExtension(IMAGE_JPEG, ext)
        || isValidExtension(IMAGE_GIF, ext)
        || isValidExtension(IMAGE_TIFF, ext) || isValidExtension(IMAGE_BMP, ext)) {
      return true;
    }
    return false;
  }

  /**
   *
   *
   * @param mime
   * @return
   *
   * @throws IOException
   */
  public static boolean isValidImageMime(String mime) throws IOException {
    if (mime != null) {
      mime = mime.trim();
      if (IMAGE_PNG.toString().equals(mime) || IMAGE_JPEG.toString().equals(mime)
          || IMAGE_GIF.toString().equals(mime) || IMAGE_TIFF.toString().equals(mime)
          || IMAGE_BMP.toString().equals(mime)) {
        return true;
      }
    }
    return false;
  }

  /**
   *
   *
   * @return
   */
  public static boolean isValidTextFile(File file, String[] extensions) {
    if (extensions != null) {
      String fileExtension = FilenameUtils.getExtension(file.getName()).toLowerCase();
      for (String extensionToCheck : extensions) {
        if (fileExtension.equals(extensionToCheck.trim())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   *
   *
   * @param file
   * @param fileType
   * @return
   * @throws IOException
   */
  public static boolean is(File file, FileType fileType) throws IOException {
    return is(file, fileType, false);
  }

  /**
   *
   *
   * @param file
   * @param fileType
   * @param logInvalidFile
   * @return
   *
   * @throws IOException
   */
  public static boolean is(File file, FileType fileType, boolean logInvalidFile) throws IOException {
    if (logInvalidFile) {
      // TODO: log invalid mime-types
    }

    String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
    if (FileType.isValidExtension(fileType, extension)) {
      if (FileType.getMimeFromFile(file).equals(fileType.toString())) {
        return true;
      }
    }
    return false;
  }

  /**
   *
   *
   * @return
   */
  public String[] getExtensions() {
    return (String[]) ArrayUtils.remove(symbol.split(":"), 0);
  }
}
