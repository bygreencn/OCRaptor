package mj.console;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mj.configuration.Config;
import mj.console.Platform.Os;
import mj.database.DBFileStatus;
import mj.database.SearchResult;
import mj.extraction.result.document.FileEntry;
import mj.extraction.result.document.FullText;
import mj.extraction.result.document.MetaData;
import mj.javafx.GUIController;
import mj.tools.StringTools;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.util.Version;
import org.fusesource.jansi.AnsiConsole;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ConsoleOutputFormatter {
  private static final int timeDifference = 200, maxLineWidth = 74;
  private static String mainDelimiter, startDelimiter, endDelimiter, progressProcentIndicator,
      progressFileIndicator;
  private static Long startTime;
  private static boolean quiet;
  private static Integer[] pseudoIDs;
  private static int resultSeperatorLength;

  private static final String CHILD_CHAR1 = AnsiColors.GREEN + "  |-- " + AnsiColors.DEFAULT;
  private static final String CHILD_CHAR2 = AnsiColors.GREEN + "  |  `-- " + AnsiColors.DEFAULT;

  /**
   *
   *
   */
  public static void init(boolean quiet) {
    ConsoleOutputFormatter.quiet = quiet;
    AnsiConsole.systemInstall();
    showedDots = 0;

    if (startTime == null)
      startTime = System.currentTimeMillis();

    if (Platform.getSystem() == Os.LINUX || Platform.getSystem() == Os.OSX) {
      // 220, 177, 176, 219, 218
      mainDelimiter = AnsiColors.RED + ExtendedAscii.getAsciiAsString(249) + AnsiColors.DEFAULT;
      startDelimiter = ExtendedAscii.getAsciiAsString(174);
      endDelimiter = ExtendedAscii.getAsciiAsString(173);
      progressProcentIndicator = AnsiColors.BLUE + ExtendedAscii.getAsciiAsString(253)
          + AnsiColors.DEFAULT;
      progressFileIndicator = AnsiColors.BLUE + ExtendedAscii.getAsciiAsString(253)
          + AnsiColors.DEFAULT;
    } else {
      mainDelimiter = AnsiColors.RED + "*" + AnsiColors.DEFAULT;
      startDelimiter = Config.SEARCH_DELIMITER_END_SINGLE;
      endDelimiter = Config.SEARCH_DELIMITER_START_SINGLE;
      progressProcentIndicator = "|";
      progressFileIndicator = "|";
    }
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //
  private static final String //
      CLASS = "indexer.jar", //
      SEPERATOR = StringUtils.repeat("*", 74), //

      HEADER = "--by default, test-files " + //
          "are generated for all available language specs.--\n";

  /**
   *
   *
   */
  public static void printCLIHelp() {
    printCLIHelp(null);
  }

  /**
   * Prints Help for Commandline-Interpreter.
   *
   * @param options
   *          possible options
   */
  public static void printCLIHelp(String errorMessage) {
    HelpFormatter helpFormater = new HelpFormatter();
    helpFormater.setOptionComparator(new OptionComparator());
    System.out.println(SEPERATOR);
    if (errorMessage != null) {
      try {
        Thread.sleep(100);
        System.out.println("ERROR: " + errorMessage);
        Thread.sleep(50);
        System.out.println(SEPERATOR);
        Thread.sleep(200);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    helpFormater.printHelp(CLASS, SEPERATOR, //
        CommandLineInterpreter.instance().getOptions(), SEPERATOR + "\n", true);
    System.exit(0);
  }

  /**
   * Prints Help for Commandline-Interpreter.
   *
   * @param options
   *          possible options
   */
  public static void printCLIExtendedHelp() {
    StringBuffer extendedHelp = new StringBuffer();
    extendedHelp.append("( TEXT=[<String>] AND FILETYPE=[<String>] ) OR FILENAME=[<String>]\n");
    extendedHelp.append("( TEXT=[account] AND FILETYPE=[xslx] ) OR FILENAME=[march]\n");
    extendedHelp.append("( TEXT=[fax] OR TEXT=[turnover] ) AND FILENAME=[scan]\n");

    System.out.println(SEPERATOR);
    HelpFormatter helpFormater = new HelpFormatter();
    helpFormater.setOptionComparator(new OptionComparator());
    helpFormater.printHelp(CLASS, SEPERATOR + "\n" + HEADER + SEPERATOR + "\n",
        CommandLineInterpreter.instance().getOptions(), SEPERATOR + "\n" + extendedHelp + "\n"
            + SEPERATOR + "\n", true);
    System.exit(0);
  }

  /**
   *
   *
   */
  public static void loadIcon(int timeInMs) {
    String[] icons = new String[] { "|", "/", "-", "\\" };

    AnsiConsole.out.print("\033[s");
    for (int i = 0; i < Math.round((double) timeInMs / (double) 100); i++) {
      String lineToRepeat = "LOADING: " + StringUtils.repeat(icons[i % icons.length], 4);
      int length = lineToRepeat.length();
      lineToRepeat = AnsiColors.CYAN_BACKGROUND + lineToRepeat + AnsiColors.DEFAULT;
      lineToRepeat = printLine(lineToRepeat, length, maxLineWidth, false);

      String ansiColor = "30";
      String line = "\033[u\033[" + lineToRepeat.length() + "D" + "\033[" + ansiColor + "m"
          + lineToRepeat + "\033[s\033[0;0f";

      AnsiConsole.out.print(line);
      AnsiConsole.out.flush();
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    AnsiConsole.out.print("\033[u");
  }

  /**
   *
   *
   * @param timeInMs
   */
  public static void loadIconInOwnThread(int timeInMs) {
    LoadIcon icon = new LoadIcon(timeInMs);
    new Thread(icon).start();
  }

  /**
   *
   */
  private static class LoadIcon implements Runnable {
    private int timeInMs;

    /**
     *
     */
    public LoadIcon(int timeInMs) {
      this.timeInMs = timeInMs;
    }

    @Override
    public void run() {
      ConsoleOutputFormatter.loadIcon(this.timeInMs);
    }
  }

  /**
   *
   */
  private static class OptionComparator implements Comparator<Option> {
    public int compare(Option opt1, Option opt2) {
      boolean hasArg1 = opt1.hasArg();
      boolean hasArg2 = opt2.hasArg();
      boolean required1 = opt1.isRequired();
      boolean required2 = opt2.isRequired();

      if (required1 && required2) {
        if (hasArg1 && hasArg2)
          return 0;
        if (hasArg1)
          return -1;
        if (hasArg2)
          return 1;
      }

      if (hasArg1 && hasArg2) {
        if (required1 && required2)
          return 0;
        if (required1)
          return -1;
        if (required2)
          return 1;
      }

      if (hasArg1 && !hasArg2)
        return -1;
      if (required1 && !required2)
        return -1;
      return 0;
    }
  }

  /**
   *
   *
   * @return
   */
  private static boolean checkTimeRestriction() {
    if (System.currentTimeMillis() - startTime > timeDifference) {
      startTime = System.currentTimeMillis();
      return true;
    }
    return false;
  }

  /**
   *
   *
   * @param fileCount
   */
  private static void initProgressBar(long fileCount) {
    if (progressBarWidth == null) {
      if (fileCount <= 100) {
        progressBarWidth = 20;
        progressBarHeight = 5;
      } else if (fileCount > 100 && fileCount <= 500) {
        progressBarWidth = 50;
        progressBarHeight = 10;
      } else if (fileCount > 500 && fileCount <= 1000) {
        progressBarWidth = 50;
        progressBarHeight = 20;
      } else if (fileCount > 1000 && fileCount <= 2500) {
        progressBarWidth = 50;
        progressBarHeight = 50;
      } else if (fileCount > 2500) {
        progressBarWidth = 50;
        progressBarHeight = 100;
      }
    }
  }

  /**
   *
   *
   * @param fileCount
   * @param currentPosition
   */
  public static void printFile(Long fileCount, Long currentPosition, File file, DBFileStatus status) {
    boolean progressIndicator = fileCount != null && currentPosition != null;
    if (progressIndicator) {
      double percent;
      if (fileCount == 0 && currentPosition == 0) {
        percent = 100;
      } else {
        percent = ((double) currentPosition / (double) fileCount * 100);
      }
      printLine("PROGRESS:  " + StringTools.formatDouble(percent, 2) + "%");
    }
    if (file != null) {
      printLine("DIR:  " + file.getParent());
      printLine("FILE: " + file.getName());

      if (status != null) {
        if (status == DBFileStatus.NOT_FOUND) {
          printLine("STATUS: Inserted into database");
        } else if (status == DBFileStatus.UP_TO_DATE) {
          printLine("STATUS: File has not changed.");
        } else {
          printLine("STATUS: File modified. Database entry updated.");
        }
      }
    }
    if (file != null || progressIndicator) {
      printSeparator();
    }
  }

  private static int showedDots;
  private static Integer progressBarWidth, progressBarHeight;

  /**
   *
   *
   * @param fileCount
   * @param currentPosition
   */
  public static synchronized void printProcess(long fileCount, long currentPosition) {

    if (quiet)
      return;

    if (!checkTimeRestriction() && (fileCount - currentPosition) > 20)
      return;

    initProgressBar(fileCount);
    double pr = 0;

    if (fileCount == 0 && currentPosition == 0)
      pr = 1;
    else
      pr = (double) currentPosition / (double) fileCount;

    int newDotsCount = (int) Math.round(pr
        / ((double) 1 / ((double) progressBarHeight * (double) progressBarWidth)));
    int newDotsToShow = newDotsCount - showedDots;
    int percent = 0;

    if (currentPosition == 0 && fileCount != 0) {
      printEmptySeparator();
    }

    for (int i = 0; i < newDotsToShow; i++) {
      int currentProgress = (showedDots % progressBarWidth);
      showedDots++;
      int resolution = (100 / progressBarHeight);
      percent = (showedDots / progressBarWidth) * resolution;
      if (showedDots % progressBarWidth == 1) {
        System.out.print(mainDelimiter + StringUtils.repeat(" ", 4)
            + StringTools.zeroPadSpaces(percent, 2) + "% " + startDelimiter + " ");
      }

      if ((currentProgress == (percent / (100 / progressBarWidth)) - 1)
          && percent != 100 - resolution) {
        System.out.print(progressProcentIndicator);
      } else {
        System.out.print(progressFileIndicator);
      }

      if (showedDots % progressBarWidth == 0) {
        System.out.print(" " + endDelimiter);
        int surroundingChars = maxLineWidth - progressBarWidth - 20;
        System.out.print(" " + StringTools.zeroPadSpaces(percent, 3) + "% "
            + StringUtils.repeat(" ", surroundingChars) + mainDelimiter + "\n");
      }
    }

    if (percent == 100) {
      printEmptySeparator();
      printSeparator();
    }
  }

  private static final String //
      MORE_INFO = "Show more information with 's[id]'. (e.g. 's1' for file with id [1]).", //
      OPEN_FILE = "Type only the id to open one specific file.",//
      OPEN_DIR = "Show directory with 'd[id]' (e.g. 'd2' for file with id [2]).",//
      QUIT = "Type 'q' to quit application.";

  /**
   *
   *
   */
  public static void printScannerInfo() {
    printSeparator();
    printLine(MORE_INFO);
    printLine(OPEN_DIR);
    printLine(OPEN_FILE);
    printLine(QUIT);
    printSeparator();
    System.out.println();
  }

  /**
   *
   *
   * @param count
   */
  public static void printFileCount(long count) {
    printFileCount(count, false);
  }

  /**
   *
   *
   * @return
   */
  public static void printFileCount(long count, boolean lastOutput) {
    if (quiet)
      return;
    if (!checkTimeRestriction() && !lastOutput)
      return;

    if (count > 2) {
      printFilledLine("File count: " + StringTools.zeroPadSpaces((int) count, 10) + " ");
    }
    if (lastOutput) {
      ConsoleOutputFormatter.printSeparator();
    }
  }

  /**
   *
   *
   */
  public static void printSeparator() {
    if (quiet)
      return;
    System.out.println(StringUtils.repeat(mainDelimiter, maxLineWidth));
  }

  /**
   *
   *
   * @param maxLineWidth
   */
  public static String printSeparator(int maxLineWidth) {
    return StringUtils.repeat(mainDelimiter, maxLineWidth);
  }

  /**
   *
   *
   */
  public static void printEmptySeparator() {
    printLine("", false);
  }

  /**
   *
   *
   * @param line
   */
  public static void printLine(String line) {
    printLine(line, false);
  }

  /**
   *
   *
   * @param line
   * @param end
   */
  public static void printLine(String line, boolean end) {
    if (quiet)
      return;
    System.out.println(printLine(line, line.length(), maxLineWidth, end));
  }

  /**
   *
   *
   * @param line
   * @param maxLineWidth
   * @param end
   * @return
   */
  private static String printDetailedResultLine(String line) {
    return printLine(line, line.length(), resultSeperatorLength, false);
  }

  /**
   *
   *
   * @param line
   * @param maxLineWidth
   * @param end
   */
  private static String printLine(String line, int length, int maxLineWidth, boolean end) {
    line = StringTools.trimToLengthIndicatorRight(line, maxLineWidth - 4);
    if (end) {
      return (mainDelimiter + StringUtils.repeat(" ", maxLineWidth - length - 3) + line + " " + mainDelimiter);
    } else {
      return (mainDelimiter + " " + line + " " + StringUtils.repeat(" ", maxLineWidth - length - 4) + mainDelimiter);
    }
  }

  /**
   *
   *
   * @param line
   */
  public static void printFilledLine(String line) {
    if (quiet)
      return;
    line = StringTools.trimToLengthIndicatorRight(line, maxLineWidth - 4);
    System.out.println(mainDelimiter + " " + line + " "
        + StringUtils.repeat(mainDelimiter, maxLineWidth - line.length() - 3));
  }

  /**
   *
   *
   */
  public static void printEnd() {
    if (quiet)
      return;
    printSeparator();
    printLine("[EXIT]", true);
    printSeparator();
  }

  /**
   *
   *
   * @param results
   * @param searchString
   */
  public static void printDetails(List<String[]> results, String[] searchStringParts, int id) {
  }

  /**
   *
   *
   * @param id
   */
  public static void printWrongIDMessage(int id) {
    System.out.println("ERROR: given id \"" + id + "\" does not exist!\n");
  }

  /**
   * @return the pseudoIDs
   */
  public static Integer[] getPseudoIDs() {
    return pseudoIDs;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  private static final int maxWidthForOneLine = 60;

  /**
   *
   *
   * @param results
   * @param metaDataSearch
   * @param contentSearch
   * @param idToShow
   * @param maxMetaDataLength
   * @param maxSnippetLength
   */
  public static void printResult(final SearchResult results, String metaDataSearch,
      String contentSearch, Integer idToShow, int maxMetaDataLength, int maxSnippetLength) {

    // TODO:
    if (GUIController.instance() != null) {
      return;
    }

    String output = getResultString(results, metaDataSearch, contentSearch, idToShow,
        maxMetaDataLength, maxSnippetLength);

    if (output != null) {
      HashMap<Integer, String> tags = getTagValues(output);
      List<Integer> keys = new ArrayList<Integer>(tags.keySet());
      class FullTextSorter implements Comparator<Integer> {
        @Override
        public int compare(Integer a, Integer b) {
          return b.compareTo(a);
        }
      }
      Collections.sort(keys, new FullTextSorter());

      for (Integer ind : keys) {
        String highlightedString = tags.get(ind);
        output = output.substring(0, ind)
            + highlightedString.replace(Config.SEARCH_DELIMITER_START,
                AnsiColors.BLUE_BACKGROUND + "" + AnsiColors.WHITE).replace(
                Config.SEARCH_DELIMITER_END, AnsiColors.DEFAULT.toString())
            + output.substring(ind + highlightedString.length());
      }
      output = output.replace(Config.SEARCH_DELIMITER_START, "");
      output = output.replace(Config.SEARCH_DELIMITER_END, "");

      System.out.println("\n\n" + output);
    }
  }

  /**
   *
   *
   * @param result
   * @return
   */
  public static String getResultString(final SearchResult results, String metaDataSearch,
      String contentSearch, Integer idToShow, int maxMetaDataLength, int maxSnippetLength) {
    StringBuffer out = new StringBuffer();

    // if (metaDataSearch != null) {
    // metaDataSearch = StringTools.replaceUmlaute(metaDataSearch, true);
    // }
    // if (contentSearch != null) {
    // contentSearch = StringTools.replaceUmlaute(contentSearch, true);
    // }

    // TODO:
    if (results == null)
      return null;

    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_30);
    QueryParser qp = new QueryParser(Version.LUCENE_30, "", analyzer);
    qp.setAllowLeadingWildcard(true);

    SortedSet<Map.Entry<FileEntry, Double>> elements = results.getFileEntries();
    maxSnippetLength += Config.SEARCH_DELIMITER_START.toString().length()
        + Config.SEARCH_DELIMITER_END.toString().length();

    int currentPseudoId = 0;

    try {
      for (Map.Entry<FileEntry, Double> flEntry : elements) {
        currentPseudoId++;

        if (idToShow != null && !idToShow.equals(currentPseudoId))
          continue;

        FileEntry fileEntry = flEntry.getKey();
        String prefix = "[" + currentPseudoId + "] FILE: ";
        out.append(prefix
            + "\n"
            + CHILD_CHAR2
            + " ["
            + AnsiColors.GREEN.toString()
            + StringTools.addLineBreaks(
                StringTools.shortenHomePathInDirectory(fileEntry.getPath()), "\n  "
                    + AnsiColors.GREEN + "|" + AnsiColors.GREEN
                    + StringUtils.repeat(" ", prefix.length() - 3), 50)
            + AnsiColors.DEFAULT.toString() + "]\n");
        List<MetaData> metadataList = fileEntry.getMetadata();

        if (metadataList != null && !metadataList.isEmpty()) {

          out.append(CHILD_CHAR1 + "METADATA: " + "\n");
          for (MetaData md : metadataList) {
            String content = "[" + md.getKey() + " | " + md.getValue() + "]";

            // content = StringTools.replaceUmlaute(content, true);

            Query mdquery = metaDataSearch != null ? qp.parse(metaDataSearch) : qp
                .parse(contentSearch);

            String output = prepareHighlight(content, CHILD_CHAR2.toString(), mdquery, analyzer,
                maxSnippetLength, maxWidthForOneLine, 0, 1);
            out.append(StringTools.removeLastLineBreak(output) + "\n");
          }
        }
        List<FullText> fullTextObjects = fileEntry.getFullTextObjects();
        if (fullTextObjects != null && !fullTextObjects.isEmpty() && contentSearch != null) {
          out.append(CHILD_CHAR1 + "SNIPPETS:" + "\n");
          Query query = qp.parse(contentSearch);
          for (FullText fullText : fullTextObjects) {
            Document doc = Jsoup.parse(fullText.getText());
            String text = doc.text();

            // text = StringTools.replaceUmlaute(text, true);

            // TODO: pagination
            String fullTextIndicator = "";

            if (text != null && !text.isEmpty()) {
              // String snippetNo = CHILD_CHAR2 + " " + AnsiColors.GREEN +
              // elementType
              // + fullTextIndicator + ":" + AnsiColors.DEFAULT;

              int offset = 100;
              String output = prepareHighlight(text, "", query, analyzer,
                  idToShow == null ? maxSnippetLength : maxSnippetLength * 3, maxWidthForOneLine,
                  offset, (idToShow == null ? 1 : 50));

              out.append(output + "\n");
            }
            if (idToShow == null)
              break;
          }
        }
        out.append("\n");
      }
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }
    return out.toString();
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
  public static String prepareHighlight(String text, String startString, Query query,
      Analyzer analyzer, int maxSnippetLength, int maxWidthForOneLine, int offset, int maxHighlights) {
    StringBuffer output = new StringBuffer();
    try {
      String modifiedString = getHighlightedField(query, analyzer, "", text);

      HashMap<Integer, String> tags = getTagValues(modifiedString);
      List<Integer> keys = new ArrayList<Integer>(tags.keySet());
      class FullTextSorter implements Comparator<Integer> {
        @Override
        public int compare(Integer a, Integer b) {
          return a.compareTo(b);
        }
      }
      Collections.sort(keys, new FullTextSorter());

      int lastPrintedIndex = 0;
      int i = 0;

      for (Integer ind : keys) {
        if (i >= maxHighlights)
          break;

        String firstHighlightString = tags.get(ind);

        if (ind + firstHighlightString.length() < lastPrintedIndex)
          continue;

        String snippetIndicator = AnsiColors.DEFAULT + "" + AnsiColors.GREEN + "[...]"
            + AnsiColors.DEFAULT;
        String[] temp = StringTools.findSn(modifiedString, firstHighlightString, snippetIndicator,
            ind, maxSnippetLength);
        String snippet = temp[0] + firstHighlightString + temp[1];

        if (temp != null) {
          String tab = AnsiColors.GREEN + "  |" + StringUtils.repeat(" ", 6) + AnsiColors.DEFAULT;

          if (i == 0)
            output.append(startString);

          output.append(((i > 0) ? (output.length() > 0 ? "\n" : "") + tab : "")
              + StringTools.breakToSpace(snippet, tab + " ", maxWidthForOneLine
                  - ((i > 0) ? 0 : offset), maxWidthForOneLine));

          if (maxHighlights > 0) {
            output.append("\n");
          }

          lastPrintedIndex = ind + firstHighlightString.length() + temp[1].length();
        }
        i++;
      }
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }

    return output.toString();
  }

  private static final Pattern TAG_REGEX = Pattern.compile(Config.SEARCH_DELIMITER_START.replace(
      "[", "\\[")
      + "(.+?)" + Config.SEARCH_DELIMITER_END.replace("[", "\\["));

  /**
   *
   *
   * @param str
   * @return
   */
  public static HashMap<Integer, String> getTagValues(final String str) {
    final HashMap<Integer, String> tagValues = new HashMap<Integer, String>();
    if (str != null) {
      final Matcher matcher = TAG_REGEX.matcher(str);
      while (matcher.find()) {
        tagValues.put(matcher.start(), Config.SEARCH_DELIMITER_START + matcher.group(1)
            + Config.SEARCH_DELIMITER_END);
      }
    }
    return tagValues;
  }

  /**
   *
   *
   * @param query
   * @param analyzer
   * @param fieldName
   * @param fieldValue
   * @return
   *
   * @throws IOException
   * @throws InvalidTokenOffsetsException
   */
  public static String getHighlightedField(Query query, Analyzer analyzer, String fieldName,
      String fieldValue) throws IOException, InvalidTokenOffsetsException {

    Formatter formatter = new SimpleHTMLFormatter(Config.SEARCH_DELIMITER_START,
        Config.SEARCH_DELIMITER_END);
    QueryScorer queryScorer = new QueryScorer(query);
    Highlighter highlighter = new Highlighter(formatter, queryScorer);
    highlighter.setTextFragmenter(new SimpleSpanFragmenter(queryScorer, Integer.MAX_VALUE));
    highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
    return highlighter.getBestFragment(analyzer, fieldName, fieldValue);
  }

}
