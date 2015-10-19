package mj.ocraptor.database.search;

import static mj.ocraptor.configuration.Config.APP_NAME_LOWER;
import static mj.ocraptor.extraction.image_processing.TikaImageHelper.IMAGE_CONTAINER_CLASS;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.Localization;
import mj.ocraptor.configuration.properties.ConfigString;
import mj.ocraptor.console.COF;
import mj.ocraptor.console.ExtendedAscii;
import mj.ocraptor.database.StandardAnalyzer;
import mj.ocraptor.file_handler.utils.FileTools;
import mj.ocraptor.tools.St;
import mj.ocraptor.tools.Tp;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.util.Version;

public class TextProcessing {

  // ------------------------------------------------ //

  // *INDENT-OFF*
  private static final String
   FILEINFO_CLASS         = "fileInfo",
   FILEINFO_TITLE_CLASS   = "fileInfoTitle",
   FILEINFO_KEY_CLASS     = "key",
   // ------------------------------------------------ //
   METADATA_CLASS         = "metadata",
   METADATA_TITLE_CLASS   = "metadataTitle",
   // ------------------------------------------------ //
   PAGE_CLASS             = "page",
   SNIPPET_CLASS          = "highlightedSnippet",
   // ------------------------------------------------ //
   IMAGE_TITLE_CLASS      = "imageDataTitle",
   // ------------------------------------------------ //
   SCRIPT_TAG_OPEN        = "<script type=\"text/javascript\" src=\"",
   SCRIPT_TAG_CLOSE       = "\"></script>",

   STYLE_TAG_OPEN         = "<link rel=\"stylesheet\" type=\"text/css\" href=\"",
   STYLE_TAG_CLOSE        = "\"/>",

   FAVICON_TAG_OPEN       = "<link rel=\"shortcut icon\" type=\"image/x-icon\" href=\"",
   FAVICON_TAG_CLOSE      = "\"/>",

   PAGE_IND_CLASS         = "pageIndicator",
   XMLNS                  = "<div xmlns=\"http://www.w3.org/1999/xhtml\">",

   SNIPPET_ID_PREFIX      = "snippet_",

   STYLESHEET             = "<head>"                                           +
                            FAVICON_TAG_OPEN + "{0}/favicon.ico"               +
                            FAVICON_TAG_CLOSE                                  +
                            SCRIPT_TAG_OPEN + "{0}/jquery_2_1_4_min.js"        +
                            SCRIPT_TAG_CLOSE                                   +
                            SCRIPT_TAG_OPEN + "{0}/jquery_arbitrary_anchor.js" +
                            SCRIPT_TAG_CLOSE                                   +
                            SCRIPT_TAG_OPEN + "{0}/jquery_scrollup_min.js"     +
                            SCRIPT_TAG_CLOSE                                   +
                            SCRIPT_TAG_OPEN + "{0}/" + APP_NAME_LOWER + ".js"  +
                            SCRIPT_TAG_CLOSE                                   +
                            STYLE_TAG_OPEN  + "{0}/" + APP_NAME_LOWER + ".css" +
                            STYLE_TAG_CLOSE                                    +
                             "<script type=\"text/javascript\">"               +
                               "jQuery(document).ready(function() '{'"         +
                                  "jQuery(\"#scrollUp\").text(\"{1}\");"       +
                               "'}');"                                         +
                             "</script>"                                       +
                            "</head>",

   HTML_EXTENSION         = ".html",
   HTML_BREAKLINE         = "<br/>",
   METADATA_START         = "<div class=\"" + METADATA_CLASS + "\">";
  // *INDENT-ON*

  // ------------------------------------------------ //

  // *INDENT-OFF*
  public final static String
   SNIPPET_SYMBOL                      = ExtendedAscii.getAsciiAsString(174) + " ",

   PAGE_INDICATOR_OPEN                 = "<div class=\"" + PAGE_CLASS + "\"",
   PAGE_INDICATOR_CLOSED               = PAGE_INDICATOR_OPEN + ">",

   PAGE_INDICATOR_IMAGE_OPEN           = "<div class=\"" + IMAGE_CONTAINER_CLASS + "\"",
   PAGE_INDICATOR_IMAGE_CLOSED         = PAGE_INDICATOR_IMAGE_OPEN + ">",

   PAGE_INDICATOR_META_OPEN            = "<div class=\"" + METADATA_CLASS + "\"",
   PAGE_INDICATOR_META_CLOSED          = PAGE_INDICATOR_META_OPEN + ">",

   PAGE_INDICATOR_CUSTOM               = "<span page=\"",

   PAGE_ID                             = " id=\"page_",
   PAGE_INDICATOR_OPEN_WITH_ID         = PAGE_INDICATOR_OPEN        + PAGE_ID,
   PAGE_INDICATOR_META_OPEN_WITH_ID    = PAGE_INDICATOR_META_OPEN   + PAGE_ID,
   PAGE_INDICATOR_IMAGE_OPEN_WITH_ID   = PAGE_INDICATOR_IMAGE_OPEN  + PAGE_ID,

   OCR_IMAGE_BREAKLINE                 = "|",

   PAGE_MARK_STRIPPED                  = St.generatePassword(130, 32),
   PAGE_MARK_CLOSED                    = "<page>" + PAGE_MARK_STRIPPED + "</page>";
  // *INDENT-ON*

  private static final int MIN_SEARCH_HIT_DISTANCE = 200;
  private static final int XHTML_SCROLL_TIME_IN_MS = 1000;

  private static Analyzer luceneAnalyzer;
  private static QueryParser queryParser;

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   */
  static {
    // TokenStream tokenStream = new StandardTokenizer(Version.LUCENE_36, new
    // StringReader(string));

    List<String> stopWordsList = null;
    final String stopWordsString = Config.inst().getProp(ConfigString.STOP_WORDS);
    if (stopWordsString != null && !stopWordsString.trim().isEmpty()) {
      stopWordsList = new ArrayList<String>();
      for (String stopWord : stopWordsString.split(";")) {
        stopWord = stopWord.trim();
        if (!stopWord.isEmpty() && !stopWordsList.contains(stopWord)) {
          stopWordsList.add(stopWord);
        }
      }
    }

    // CharArraySet stopSet = CharArraySet.copy(Version.LUCENE_30,
    // StandardAnalyzer.STOP_WORDS_SET);
    CharArraySet stopSet = CharArraySet.copy(Version.LUCENE_30, new HashSet<String>(stopWordsList));
    final Version luceneVersion = Version.LUCENE_30;
    luceneAnalyzer = new StandardAnalyzer(luceneVersion, stopSet);
    queryParser = new QueryParser(luceneVersion, "_DATA", luceneAnalyzer);
    queryParser.setAllowLeadingWildcard(true);
  }

  /**
   *
   *
   * @param xml
   * @return
   */
  public static String preProcess(String xml) {
    // *INDENT-OFF*
    xml = xml.
      // ms excel fix, generates to many rows
      replace("<tr> </tr>",  "").
      replace("<tr></tr>",   "").
      replace("<tr/> <tr/>", "").
      replace("<tr/><tr/>",  "").
      // remove some some special characters
      replace("\ufffd", "").
      replace("\u25a0", "").
      replace("\u2022", "").
      // remove multiple punctuation
      replaceAll("(\\s?(\\.)\\s?)+", ".").
      replaceAll("(\\s?(\\,)\\s?)+", ",").
      replaceAll("\\|+", "|")
      ;
    xml = St.stripUrlTags(xml);
    xml = St.normalizeDocumentText(xml);
    // *INDENT-ON*
    return xml;
  }

  /**
   *
   *
   * @param xml
   * @return
   */
  public static String postProcess(String xml) {
    // TODO: performance testing
    // TODO: multiple postProcess call for one single file
    xml = St.stripHtmlTags(xml);
    xml = St.replaceLineBreaks(xml);
    xml = xml.replaceAll("\\s+", " ");
    return xml;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param text
   * @return
   */
  public static String encodePagePositions(final String text) {
    String encodedXml = null;
    try {
      encodedXml = encodePagePositions(text, PAGE_INDICATOR_OPEN, PAGE_INDICATOR_CLOSED);
      encodedXml = encodePagePositions(encodedXml, PAGE_INDICATOR_IMAGE_OPEN,
          PAGE_INDICATOR_IMAGE_CLOSED);
      encodedXml = encodePagePositions(encodedXml, PAGE_INDICATOR_META_OPEN,
          PAGE_INDICATOR_META_CLOSED);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return encodedXml;
  }

  /**
   *
   *
   * @return
   */
  private static String encodePagePositions(final String text, final String openIndicator,
      final String closedIndicator) throws Exception {
    // ------------------------------------------------ //
    int index = -1;
    final ArrayList<Integer> pageIndicatorIndex = new ArrayList<Integer>();

    while (!Thread.currentThread().isInterrupted()) {
      index = text.indexOf(closedIndicator, index + 1);
      if (index < 0) {
        break;
      }
      pageIndicatorIndex.add(index);
    }

    StringBuilder textWithTempPageMarker = new StringBuilder(text);
    for (int i = pageIndicatorIndex.size() - 1; i >= 0; i--) {
      Integer ind = pageIndicatorIndex.get(i) + closedIndicator.length();
      textWithTempPageMarker.insert(ind, PAGE_MARK_CLOSED);
    }

    // ------------------------------------------------ //

    final String postProcessedText = postProcess(textWithTempPageMarker.toString());
    final ArrayList<Integer> pageTempMarkerPosition = new ArrayList<Integer>();
    index = -1;
    while (!Thread.currentThread().isInterrupted()) {
      index = postProcessedText.indexOf(PAGE_MARK_STRIPPED, index + 1);
      if (index < 0) {
        break;
      }
      pageTempMarkerPosition.add(index);
    }

    // ------------------------------------------------ //

    final ArrayList<Integer> actualPagePositions = new ArrayList<Integer>();
    for (int i = pageTempMarkerPosition.size() - 1; i >= 0; i--) {
      Integer ind = pageTempMarkerPosition.get(i);
      int newIndex = ind - (i * PAGE_MARK_STRIPPED.length());
      actualPagePositions.add(newIndex);
    }

    // ------------------------------------------------ //

    String finalOutput = text;
    final Pattern pattern = Pattern.compile(PAGE_INDICATOR_CUSTOM + "(.+?)\"");
    for (int i = pageIndicatorIndex.size() - 1; i >= 0; i--) {
      int pageCount = (i + 1);
      if (openIndicator.equals(PAGE_INDICATOR_IMAGE_OPEN)) {
        final Matcher matcher = pattern.matcher(text.substring(pageIndicatorIndex.get(i)));
        if (matcher.find()) {
          final String page[] = matcher.group(1).split(":");
          if (page.length > 0) {
            try {
              pageCount = Integer.parseInt(page[0]);
            } catch (NumberFormatException e) {
            }
          }
        }
      }
      final String insert = PAGE_ID.concat(
          String.valueOf(actualPagePositions.get(pageIndicatorIndex.size() - (i + 1)))).concat(":")
          .concat(String.valueOf(pageCount)).concat("\"");
      final String part1 = StringUtils.substring(finalOutput, 0,
          pageIndicatorIndex.get(i) + openIndicator.length()).concat(insert);
      finalOutput = part1.concat(StringUtils.substring(finalOutput, pageIndicatorIndex.get(i)
          + openIndicator.length()));
    }

    // ------------------------------------------------ //

    // prettyPrint(finalOutput);
    return finalOutput;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   * @return the luceneAnalyzer
   */
  public static Analyzer getLuceneAnalyzer() {
    return luceneAnalyzer;
  }

  /**
   * @return the queryParser
   */
  public static QueryParser getQueryParser() {
    return queryParser;
  }

  /**
   * Last positions marks the metadata. e.g.: {0=1, 40=2, 3963=3, 7154=1} index
   * of metadata: '7154'
   *
   * @param xml
   * @return
   */
  public static SortedMap<Integer, PartialEntry> decodePagePositions(final String xml) {
    final SortedMap<Integer, PartialEntry> decodedPositions = decodePagePositions(xml,
        PartialEntryType.TEXTDATA);
    decodedPositions.putAll(decodePagePositions(xml, PartialEntryType.METADATA));
    decodedPositions.putAll(decodePagePositions(xml, PartialEntryType.IMAGEDATA));
    // System.out.println(decodedPositions);
    return decodedPositions;
  }

  /**
   *
   *
   * @param xml
   * @param pageTag
   * @return
   */
  public static SortedMap<Integer, PartialEntry> decodePagePositions(final String xml,
      final PartialEntryType type) {

    String pageTag = null;
    final SortedMap<Integer, PartialEntry> pagePositions = new TreeMap<Integer, PartialEntry>();

    if (type == PartialEntryType.TEXTDATA) {
      pageTag = PAGE_INDICATOR_OPEN_WITH_ID;
      pagePositions.put(0, new PartialEntry(1, type));
    } else if (type == PartialEntryType.METADATA) {
      pageTag = PAGE_INDICATOR_META_OPEN_WITH_ID;
    } else if (type == PartialEntryType.IMAGEDATA) {
      pageTag = PAGE_INDICATOR_IMAGE_OPEN_WITH_ID;
    }

    final Pattern pattern = Pattern.compile(pageTag + "(.+?)\"");
    final Matcher matcher = pattern.matcher(xml);

    // aa
    while (matcher.find()) {
      try {
        String pageID = matcher.group(1);
        if (pageID != null) {
          String[] parts = pageID.split(":");
          if (parts.length == 2) {
            Integer pagePosition = Integer.parseInt(parts[0]);
            Integer pageNumber = Integer.parseInt(parts[1]);
            pagePositions.put(pagePosition, new PartialEntry(pageNumber, type));
          }
        }
      } catch (NumberFormatException e) {
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return pagePositions;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param positionMap
   * @param index
   * @return
   */
  public static Integer getPagePosition(final SortedMap<Integer, PartialEntry> positionMap,
      final Integer index) {
    int bestPosition = 0;
    for (Integer position : positionMap.keySet()) {
      if (index >= position) {
        bestPosition = Math.max(position, bestPosition);
      }
    }
    return bestPosition;
  }

  /**
   *
   *
   * @param positionMap
   * @param index
   * @return
   */
  public static Integer getPage(final SortedMap<Integer, PartialEntry> positionMap,
      final Integer index) {
    final Integer bestPagePosition = getPagePosition(positionMap, index);
    PartialEntry partialEntry = positionMap.get(bestPagePosition);
    Integer page = null;

    if (partialEntry != null) {
      page = positionMap.get(bestPagePosition).getPage();
    }
    if (page == null) {
      page = 1;
    }
    return page;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param xhtml
   * @param fileName
   * @return
   */
  public static File saveXhtmlToFile(String xhtml, final String contentSearch, final File file) {
    final String normalizedFileName = St.normalizeFileName(file.getName());
    File tempFile = FileTools.getTempFile(normalizedFileName, HTML_EXTENSION, true);

    try {
      final String filePath = St.shortenHomePathInDirectory(FileTools.multiplatformPath(file));
      final String fileSizeInKB = String.valueOf(file.length() / 1024);

      // TODO: Performance problems if enabled
      final Query query = queryParser.parse(contentSearch);

      final String snippetMarker = "<span class=\"" + SNIPPET_CLASS + "\" id=\""
          + SNIPPET_ID_PREFIX + "";
      xhtml = COF.getHighlightedField(query, luceneAnalyzer, "", xhtml, snippetMarker + "\">",
          "</span>");
      xhtml = St.numberSubstrings(xhtml, snippetMarker, false);

      // StringUtils.replace(xhtml, OCR_IMAGE_BREAKLINE, HTML_BREAKLINE);
      xhtml = xhtml.replace(OCR_IMAGE_BREAKLINE, HTML_BREAKLINE);

      final List<Integer> indexList = St.getSubstringIndexAsList(xhtml, SNIPPET_CLASS);

      final StringBuilder anchorList = new StringBuilder();
      int lastIndex = 0, pseudoAnchorIndex = 1;
      for (int i = 0; i < indexList.size(); i++) {
        final Integer currentIndex = indexList.get(i);
        if (lastIndex == 0 || currentIndex - lastIndex > MIN_SEARCH_HIT_DISTANCE) {
          final Integer anchorIndex = i + 1;
          anchorList.append("<a href=\"##" + SNIPPET_ID_PREFIX + "" + anchorIndex + "|"
              + XHTML_SCROLL_TIME_IN_MS + "\">" + pseudoAnchorIndex++ + "</a> ");
        }
        lastIndex = currentIndex;
      }

      Localization lc = Localization.instance();

      // some basic file info like name and size for our header:
      // *INDENT-OFF*
      final String fileInfo =
          "<p class=\"" + FILEINFO_TITLE_CLASS + "\">"        +
            lc.getText("SEARCH_RESULT.VIEWED_FILE")            +
          "</p>"                                              +
          "<div class=\"" + FILEINFO_CLASS + "\">"            +
            "<table>"                                         +
              "<tr>"                                          +
                "<td class=\"" + FILEINFO_KEY_CLASS + "\">"   +
                  lc.getText("SEARCH_RESULT.PATH")             +
                "</td>"                                       +
                "<td>"                                        +
                  filePath                                    +
                "</td>"                                       +
              "</tr>"                                         +
              "<tr>"                                          +
                "<td class=\"" + FILEINFO_KEY_CLASS + "\">"   +
                  lc.getText("SEARCH_RESULT.SIZE")             +
                "</td>"                                       +
                "<td>"                                        +
                  lc.getText("SEARCH_RESULT.KB", fileSizeInKB) +
                "</td>"                                       +
              "</tr>"                                         +
              "<tr>"                                          +
                "<td class=\"" + FILEINFO_KEY_CLASS + "\">"   +
                  lc.getText("SEARCH_RESULT.QUERY")            +
                "</td>"                                       +
                "<td>"                                        +
                  contentSearch                               +
                "</td>"                                       +
              "</tr>"                                         +
              "<tr>"                                          +
                "<td class=\"" + FILEINFO_KEY_CLASS + "\">"   +
                  // TODO: text
                  "Snippets"                                  +
                "</td>"                                       +
                "<td>"                                        +
                  anchorList.toString()                       +
                "</td>"                                       +
              "</tr>"                                         +
            "</table>"                                        +
          "</div>";
      // *INDENT-ON*

      final String filesFolder = Config.inst().getTempFullTextStylesheetFolder().getName();
      final String styleSheetPath = MessageFormat.format(STYLESHEET, filesFolder, lc
          .getText("SEARCH_RESULT.SCROLLUP"));

      xhtml = xhtml.replace(XMLNS, XMLNS + styleSheetPath + fileInfo);

      final Pattern pattern = Pattern.compile(PAGE_INDICATOR_CUSTOM + "(.+?)\"");
      final Matcher matcher = pattern.matcher(xhtml);

      boolean imagesAreMarked = false;
      while (matcher.find()) {
        final String page[] = matcher.group(1).split(":");
        if (page.length > 0) {
          Integer parsedPage = null;
          try {
            parsedPage = Integer.parseInt(page[0]);
          } catch (NumberFormatException e) {
          }
          if (parsedPage != null) {
            final String pageString = lc.getText("SEARCH_RESULT.IMAGES_ON_PAGE", parsedPage);
            // System.out.println(parsedPage + " -- " + pageString);
            if (!xhtml.contains(pageString)) {
              final String indicator = "<div class=\"" + IMAGE_CONTAINER_CLASS + "\">"
                  + matcher.group(0);
              xhtml = xhtml.replaceFirst(indicator, "<p class=\"" + PAGE_IND_CLASS + "\">"
                  + pageString + "</p>" + indicator);
              imagesAreMarked = true;
            }
          }
        }
      }
      xhtml = xhtml.replace(METADATA_START, "<p class=\"" + METADATA_TITLE_CLASS + "\">"
          + lc.getText("SEARCH_RESULT.METADATA") + "</p>" + METADATA_START);

      if (!imagesAreMarked) {
        final String imageContainer = "<div class=\"" + IMAGE_CONTAINER_CLASS + "\">";
        String imageContainerTitle = "<p class=\"" + IMAGE_TITLE_CLASS + "\">"
            + lc.getText("SEARCH_RESULT.IMAGE_FILES") + "</p>" + imageContainer;
        xhtml = xhtml.replace(imageContainer, imageContainerTitle);
      }

      FileTools.stringToFile(xhtml, tempFile);
    } catch (IOException e) {
      // TODO log
      tempFile = null;
      e.printStackTrace();
    } catch (ParseException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (InvalidTokenOffsetsException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    return tempFile;
  }

  /**
   *
   *
   * @param text
   * @param startString
   * @param query
   * @param analyzer
   * @param maxSnippetLength
   * @param maxWidthForOneLine
   * @return
   */
  public static Tp<String[], Integer[]> prepareHighlight(String text, String startString,
      Query query, int maxSnippetLength, int maxHighlights, boolean fulltext) {

    Integer firstIndex = null;
    List<Integer> keys = null;
    List<Integer> firstIndices = new ArrayList<Integer>();
    List<String> snippets = new ArrayList<String>();

    try {
      String modifiedString = COF.getHighlightedField(query, luceneAnalyzer, "", text);
      if (modifiedString == null) {
        return null;
      }

      firstIndex = modifiedString.indexOf(Config.SEARCH_DELIMITER_START) - 1;

      if (firstIndex < 0) {
        firstIndex = 0;
        // firstIndex = null;
      }

      HashMap<Integer, String> tags = COF.getTagValues(modifiedString);

      // sort tags by their position
      keys = new ArrayList<Integer>(tags.keySet());
      class ScoreSorter implements Comparator<Integer> {
        @Override
        public int compare(Integer a, Integer b) {
          return a.compareTo(b);
        }
      }
      Collections.sort(keys, new ScoreSorter());

      int lastPrintedIndex = 0;
      int i = 0;

      List<Integer> tempIndices = new ArrayList<Integer>();
      int indices = 0;

      for (Integer ind : keys) {
        String firstHighlightString = tags.get(ind);

        if (ind + firstHighlightString.length() <= lastPrintedIndex)
          continue;

        if (i >= maxHighlights && maxHighlights <= 5) {
          break;
        }

        String snippetIndicator = "[...]";
        String[] temp = St.findSn(modifiedString, firstHighlightString, snippetIndicator, ind,
            maxSnippetLength);

        String snippet = (fulltext ? ExtendedAscii.getAsciiAsString(174) + " " : "")
            + St.normalizeDocumentText(temp[0] + firstHighlightString + temp[1]);

        tempIndices.add(ind);

        // TODO: stephen king
        if (i >= maxHighlights) {
          continue;
        }

        // ------------------------------------------------ //

        snippets.add(snippet);

        if (!firstIndices.contains(tempIndices.get(0))) {
          firstIndices.add(tempIndices.get(0) - indices);
          indices += Config.SEARCH_DELIMITER_START.length()
              * St.getSubstringOccurrence(snippet, Config.SEARCH_DELIMITER_START)
              + Config.SEARCH_DELIMITER_END.length()
              * St.getSubstringOccurrence(snippet, Config.SEARCH_DELIMITER_END);
        }

        tempIndices.clear();
        lastPrintedIndex = ind + firstHighlightString.length() + temp[1].length();

        i++;
      }
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }

    // *INDENT-OFF*
    return new Tp<String[], Integer[]>(
        snippets.toArray      (new String[snippets.size()]),
        firstIndices.toArray  (new Integer[firstIndices.size()])
      );
    // *INDENT-ON*
  }

  /**
   *
   *
   * @param fulltext
   * @param snippet
   *          snippet-string with highlight markers
   * @param positions
   * @param highlightedIndexFulltext
   * @param fileName
   * @return
   */
  // TODO: "there was"-search, no snippets found
  public static StyledSnippet highlightString(final String fulltext, String snippet,
      final SortedMap<Integer, PartialEntry> positions, Integer highlightedIndexFulltext,
      final String fileName) {

    final StyledSnippet styledSnippet = new StyledSnippet();

    if (snippet != null) {
      // ------------------------------------------------ //
      // don't color the snippet symbol
      if (snippet.startsWith(SNIPPET_SYMBOL)) {
        snippet = snippet.replaceFirst(Pattern.quote(SNIPPET_SYMBOL), "");
        styledSnippet.add(SNIPPET_SYMBOL, StyledSnippetType.START_INDICATOR);
      }
      // don't color the trimmed indicator at the start
      if (snippet.startsWith(St.TRIMMED_INDICATOR)) {
        snippet = snippet.replaceFirst(Pattern.quote(St.TRIMMED_INDICATOR), "");
        styledSnippet.add(St.TRIMMED_INDICATOR, StyledSnippetType.TRIMMED_INDICATOR);
      }
      boolean snippetEndsWithTrimmedIndicator = false;
      // don't color the trimmed indicator at the end
      if (snippet.endsWith(St.TRIMMED_INDICATOR)) {
        snippet = St.replaceLast(snippet, St.TRIMMED_INDICATOR);
        snippetEndsWithTrimmedIndicator = true;
      }
      // ------------------------------------------------ //

      final HashMap<Integer, String> tags = COF.getTagValues(snippet);
      // sort tags by their position
      List<Integer> keys = new ArrayList<Integer>(tags.keySet());
      class ScoreSorter implements Comparator<Integer> {
        @Override
        public int compare(Integer a, Integer b) {
          return a.compareTo(b);
        }
      }
      Collections.sort(keys, new ScoreSorter());

      // ------------------------------------------------ //
      Integer metadataIndexFulltext = 0;
      for (Integer positionIndex : positions.keySet()) {
        PartialEntry entry = positions.get(positionIndex);
        if (entry.getType() == PartialEntryType.METADATA) {
          metadataIndexFulltext = positionIndex;
        }
      }
      // ------------------------------------------------ //

      // *INDENT-OFF*
      int loopIndex                       = 0,
          indexAfterLastHighlightMarked   = 0,
          indexAfterLastHighlightUnmarked = 0,
          metadataIndex                   = 0,
          adjustedMetadataIndex           = 0;
      // *INDENT-ON*

      // ------------------------------------------------ //

      int snippetStartIndexFulltext = 0;
      if (!keys.isEmpty()) {
        snippetStartIndexFulltext = Math.abs(highlightedIndexFulltext - keys.get(0));
        if (metadataIndexFulltext != null) {
          metadataIndex = metadataIndexFulltext - snippetStartIndexFulltext;
          metadataIndex = metadataIndex < 0 ? 0 : metadataIndex;
        }
      }

      String cleanedFulltext = St.removeSearchDelimiter(fulltext);
      String cleanedSnippet = St.removeSearchDelimiter(snippet);
      // System.out.println(cleanedFulltext.substring(metadataIndex));

      // ------------------------------------------------ //

      // highlightedIndex := index of first highlight in current snippet (with
      // markers)
      for (Integer highlightedIndex : keys) {
        final String highlightedString = tags.get(highlightedIndex);
        try {
          // ------------------------------------------------ //
          String snippetPrefix = St.removeSearchDelimiter(snippet.substring(
              indexAfterLastHighlightMarked, highlightedIndex));

          adjustedMetadataIndex = metadataIndex - indexAfterLastHighlightUnmarked;

          indexAfterLastHighlightUnmarked += snippetPrefix.length()
              + St.removeSearchDelimiter(highlightedString).length();

          // ------------------------------------------------ //

          if (highlightedIndex >= 0) {
            if (snippetPrefix != null && !snippetPrefix.isEmpty()) {
              // ------------------------------------------------ //
              if (styledSnippet.getLength() >= metadataIndex + 4) {
                addLinebreak(styledSnippet);
                styledSnippet.add(snippetPrefix, StyledSnippetType.METADATA);
                snippetPrefix = null;
              } else if (snippetPrefix.length() > adjustedMetadataIndex) {
                final String unhighlightedSnippet = snippetPrefix.substring(0,
                    adjustedMetadataIndex);
                styledSnippet.add(unhighlightedSnippet, StyledSnippetType.FULLTEXT);
                addLinebreak(styledSnippet);
                final String metadataSnippet = TextProcessing.postProcess(snippetPrefix
                    .substring(adjustedMetadataIndex));
                styledSnippet.add(metadataSnippet, StyledSnippetType.METADATA);
                snippetPrefix = null;
              }
              // ------------------------------------------------ //
            }
          }

          // ------------------------------------------------ //

          if (snippetPrefix != null && !snippetPrefix.isEmpty()) {
            styledSnippet.add(snippetPrefix, StyledSnippetType.FULLTEXT);
          }
          indexAfterLastHighlightMarked = highlightedIndex + highlightedString.length();

          // ------------------------------------------------ //

          // snippet to highlight:
          String fragmentString = St.removeSearchDelimiter(highlightedString);
          styledSnippet.add(fragmentString, StyledSnippetType.HIGHLIGHT);

          // ------------------------------------------------ //
          // last string
          if (++loopIndex == keys.size()) {
            String suffix = St.removeSearchDelimiter(snippet.substring(
                indexAfterLastHighlightMarked, snippet.length()));
            snippetStartIndexFulltext = Math.abs(highlightedIndexFulltext - highlightedIndex);

            adjustedMetadataIndex = metadataIndex - indexAfterLastHighlightUnmarked;

            // ------------------------------------------------ //
            if (highlightedIndex >= 0 && metadataIndex >= 0 && suffix != null
                && !suffix.isEmpty()) {
              if (styledSnippet.getLength() >= metadataIndex + 4) {
                addLinebreak(styledSnippet);
                styledSnippet.add(suffix, StyledSnippetType.METADATA);
                suffix = null;
              } else if (suffix.length() > adjustedMetadataIndex) {
                String unhighlightedSnippet = suffix.substring(0, adjustedMetadataIndex);
                styledSnippet.add(unhighlightedSnippet, StyledSnippetType.FULLTEXT);
                addLinebreak(styledSnippet);
                String metadataSnippet = suffix.substring(adjustedMetadataIndex);
                styledSnippet.add(metadataSnippet, StyledSnippetType.METADATA);
                suffix = null;
              }
            }

            if (suffix != null) {
              styledSnippet.add(suffix, StyledSnippetType.FULLTEXT);
            }
          }
        } catch (Exception e) {
          // TODO: log
          System.out.println(ExceptionUtils.getStackTrace(e));
        }
      }

      if (snippetEndsWithTrimmedIndicator) {
        styledSnippet.add(St.TRIMMED_INDICATOR, StyledSnippetType.TRIMMED_INDICATOR);
      }
    }
    return styledSnippet;
  }

  /**
   *
   *
   * @param styledSnippet
   */
  public static void addLinebreak(StyledSnippet styledSnippet) {
    boolean metadataStarted = false, onlyConsistsOfMetadata = true;
    final List<Tp<String, StyledSnippetType>> snippets = styledSnippet.getSnippets();

    for (int i = snippets.size() - 1; i >= 0; i--) {
      if (snippets.get(i).getValue() == StyledSnippetType.FULLTEXT) {
        String fulltextSnippet = snippets.get(i).getKey();
        if (!fulltextSnippet.trim().isEmpty()) {
          onlyConsistsOfMetadata = false;
        }
      }
      if (snippets.get(i).getValue() == StyledSnippetType.METADATA) {
        metadataStarted = true;
      }
    }

    boolean trimmedIndicatorRemoved = false;
    if (!metadataStarted && !onlyConsistsOfMetadata) {
      for (int i = snippets.size() - 1; i >= 0; i--) {
        final String value = snippets.get(i).getKey().trim();
        final StyledSnippetType type = snippets.get(i).getValue();
        if (type == StyledSnippetType.FULLTEXT && !value.isEmpty()) {
          break;
        } else if (value.equals("|")) {
          snippets.remove(i);
        } else if (value.equals(St.TRIMMED_INDICATOR)) {
          snippets.remove(i);
          trimmedIndicatorRemoved = true;
        }
      }

      styledSnippet.add("\n", StyledSnippetType.FULLTEXT);
      if (trimmedIndicatorRemoved) {
        styledSnippet.add(St.TRIMMED_INDICATOR, StyledSnippetType.TRIMMED_INDICATOR);
      }
    }
  }

}
