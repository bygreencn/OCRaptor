package mj.ocraptor.console;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.console.Platform.Os;
import mj.ocraptor.database.DBFileStatus;
import mj.ocraptor.tools.St;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.fusesource.jansi.AnsiConsole;

public class COF {
  private static final int timeDifference = 200, MAX_LINE_WIDTH = 74;
  private static String mainDelimiter, startDelimiter, endDelimiter, progressProcentIndicator,
      progressFileIndicator;
  private static Long startTime = 0L;
  private static boolean quiet;

  private static String seperator, scriptName;

  public static final AnsiColor[] ERROR_CLR = { AnsiColor.RED_BACKGROUND, AnsiColor.WHITE };

  private static final String ID_COLOR = AnsiColor.RED.toString() + AnsiColor.BOLD.toString();

  // *INDENT-OFF*
  private static final String //
      MORE_INFO
       = "* Show more information with "
       + ID_COLOR
       + "s[id]"
       + AnsiColor.DEFAULT
       + ".\n   (e.g. '"
       + ID_COLOR
       + AnsiColor.BOLD.toString()
       + "s1"
       + AnsiColor.DEFAULT
       + "' for file with id=1)\n ",

      OPEN_FILE
       = "* Type only the "
       + ID_COLOR
       + "id"
       + AnsiColor.DEFAULT
       + " to open one specific file.\n ",

      OPEN_DIR
       = "* Show directory with "
       + ID_COLOR
       + "d[id]"
       + AnsiColor.DEFAULT
       + ".\n   (e.g. '"
       + ID_COLOR
       + "d1"
       + AnsiColor.DEFAULT
       + "' for file with id=1)\n ",

      OPEN_FULLTEXT
       = "* Open fulltext in your browser with "
       + ID_COLOR
       + "b[id]"
       + AnsiColor.DEFAULT
       + ".\n   (e.g. '"
       + ID_COLOR
       + "b1"
       + AnsiColor.DEFAULT
       + "' for file with id=1)\n ",

      QUIT
       = "* Type "
       + ID_COLOR
       + "q"
       + AnsiColor.DEFAULT
       +" to quit application.";
  // *INDENT-ON*

  /**
   *
   *
   */
  static {
    if (startTime == 0) {
      // don't install if running on an eclipse with 'Ansi Console'-Plugin
      if (!Config.devMode()) {
        AnsiConsole.systemInstall();
      }
      showedDots = 0;

      if (startTime == null)
        startTime = System.currentTimeMillis();

      if (Platform.getSystem() == Os.LINUX || Platform.getSystem() == Os.OSX) {
        // 220, 177, 176, 219, 218
        mainDelimiter = AnsiColor.BLUE_BACKGROUND.toString() + AnsiColor.WHITE.toString() + " "
            + AnsiColor.DEFAULT;
        startDelimiter = ExtendedAscii.getAsciiAsString(174);
        endDelimiter = ExtendedAscii.getAsciiAsString(173);
        progressProcentIndicator = AnsiColor.GREEN + ExtendedAscii.getAsciiAsString(253)
            + AnsiColor.DEFAULT;
        progressFileIndicator = AnsiColor.GREEN + ExtendedAscii.getAsciiAsString(253)
            + AnsiColor.DEFAULT;
        scriptName = Config.APP_NAME_LOWER + "-cl";
      } else {
        mainDelimiter = AnsiColor.BLUE_BACKGROUND.toString() + AnsiColor.WHITE.toString() + "*"
            + AnsiColor.DEFAULT;
        startDelimiter = Config.SEARCH_DELIMITER_END_SINGLE;
        endDelimiter = Config.SEARCH_DELIMITER_START_SINGLE;
        progressProcentIndicator = "|";
        progressFileIndicator = "|";
        scriptName = Config.APP_NAME + "CL";
      }

      // seperator = StringUtils.repeat(mainDelimiter, MAX_LINE_WIDTH);
      seperator = AnsiColor.GREEN + StringUtils.repeat("*", MAX_LINE_WIDTH) + AnsiColor.DEFAULT;
    }

  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param line
   */
  public static void printLine(final String line) {
    printLine(line, false);
  }

  /**
   *
   *
   * @param line
   */
  public static void printLineStretched(final String line) {
    printLine(line, false, AnsiColor.getColorIndex(line), true);
  }

  /**
   *
   *
   * @param line
   * @param end
   */
  public static void printLine(String line, boolean end) {
    printLine(line, end, AnsiColor.getColorIndex(line), false);
  }

  /**
   *
   *
   * @param line
   * @param end
   */
  public static void printLineStretched(String line, boolean end) {
    printLine(line, end, AnsiColor.getColorIndex(line), true);
  }

  /**
   *
   *
   * @param line
   * @param end
   * @param colors
   */
  private static synchronized void printLine(final String line, boolean end,
      final SortedMap<Integer, AnsiColor[]> colors, boolean stretched) {
    if (quiet)
      return;

    final boolean lineIsEmpty = line.trim().isEmpty();
    if (!blockEmptySeparator || !lineIsEmpty) {
      System.out.println(printLine(line, MAX_LINE_WIDTH, end, colors, stretched));
    }
    blockEmptySeparator = lineIsEmpty ? true : false;
    blockSeparator = false;
  }

  /**
   *
   *
   * @param line
   * @param maxLineWidth
   * @param end
   */
  private static String printLine(String line, int maxLineWidth, boolean end,
      final SortedMap<Integer, AnsiColor[]> colors, boolean stretched) {

    // colors = AnsiColor.getColorIndexAsSet(line);
    line = AnsiColor.removeAnsiColors(line);
    line = line.replaceAll("\\s", " ");
    line = St.trimToLengthIndicatorRight(line, maxLineWidth - 4);

    boolean endsWithIndicator = line.endsWith(St.TRIMMED_INDICATOR);
    int lineLength = line.length();

    String mainColor = AnsiColor.DEFAULT.toString();

    if (colors != null) {
      for (Entry<Integer, AnsiColor[]> colorEntry : colors.entrySet()) {
        int index = colorEntry.getKey();
        AnsiColor[] entryColors = colorEntry.getValue();
        for (int i = entryColors.length - 1; i >= 0; i--) {
          AnsiColor cl = entryColors[i];
          if (!stretched) {
            if (endsWithIndicator && index + 5 > lineLength) {
              line = St.insertStringAtIndex(line, AnsiColor.DEFAULT.toString(), lineLength - 5);
            } else {
              if (line.length() > index) {
                line = St.insertStringAtIndex(line, cl.toString(), index);
              }
            }
          } else {
            mainColor += cl.toString();
          }
        }
      }
    }
    // AnsiColor.printColorIndex(colors);

    // *INDENT-OFF*
    if (end) {
      return (
          mainDelimiter
          +
          mainColor
          +
          StringUtils.repeat(" ", maxLineWidth - lineLength - 3) + line
          +
          " "
          +
          AnsiColor.DEFAULT
          +
          mainDelimiter);
    } else {
      return (
          mainDelimiter
          +
          mainColor
          +
          " "
          +
          line + StringUtils.repeat(" ", maxLineWidth - lineLength - 4)
          +
          mainColor
          +
          " "
          +
          AnsiColor.DEFAULT
          +
          mainDelimiter
          );
    }
    // *INDENT-ON*
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

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
    System.out.println(seperator);
    if (errorMessage != null) {
      try {
        Thread.sleep(100);
        System.out.println("ERROR: " + errorMessage);
        Thread.sleep(50);
        System.out.println(seperator);
        Thread.sleep(200);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    // System.out.println(CommandLineInterpreter.instance().getOptions());
    helpFormater.printHelp(scriptName, "\noptions:", CommandLineInterpreter.instance().getOptions(),
        "\n", true);
    System.out.println(seperator);
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
    // extendedHelp.append("( TEXT=[<String>] AND FILETYPE=[<String>] ) OR
    // FILENAME=[<String>]\n");
    // extendedHelp.append("( TEXT=[account] AND FILETYPE=[xslx] ) OR
    // FILENAME=[march]\n");
    // extendedHelp.append("( TEXT=[fax] OR TEXT=[turnover] ) AND
    // FILENAME=[scan]\n");

    System.out.println(seperator);
    HelpFormatter helpFormater = new HelpFormatter();
    helpFormater.setOptionComparator(new OptionComparator());
    helpFormater.printHelp(scriptName, "\noptions:", CommandLineInterpreter.instance().getOptions(),
        "\n" + extendedHelp + "\n" + "\n", true);
    System.out.println(seperator);
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
      lineToRepeat = AnsiColor.CYAN_BACKGROUND + lineToRepeat + AnsiColor.DEFAULT;
      lineToRepeat = printLine(lineToRepeat, MAX_LINE_WIDTH, false, null, false);

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
      Thread.currentThread().setName(Config.APP_NAME + "Loading icon");
      COF.loadIcon(this.timeInMs);
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

  private static double lastPercent = 0;

  /**
   *
   *
   * @param fileCount
   * @param currentPosition
   */
  public static void printFile(Long fileCount, Long currentPosition, File file,
      DBFileStatus status) {
    boolean progressIndicator = fileCount != null && currentPosition != null;
    double percent = 0;

    // ------------------------------------------------ //
    if (file != null && status != null) {
      String message = "";
      String color = null;
      if (status == DBFileStatus.NOT_FOUND) {
        message = "Status:   Inserted into database";
        color = AnsiColor.GREEN.toString() + AnsiColor.BOLD.toString();
      } else if (status == DBFileStatus.UP_TO_DATE) {
        message = "Status:   File has not changed.";
        color = AnsiColor.BOLD.toString();
      } else {
        message = "Status:   File modified. Database entry updated.";
        color = AnsiColor.MAGENTA + AnsiColor.BOLD.toString();
      }

      COF.printEmptySeparator();
      printLine(color + "File:     " + file.getName());
      printLine(color + "Dir:      " + file.getParent());
      printLine(color + message);
      COF.printEmptySeparator();
    }
    // ------------------------------------------------ //
    if (progressIndicator) {
      if (fileCount == 0 && currentPosition == 0) {
        percent = 100;
      } else {
        percent = ((double) currentPosition / (double) fileCount * 100);
      }

      if (percent == 100) {
        percent = 99.99d;
      }

      if (percent > lastPercent) {
        COF.printEmptySeparator();
        printLine(AnsiColor.BOLD + "Progress: " + St.formatDouble(percent, 2) + "%");
        COF.printEmptySeparator();
        lastPercent = percent;
      }
    }
    // ------------------------------------------------ //
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

    blockEmptySeparator = false;
    blockSeparator = false;

    if (!checkTimeRestriction() && (fileCount - currentPosition) > 20)
      return;

    initProgressBar(fileCount);
    double pr = 0;

    if (fileCount == 0 && currentPosition == 0)
      pr = 1;
    else
      pr = (double) currentPosition / (double) fileCount;

    int newDotsCount = (int) Math.round(pr / ((double) 1 / ((double) progressBarHeight
        * (double) progressBarWidth)));
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
        System.out.print(mainDelimiter + StringUtils.repeat(" ", 4) + St.zeroPadSpaces(percent, 2)
            + "% " + startDelimiter + " ");
      }

      if ((currentProgress == (percent / (100 / progressBarWidth)) - 1) && percent != 100
          - resolution) {
        System.out.print(progressProcentIndicator);
      } else {
        System.out.print(progressFileIndicator);
      }

      if (showedDots % progressBarWidth == 0) {
        System.out.print(" " + endDelimiter);
        int surroundingChars = MAX_LINE_WIDTH - progressBarWidth - 20;
        System.out.print(" " + St.zeroPadSpaces(percent, 3) + "% " + StringUtils.repeat(" ",
            surroundingChars) + mainDelimiter + "\n");
      }
    }

    if (percent == 100) {
      printEmptySeparator();
    }
  }

  /**
   *
   *
   */
  public static void printScannerInfo() {
    printSeparator();
    printEmptySeparator();

    printText(MORE_INFO);
    printText(OPEN_DIR);
    printText(OPEN_FULLTEXT);
    printText(OPEN_FILE);
    printText(QUIT);

    printEmptySeparator();
    printSeparator();
    System.out.print("\n" + AnsiColor.BLUE.toString() + AnsiColor.BOLD.toString()
        + " -> Your input: ");
    System.out.print(AnsiColor.RED.toString());
  }

  private static long lastFileCount;

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

    if (count > 0 && lastFileCount != count) {
      printLine("File count: " + St.zeroPadSpaces((int) count, 5) + " ");
      lastFileCount = count;
    }
  }

  /**
   *
   *
   */
  public static void printExit() {
    COF.printFilledLine("Exiting " + Config.APP_NAME + " and cleaning up", true);
  }

  private static boolean blockEmptySeparator, blockSeparator;

  /**
   *
   *
   */
  public static void printSeparator() {
    if (quiet)
      return;

    if (!blockSeparator) {
      System.out.println(StringUtils.repeat(mainDelimiter, MAX_LINE_WIDTH));
      blockEmptySeparator = false;
    }
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
   * @param lines
   */
  public static void printLines(final String lines) {
    printLines(lines, false);
  }

  /**
   *
   *
   * @param lines
   */
  public static void printLinesStretched(final String lines) {
    printLines(lines, true);
  }

  /**
   *
   *
   * @param lines
   * @param stretched
   */
  private static void printLines(final String lines, final boolean stretched) {
    SortedMap<Integer, AnsiColor[]> colors = AnsiColor.getColorIndex(lines);
    final String[] partialTextStrings = St.splitToLines(lines);
    for (String singleLine : partialTextStrings) {
      // aa
      printLine(singleLine, false, colors, stretched);
      colors = AnsiColor.shiftColorIndex(colors, singleLine.length());
    }
  }

  /**
   *
   *
   * @param text
   */
  public static void printText(String text) {
    printText(text, false, true);
  }

  /**
   *
   *
   * @param text
   */
  public static void printTextStretched(String text) {
    printText(text, true, true);
  }

  /**
   *
   *
   * @param text
   */
  public static void printText(String text, boolean stretched, boolean showEmptyLines) {
    SortedMap<Integer, AnsiColor[]> cls = AnsiColor.getColorIndex(text);
    text = AnsiColor.removeAnsiColors(text);
    final String[] partialTextStrings = St.splitToLines(text);
    for (int i = 0; i < partialTextStrings.length; i++) {
      final String partialText = partialTextStrings[i];
      printPartialText(partialText, cls, stretched, showEmptyLines);
      cls = AnsiColor.shiftColorIndex(cls, partialText.length() + 1);
    }
  }

  /**
   *
   *
   * @param text
   * @param colors
   * @param stretched
   */
  private static void printPartialText(final String text, SortedMap<Integer, AnsiColor[]> colors,
      boolean stretched, boolean showEmptyLines) {
    List<String> strings = new ArrayList<String>();
    int index = 0;
    int lineWidth = MAX_LINE_WIDTH - 4;
    while (index < text.length()) {
      strings.add(text.substring(index, Math.min(index + lineWidth, text.length())));
      index += lineWidth;
    }
    for (String str : strings) {
      if (!str.trim().isEmpty() || showEmptyLines) {
        printLine(str, false, colors, stretched);
        colors = AnsiColor.shiftColorIndex(colors, str.length());
      }
    }
  }

  /**
   *
   *
   * @param line
   */
  public static void printFilledLine(String line) {
    printFilledLine(line, false);
  }

  /**
   *
   *
   * @param line
   * @param end
   */
  public static void printFilledLine(String line, boolean end) {
    if (quiet)
      return;
    line = St.trimToLengthIndicatorRight(line, MAX_LINE_WIDTH - 4);

    String colorString = AnsiColor.BLUE_BACKGROUND.toString() + AnsiColor.WHITE.toString();

    if (end) {
      System.out.println(StringUtils.repeat(mainDelimiter, MAX_LINE_WIDTH - line.length() - 3)
          + colorString + " " + line + " " + mainDelimiter + AnsiColor.DEFAULT);
    } else {
      System.out.println(mainDelimiter + " " + line + " " + StringUtils.repeat(mainDelimiter,
          MAX_LINE_WIDTH - line.length() - 3));
    }
    blockSeparator = true;
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
   * @param quiet
   *          the quiet to set
   */
  public static void setQuiet(boolean quiet) {
    COF.quiet = quiet;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

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
      Analyzer analyzer, int maxSnippetLength, int maxWidthForOneLine, int offset,
      int maxHighlights) {
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

        String snippetIndicator = AnsiColor.DEFAULT + "" + AnsiColor.GREEN + "[...]"
            + AnsiColor.DEFAULT;
        String[] temp = St.findSn(modifiedString, firstHighlightString, snippetIndicator, ind,
            maxSnippetLength);
        String snippet = temp[0] + firstHighlightString + temp[1];

        if (temp != null) {
          String tab = AnsiColor.GREEN + "  |" + StringUtils.repeat(" ", 6) + AnsiColor.DEFAULT;

          if (i == 0)
            output.append(startString);

          output.append(((i > 0) ? (output.length() > 0 ? "\n" : "") + tab : "") + St.breakToSpace(
              snippet, tab + " ", maxWidthForOneLine - ((i > 0) ? 0 : offset), maxWidthForOneLine));

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
      "[", "\\[") + "(.+?)" + Config.SEARCH_DELIMITER_END.replace("[", "\\["));

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
    return getHighlightedField(query, analyzer, fieldName, fieldValue,
        Config.SEARCH_DELIMITER_START, Config.SEARCH_DELIMITER_END);
  }

  /**
   *
   *
   * @param query
   * @param analyzer
   * @param fieldName
   * @param fulltext
   * @param startDelimiter
   * @param stopDelimiter
   * @return
   *
   * @throws IOException
   * @throws InvalidTokenOffsetsException
   */
  public static String getHighlightedField(Query query, Analyzer analyzer, String fieldName,
      String fulltext, final String startDelimiter, final String stopDelimiter) throws IOException,
          InvalidTokenOffsetsException {

    Formatter formatter = new SimpleHTMLFormatter(startDelimiter, stopDelimiter);
    QueryScorer queryScorer = new QueryScorer(query);
    Highlighter highlighter = new Highlighter(formatter, queryScorer);
    highlighter.setTextFragmenter(new SimpleSpanFragmenter(queryScorer, Integer.MAX_VALUE));
    highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);

    return highlighter.getBestFragment(analyzer, fieldName, fulltext);
  }

}
