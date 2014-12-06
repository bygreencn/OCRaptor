package mj.events;

import java.io.File;

import mj.database.DBFileStatus;
import mj.database.SearchResult;

public abstract class EventAbstr {

  protected static final String FILE_NOT_FOUND_ERROR = "Configuration file not found:";

  protected abstract void configFileNotFound(final File invalidFile);

  protected abstract void countingFiles(Long currentCount, boolean finalCount);

  protected abstract void configFileNameInvalid();

  protected abstract void cancelingIndexing();

  protected abstract void propertiesFileAlreadyExists();

  protected abstract void searchProgressIndicator(final double value, final String text);

  protected abstract void initLoggerAppender(final org.apache.log4j.Logger logger);

  protected abstract void failedToProcessFile(final String error, final String filePath);

  protected abstract void removingMissingFiles(final Integer filesDeleted, final Integer filesToLookAt);

  protected abstract void databaseConnectError(Exception e, String dataPath);

  protected abstract void ocrEngineDeployError(Exception e);

  protected abstract void printResult(final SearchResult results,
      final String metaDataSearch, final String contentSearch,
      final Integer idToShow, int maxMetaDataLength, int maxSnippetLength);

  protected abstract void printProcess(File file, Long filesCount,
      Long processedCount, boolean finalCount, DBFileStatus status);

}
