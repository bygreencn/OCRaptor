package mj.events;

import java.io.File;

import mj.configuration.Config;
import mj.console.ConsoleOutputFormatter;
import mj.database.DBFileStatus;
import mj.database.SearchResult;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class EventConsole extends EventAbstr {
  private static final int timeDifference = 500;
  private static Long startTime;

  @Override
  protected void configFileNotFound(File invalidFile) {
    ConsoleOutputFormatter.printCLIHelp(FILE_NOT_FOUND_ERROR + " \n\""
        + invalidFile.getAbsolutePath() + "\"");
  }

  @Override
  protected void countingFiles(Long currentCount, boolean finalCount) {
    ConsoleOutputFormatter.printFileCount(currentCount, finalCount);
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

    if (Config.inst().verbose() || (filesCount == null || processedCount == null)) {
      if (status != DBFileStatus.NOT_SUPPORTED && (finalCount || checkTimeRestriction() || file != null)) {

        ConsoleOutputFormatter.printFile(filesCount, processedCount, file, status);
        startTime = System.currentTimeMillis();

      } else if (checkTimeRestriction()) {
        ConsoleOutputFormatter.printFile(filesCount, processedCount, null, status);
        startTime = System.currentTimeMillis();
      }
    } else if (Config.inst().showProgress()) {
      ConsoleOutputFormatter.printProcess(filesCount, processedCount);
    }
  }

  @Override
  protected void cancelingIndexing() {
    // TODO Auto-generated method stub
  }

  @Override
  protected void printResult(SearchResult results, String metaDataSearch, String contentSearch,
      Integer idToShow, int maxMetaDataLength, int maxSnippetLength) {
    ConsoleOutputFormatter.printResult(results, metaDataSearch, contentSearch, idToShow,
        maxMetaDataLength, maxSnippetLength);
  }

  @Override
  protected void searchProgressIndicator(double value, String text) {

  }

  public static final String CONSOLE_PATTERN = //
  "[%-5p]--%d{yyyy-MM-dd HH:mm:ss}--%c{1}%n[Thread:%t][%l]%n%m%n%n";

  @Override
  protected void initLoggerAppender(Logger logger) {
    String appenderName = "console appender";
    if (logger.getAppender(appenderName) == null) {
      ConsoleAppender console = new ConsoleAppender();
      console.setLayout(new PatternLayout(CONSOLE_PATTERN));
      console.setThreshold(Level.ERROR);
      console.setTarget(ConsoleAppender.SYSTEM_ERR);
      console.setTarget(ConsoleAppender.SYSTEM_OUT);
      console.activateOptions();
      console.setName(appenderName);
      logger.addAppender(console);
    }
  }

  @Override
  protected void failedToProcessFile(String error, String filePath) {
    // TODO Auto-generated method stub
  }

  @Override
  protected void removingMissingFiles(Integer filesDeleted, Integer filesToLookAt) {
    if (filesDeleted == null && filesToLookAt != null) {
      ConsoleOutputFormatter.printSeparator();
      ConsoleOutputFormatter.printLine("LOOKING FOR MISSING FILES IN DATABASE");
      ConsoleOutputFormatter.printLine("[" + filesToLookAt
          + "] files to look at. This could take a while.");
    }
    if (filesDeleted != null) {
      ConsoleOutputFormatter.printLine("[" + filesDeleted + "] files removed from your database.");
      ConsoleOutputFormatter.printSeparator();
    }
  }

  @Override
  protected void databaseConnectError(Exception e, String dataPath) {
    System.out.println("ERROR: failed to connect to database \"" + dataPath
        + "\"\nSee error-message in the log file.\n\n");
    // e.printStackTrace();
    if (EventManager.instance().getEventHandlers().size() == 1) {
      System.exit(1);
    }
  }

  @Override
  protected void ocrEngineDeployError(Exception e) {
    // TODO Auto-generated method stub

  }
}
