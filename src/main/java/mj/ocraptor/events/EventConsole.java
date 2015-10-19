package mj.ocraptor.events;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;

import mj.ocraptor.MainController;
import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.Localization;
import mj.ocraptor.configuration.properties.ConfigString;
import mj.ocraptor.console.AnsiColor;
import mj.ocraptor.console.COF;
import mj.ocraptor.database.DBFileStatus;
import mj.ocraptor.database.dao.FileEntry;
import mj.ocraptor.database.search.LuceneResult;
import mj.ocraptor.database.search.PartialEntry;
import mj.ocraptor.database.search.StyledSnippet;
import mj.ocraptor.database.search.StyledSnippetType;
import mj.ocraptor.database.search.TextProcessing;
import mj.ocraptor.file_handler.utils.FileTools;
import mj.ocraptor.javafx.GUIController;
import mj.ocraptor.tools.St;
import mj.ocraptor.tools.Tp;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.apache.lucene.search.Query;

public class EventConsole extends EventAbstr {
  private static final int timeDifference = 500;
  private static Long startTime;
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
      .getLogger(EventConsole.class);

  private static final String ERROR_COLOR = AnsiColor.RED_BACKGROUND.toString()
      + AnsiColor.WHITE.toString();

  @Override
  protected void configFileNotFound(File invalidFile) {
    COF.printCLIHelp(FILE_NOT_FOUND_ERROR + " \n\"" + invalidFile.getAbsolutePath() + "\"");
  }

  @Override
  protected void startCountingFiles() {
    COF.printEmptySeparator();
    COF.printLine(AnsiColor.BOLD + "Starting to count files:");
  }

  @Override
  protected void countingFiles(Long currentCount, boolean finalCount) {
    COF.printFileCount(currentCount, finalCount);
  }

  @Override
  protected void configFileNameInvalid() {
    // TODO Auto-generated method stub
  }

  @Override
  protected void propertiesFileAlreadyExists() {
    // TODO Auto-generated method stub
  }

  /**
   *
   *
   * @return
   */
  private static boolean checkTimeRestriction() {
    if (startTime == null || System.currentTimeMillis() - startTime > timeDifference) {
      startTime = System.currentTimeMillis();
      return true;
    }
    return false;
  }

  /**
   * @param file
   * @param filesCount
   * @param processedCount
   * @param finalCount
   * @param status
   */
  @Override
  protected void printProcess(File file, Long filesCount, Long processedCount, boolean finalCount,
      DBFileStatus status) {

    //System.out.println(filesCount + " - " + processedCount); // TODO: DELETE IT!

    if (Config.inst().verbose() || (filesCount == null || processedCount == null)) {
      if (status != DBFileStatus.NOT_SUPPORTED
          && (finalCount || checkTimeRestriction() || file != null)) {
        COF.printFile(filesCount, processedCount, file, status);
        startTime = System.currentTimeMillis();
      } else if (checkTimeRestriction()) {
        COF.printFile(filesCount, processedCount, null, status);
        startTime = System.currentTimeMillis();
      }
    } else if (Config.inst().showProgress()) {
      COF.printProcess(filesCount, processedCount);
    }

    if (finalCount) {
      COF.printEmptySeparator();
      COF.printLine(AnsiColor.BOLD.toString() + "Progress: finished");
      COF.printEmptySeparator();
    }
  }

  @Override
  protected void cancelingIndexing() {
    // TODO Auto-generated method stub
  }

  @Override
  protected void printResult(final LuceneResult results, final String contentSearch,
      final Integer idToShow, final int maxSnippetLength) {

    // print results is heavy on the cpu, we don't want to run this twice
    if (GUIController.instance() != null || results == null) {
      return;
    }

    final Throwable throwable = results.getThrowable();
    final String error_color = AnsiColor.RED_BACKGROUND.toString() + AnsiColor.WHITE.toString();
    if (throwable != null) {
      COF.printEmptySeparator();
      COF.printText(error_color + Localization.instance().getText("ERROR.LUCENE_ERROR") + "\n ");
      COF.printText(error_color + ExceptionUtils.getRootCauseMessage(throwable));
      COF.printEmptySeparator();
    } else if (results.getFileEntries().isEmpty()) {
      COF.printEmptySeparator();
      COF.printText(error_color + Localization.instance().getText("SEARCH_RESULT.NO_RESULTS"));
      COF.printEmptySeparator();
    }

    Integer index = null;
    int pseudoID = 1;
    for (Map.Entry<FileEntry, Double> flEntry : results.getFileEntries()) {
      final Double score = flEntry.getValue();
      final FileEntry fileEntry = flEntry.getKey();

      SortedMap<Integer, PartialEntry> positions = null;
      try {
        String original = fileEntry.getFullTextString();
        final String encodedXml = TextProcessing.encodePagePositions(original);
        // TODO: cancel in cmd?
        // if (cancelResultGeneration)
        // return false;
        positions = TextProcessing.decodePagePositions(encodedXml);
      } catch (Exception e) {
        // TODO: logging
        e.printStackTrace();
      }

      final String fulltext = fileEntry.getFullTextString();
      if (fulltext != null && !fulltext.isEmpty() && contentSearch != null) {
        try {
          final Query query = TextProcessing.getQueryParser().parse(contentSearch);
          final String text = TextProcessing.postProcess(fulltext);
          if (text != null && !text.isEmpty()) {
            int snippetsToShow = 50;

            // *INDENT-OFF*
            Tp<String[], Integer[]> resultTupel =
              TextProcessing.prepareHighlight(
                  text,
                  "",
                  query,
                  idToShow == null ? maxSnippetLength : maxSnippetLength * 3,
                  snippetsToShow,
                  true
              );
            // *INDENT-ON*

            // ------------------------------------------------ //
            COF.printEmptySeparator();
            COF.printText(AnsiColor.BOLD + "FILE:  " + fileEntry.getFile().getName());
            COF.printText(AnsiColor.BOLD
                + "DIR:   "
                + St.shortenHomePathInDirectory(fileEntry.getFile().getParentFile()
                    .getAbsolutePath()));

            if (score != null) {
              COF.printText(AnsiColor.BOLD + "SCORE: " + score);
            }
            COF.printText(AnsiColor.BOLD + "ID:    " + pseudoID++);
            // ------------------------------------------------ //
            if (resultTupel == null) {
              COF.printText(AnsiColor.RED_BACKGROUND.toString() + AnsiColor.WHITE.toString()
                  + "Something went wrong, can not generate snippet");
              COF.printEmptySeparator();
              continue;
            }
            // ------------------------------------------------ //

            final String[] snippets = resultTupel.getKey();
            for (int i = 0; i < snippets.length; i++) {
              final String sn = snippets[i];
              index = resultTupel.getValue()[i];
              final StyledSnippet styledSnippet = TextProcessing.highlightString(text, sn,
                  positions, index, fileEntry.getFile().getName());
              String coloredSnipped = colorSnippet(styledSnippet);
              COF.printEmptySeparator();
              COF.printText(coloredSnipped);
            }
            COF.printEmptySeparator();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   *
   *
   * @param snippet
   * @return
   */
  private String colorSnippet(final StyledSnippet styledSnippet) {
    final StringBuilder finalStringBuilder = new StringBuilder();

    for (final Tp<String, StyledSnippetType> snippet : styledSnippet.getSnippets()) {
      final String finalSnippetString = snippet.getKey();
      final StyledSnippetType snippetType = snippet.getValue();

      finalStringBuilder.append(AnsiColor.DEFAULT);

      // aa
      if (snippetType == StyledSnippetType.FULLTEXT) {
      } else if (snippetType == StyledSnippetType.HIGHLIGHT) {
        finalStringBuilder.append(AnsiColor.BLUE.toString() + AnsiColor.BOLD.toString());
      } else if (snippetType == StyledSnippetType.LINE_SEPERATOR) {
        finalStringBuilder.append(AnsiColor.GREEN.toString());
      } else if (snippetType == StyledSnippetType.METADATA) {
        finalStringBuilder.append(AnsiColor.MAGENTA);
      } else if (snippetType == StyledSnippetType.IMAGE_TEXT) {
        finalStringBuilder.append(AnsiColor.RED.toString());
      } else if (snippetType == StyledSnippetType.START_INDICATOR) {
        finalStringBuilder.append(AnsiColor.GREEN.toString());
      } else if (snippetType == StyledSnippetType.TRIMMED_INDICATOR) {
        // TODO: not working for ending indicator, COF.printText removes
        // formatting
        // finalStringBuilder.append(AnsiColor.RED_BACKGROUND.toString());
      }

      finalStringBuilder.append(finalSnippetString);
      finalStringBuilder.append(AnsiColor.DEFAULT);
    }
    return finalStringBuilder.toString();
  }

  @Override
  protected void searchProgressIndicator(double value, String text) {
    //
  }

  public static final String CONSOLE_PATTERN = //
  "[%-5p]--%d{yyyy-MM-dd HH:mm:ss}--%c{1}%n[Thread:%t][%l]%n%m%n";

  // TODO: jbig2-processing problems
  public static final String[] KNOWN_ISSUES = { "jbig2" };

  @Override
  protected void initLoggerAppender(Logger logger) {
    String appenderName = "console appender";
    if (logger.getAppender(appenderName) == null) {
      logger.addAppender(new FormattedLogAppender());
    }
  }

  private class FormattedLogAppender extends AppenderSkeleton {
    @Override
    protected void append(LoggingEvent event) {
      String threshold = Config.inst().getProp(ConfigString.LOGFILE_THRESHOLD_OUTPUT);
      Priority thresholdLevel = Level.ERROR;

      if (threshold != null) {
        if (threshold.equals("FATAL")) {
          thresholdLevel = Level.FATAL;
        } else if (threshold.equals("ERROR")) {
          thresholdLevel = Level.ERROR;
        } else if (threshold.equals("INFO")) {
          thresholdLevel = Level.INFO;
        } else if (threshold.equals("DEBUG")) {
          thresholdLevel = Level.DEBUG;
        }
      }

      // ------------------------------------------------ //

      if (event != null && event.getLevel().isGreaterOrEqual(thresholdLevel)) {
        // ------------------------------------------------ //

        COF.printSeparator();
        COF.printEmptySeparator();

        try {
          String generalMessage = event.getRenderedMessage();
          COF.printLine(ERROR_COLOR + "[" + event.getLevel() + "] " + new Date() + "\n");

          if (generalMessage != null && !generalMessage.trim().isEmpty()) {
            COF.printText(ERROR_COLOR + "[MESSAGE] " + generalMessage + "\n");
          }

          ThrowableInformation info = event.getThrowableInformation();
          Throwable e = null;

          if (info != null) {
            e = info.getThrowable();
          }

          if (e == null) {
            COF.printEmptySeparator();
            return;
          }

          String stacktrace = ExceptionUtils.getStackTrace(e);

          for (String knownIssue : KNOWN_ISSUES) {
            if (stacktrace.contains(knownIssue)) {
              return;
            }
          }

          Integer linecount = null;
          try {
            linecount = FileTools.countTextfileLines(Config.inst().getMainLogFile())
                - St.countLines(stacktrace) + 1;
          } catch (Exception e2) {
          }

          COF.printEmptySeparator();
          // *INDENT-OFF*
          if (e.getMessage() != null) {
            COF.printText(
                ERROR_COLOR
                + (!e.getMessage().equals(generalMessage) ? "[DETAILS] " + e.getMessage(): "")
                + (e.getCause() != null ? "\n[CAUSE] " + e.getCause() : "")
                + "\n[LOGFILE] " + Config.inst().getMainLogAbsoluteFilePath()
                + (linecount != null ? "\n[LOGFILE-LINE] " + linecount : ""));
          }
          // *INDENT-ON*

          final String stack = ERROR_COLOR + "[FULL STACKTRACE]\n"
              + ExceptionUtils.getStackTrace(e);
          if (Config.inst().verbose()) {
            COF.printText(stack);
          } else {
            COF.printLines(stack);
          }

          COF.printEmptySeparator();

          // ------------------------------------------------ //

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void close() {
      // ---
    }

    public boolean requiresLayout() {
      return false;
    }

  }

  @Override
  protected void failedToProcessFile(String error, String filePath) {
    // TODO Auto-generated method stub
  }

  @Override
  protected void removingMissingFiles(Integer filesDeleted, Integer filesToLookAt) {
    if (filesDeleted == null && filesToLookAt != null) {
      COF.printEmptySeparator();
      COF.printLine(AnsiColor.BOLD + "Looking for missing files in database:");
      String filesToLookAtInfo = AnsiColor.BOLD + "[" + St.zeroPadSpaces(filesToLookAt, 4) + "]"
          + AnsiColor.DEFAULT + " files to look at";
      if (filesToLookAt > 500) {
        filesToLookAtInfo += " This could take a while";
      }
      COF.printLine(filesToLookAtInfo);
    }
    if (filesDeleted != null) {
      COF.printLine(AnsiColor.BOLD + "[" + St.zeroPadSpaces(filesDeleted, 4) + "]"
          + AnsiColor.DEFAULT + " files removed from your database");
    }
  }

  @Override
  protected void databaseConnectError(final Exception e, final String dataPath) {
    // *INDENT-OFF*
    // COF.printLines(
    //     "ERROR: failed to connect to database \""
    //     + dataPath
    //     + "\"\nSee error-message in the log file:\n"
    //     + Config.inst().getMainLogAbsoluteFilePath()  + "\n\n"
    //     + ExceptionUtils.getStackTrace(e)
    // );
    // *INDENT-OFF*

    LOGGER.error("Failed to connect to database", e);
    if (EventManager.instance().getEventHandlers().size() == 1) {
      MainController.inst().shutdown(false);
    }
  }

  @Override
  protected void ocrEngineDeployError(Exception e) {
  }

  @Override
  protected void serverStarted() {
    COF.printEmptySeparator();
    COF.printLine(AnsiColor.MAGENTA.toString() + AnsiColor.BOLD.toString() + "RMI Server started - listening for clients");
    COF.printEmptySeparator();
  }

  @Override
  protected void serverProblem(Exception e) {
    LOGGER.error("Can not connect server", e);
  }

  @Override
  protected void cantConnectToClients() {
  }
}
