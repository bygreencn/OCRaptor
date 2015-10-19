package mj.ocraptor.events;

import java.io.File;

import mj.ocraptor.database.DBFileStatus;
import mj.ocraptor.database.search.LuceneResult;

public abstract class EventAbstr {

  protected static final String FILE_NOT_FOUND_ERROR = "Configuration file not found:";

  protected abstract void configFileNotFound(final File invalidFile);


  protected abstract void configFileNameInvalid();

  protected abstract void cancelingIndexing();

  protected abstract void serverStarted();

  protected abstract void serverProblem(final Exception e);

  protected abstract void propertiesFileAlreadyExists();

  protected abstract void searchProgressIndicator(final double value, final String text);

  protected abstract void initLoggerAppender(final org.apache.log4j.Logger logger);

  protected abstract void failedToProcessFile(final String error, final String filePath);

  protected abstract void removingMissingFiles(final Integer filesDeleted,
      final Integer filesToLookAt);

  protected abstract void databaseConnectError(Exception e, String dataPath);

  protected abstract void ocrEngineDeployError(Exception e);

  protected abstract void printResult(final LuceneResult results, final String contentSearch,
      final Integer idToShow, int maxSnippetLength);

  protected abstract void printProcess(File file, Long filesCount, Long processedCount,
      boolean finalCount, DBFileStatus status);

  protected abstract void cantConnectToClients();

  // ------------------------------------------------ //

  protected abstract void startCountingFiles();
  protected abstract void countingFiles(Long currentCount, boolean finalCount);
}
