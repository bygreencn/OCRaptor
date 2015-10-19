package mj.ocraptor.file_handler;

import static mj.ocraptor.MainController.Status.INDEXING_FINISHED;
import static mj.ocraptor.MainController.Status.PAUSED;
import static mj.ocraptor.MainController.Status.STOPPED;
import static mj.ocraptor.database.DBFileStatus.MODIFIED;
import static mj.ocraptor.database.DBFileStatus.NOT_FOUND;
import static mj.ocraptor.database.DBFileStatus.NOT_SUPPORTED;
import static mj.ocraptor.database.DBFileStatus.UP_TO_DATE;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import mj.ocraptor.MainController;
import mj.ocraptor.MainController.Status;
import mj.ocraptor.configuration.Config;
import mj.ocraptor.console.COF;
import mj.ocraptor.database.DBFileStatus;
import mj.ocraptor.database.DBManager;
import mj.ocraptor.database.dao.FileEntry;
import mj.ocraptor.database.dao.FileEntryDao;
import mj.ocraptor.database.dao.ResultError;
import mj.ocraptor.events.Event;
import mj.ocraptor.events.EventManager;
import mj.ocraptor.file_handler.filter.FileType;
import mj.ocraptor.file_handler.utils.FileTools;
import mj.ocraptor.rmi_server.RMIServerImpl;
import mj.ocraptor.tools.St;

import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextExtractorThread implements Runnable {
  private DBManager indexDB;
  private File currentFile;
  private static Long fileCount;
  private static Long processedCount;
  private Config cfg;

  private DBFileStatus fileStatus;
  private FileEntry currentDBFileEntry;
  private String currentFileMD5Hash;
  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private FileType fileType;
  private FileEntryDao fileEntryDao;
  private Connection connection;
  private TextExtractorTools extractorTools;

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
    Thread.currentThread().setName(Config.APP_NAME + "TextExtractor");

    long threadId = Thread.currentThread().getId();
    MainController controller = MainController.inst();
    try {
      // ------------------------------------------------ //
      final Status status = controller.getStatus();
      if (status == STOPPED || status == INDEXING_FINISHED) {
        return;
      }

      // pause thread on users demand
      try {
        while (!Thread.currentThread().isInterrupted() && status == PAUSED) {
          Thread.sleep(200);
        }
      } catch (InterruptedException e) {
      }

      if (!controller.getCurrentFileWorkers().contains(currentFile)) {
        controller.getCurrentFileWorkers().put(threadId, currentFile);
      }

      this.init();
      this.fileType = FileType.get(currentFile);

      if (controller.showInitialCPUList()) {
        Thread.sleep(10);
        // TODO:
        showProgress(null, null);
        controller.setShowInitialCPUList(false);
      }

      // ------------------------------------------------ //
      // -- finally extract text from the file
      // ------------------------------------------------ //

      final FileEntry resultFromTika = extractTextTika(currentFile);

      // ------------------------------------------------ //

      if (resultFromTika != null && status != STOPPED && status != INDEXING_FINISHED
          && resultFromTika.getFullText() != null) {

        final String extractedText = resultFromTika.getFullText().getText();
        boolean fileTypeNotSupported = false;
        boolean killedDuringProcessing = false;

        if (extractedText.length() < 50) {
          fileTypeNotSupported = ResultError.NOT_SUPPORTED == ResultError.getByCode(extractedText);
          killedDuringProcessing = ResultError.KILLED == ResultError.getByCode(extractedText);
        }

        if (fileTypeNotSupported || killedDuringProcessing) {
          showProgress(null, null); // update progress percentage
        } else {
          showProgress(currentFile, fileStatus);
          // aa
          if (this.fileStatus == NOT_FOUND || this.fileStatus == MODIFIED) {
            // TODO: find a better way, starting and closing too many threads
            new Thread(new CpuListUpdateWorker()).start();
          }
          this.saveToDatabase(resultFromTika);
        }
      }

      // this.indexDB.getH2DB().printTables();
    } catch (RuntimeException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (controller.getCurrentFileWorkers().containsKey(threadId)) {
        controller.getCurrentFileWorkers().remove(threadId);
      }
      if (this.connection != null) {
        try {
          this.connection.close();
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private class CpuListUpdateWorker implements Runnable {
    @Override
    public void run() {
      Thread.currentThread().setName(Config.APP_NAME + "JavaFX: Cpu list updating");
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
      }
      showProgress(null, null);
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
    fileStatus = getFileStatus(file);
    if (fileStatus == NOT_FOUND || fileStatus == MODIFIED) {
      return true;
    }
    return false;
  }

  /**
   *
   *
   * @param file
   * @param valid
   */
  public static synchronized void showProgress(File file, DBFileStatus status) {
    if (file != null && status == null) {
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
    this.currentFileMD5Hash = FileTools.calculateMD5FromFile(file);
    this.currentDBFileEntry = indexDB.findMD5Hash(file.getPath());

    if (this.currentDBFileEntry == null) {
      return NOT_FOUND;
    }

    String md5FromDB = this.currentDBFileEntry.getHash();

    if (md5FromDB == null || md5FromDB.trim().isEmpty()) {
      return NOT_FOUND;
    } else if (!md5FromDB.equals(currentFileMD5Hash)) {
      return MODIFIED;
    }
    return UP_TO_DATE;
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
    if (processedCount == null) {
      processedCount = 0L;
    }
    this.extractorTools = new TextExtractorTools();
    this.fileEntryDao = new FileEntryDao();
  }

  /**
   *
   *
   * @param file
   * @return
   */
  public FileEntry extractTextTika(final File file) {
    try {
      processedCount++;
      this.fileStatus = NOT_SUPPORTED;

      if (this.fileType == null) {
        this.fileType = FileType.get(file);
      }

      // do not index the given database-folder
      final String databaseDirPath = new File(indexDB.getDatabaseDir()).getAbsolutePath();
      if (indexDB != null && file.getAbsolutePath().startsWith(databaseDirPath)) {
        return null;
      }

      // check if file was found in the current database
      boolean unknownFile = hasUnknownFileHash(file);
      if (!unknownFile || !extractorTools.hasAvailableParsers(file)) {
        FileEntry onlyUpdateProgress = new FileEntry(file);

        if (unknownFile) {
          onlyUpdateProgress.setError(ResultError.NOT_SUPPORTED);
        } else {
          onlyUpdateProgress.setError(ResultError.NOT_SUPPORTED);
          showProgress(file, fileStatus);
        }

        return onlyUpdateProgress; // known file hash
      }

      final RMIServerImpl extractionServer = MainController.inst().getServer();
      final Event<FileEntry> first = extractionServer.requestTextExtraction(file);

      FileEntry exResult = null;
      if (first != null) {
        exResult = first.get();
        // TODO: delete later
        // exResult.setFullText(ResultError.TIMEOUT.getErrorCode());

        if (Config.DEBUG) {
          COF.printLine("Received result for: " + exResult.getFile().getName());
          COF.printText(St.trimToLengthIndicatorRight(St.stripHtmlTags(exResult
              .getFullTextString()), 500));
        }
      }

      return exResult;
    } catch (InterruptedException e) {
      return null;
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }

    // ------------------------------------------------ //

    return null;
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
        key = St.normalizeDocumentText(key);
        key = St.stripHtmlTags(key);
        String value = metadata.get(key);
        if (value != null && !value.trim().isEmpty()) {
          value = St.normalizeDocumentText(value);
          value = St.stripHtmlTags(value);
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
  private void saveToDatabase(final FileEntry result) {
    try {
      hasUnknownFileHash(currentFile); // update filestatus property

      if (fileStatus == null) {
        // TODO:
        throw new IllegalArgumentException();
      }

      if (result == null) {
        return;
      }

      final String text = result.getFullTextString();
      if (text == null || text.isEmpty() || fileStatus == NOT_SUPPORTED
          || fileStatus == UP_TO_DATE) {
        return;
      }

      // TODO: process result string before db insert???
      if (fileStatus == NOT_FOUND && currentDBFileEntry == null) {
        fileEntryDao.insert(result, connection);
      }

      if (fileStatus == MODIFIED && currentDBFileEntry != null) {
        currentDBFileEntry.setFile(result.getFile());
        currentDBFileEntry.setFullText(result.getFullText());
        fileEntryDao.update(currentDBFileEntry, connection);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
