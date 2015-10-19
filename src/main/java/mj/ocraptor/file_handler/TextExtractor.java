package mj.ocraptor.file_handler;

import java.io.File;
import java.util.concurrent.ThreadFactory;

import mj.ocraptor.MainController;
import mj.ocraptor.MainController.Status;
import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigBool;
import mj.ocraptor.database.DBManager;
import mj.ocraptor.events.EventManager;
import mj.ocraptor.file_handler.events.FileHandler;

public class TextExtractor extends FileHandler {

  private DBManager indexDB;
  private PausableExecutor executor;
  private boolean countFiles;
  private long fileCount;
  private File currentDir;

  /**
   *
   */
  public TextExtractor(DBManager db, PausableExecutor executor) {
    this.indexDB = db;
    this.executor = executor;
    this.currentDir = new File(Config.inst().getDirectoryToIndex())
    .getAbsoluteFile();
  }

  /**
   * @param countFiles
   *          the countFiles to set
   */
  public void setCountFiles(boolean countFiles) {
    this.countFiles = countFiles;
  }

  /**
   * @return the fileCount
   */
  public long getFileCount() {
    return fileCount;
  }

  /**
   *
   *
   */
  public void countFilesOnly() {
    this.countFiles = true;
  }

  /**
   *
   *
   */
  public void stopCounting() {
    this.countFiles = false;
  }

  /**
   * @param dir
   */
  @Override
  public void handleDir(File dir) {
    //
  }

  /**
   *
   */
  public class WorkerThreadFactory implements ThreadFactory {
    private int counter = 0;
    private String prefix = "";

    public WorkerThreadFactory(String prefix) {
      this.prefix = prefix;
    }

    public Thread newThread(Runnable r) {
      return new Thread(r, prefix + "-" + counter++);
    }
  }


  /**
   * @param file
   */
  @Override
  public void handleFile(File file) {
    final MainController controller = MainController.inst();
    while (!Thread.currentThread().isInterrupted() && controller.getStatus() == Status.PAUSED) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    if (controller.getStatus() == Status.STOPPED || controller.getStatus() == Status.INDEXING_FINISHED)
      return;

    if (file != null && file.exists()) {
      boolean isHidden = !Config.inst().getProp(
                           ConfigBool.INDEX_HIDDEN_FILES_AND_FOLDERS);

      if (isHidden) {
        isHidden = file.isHidden();
        if (!isHidden) {
          File parent = file.getParentFile();
          while (parent != null && !isHidden) {
            if (parent.isHidden()) {
              isHidden = true;
            }
            if (parent.getAbsoluteFile().equals(this.currentDir)) {
              parent = null;
            } else {
              parent = parent.getParentFile();
            }
          }
        }
      }

      if (!isHidden && !file.getParent().startsWith(indexDB.getDatabaseDir())) {
        if (countFiles) {
          EventManager.instance().countingFiles(++this.fileCount, false);
        } else {
          try {
            Runnable worker = new TextExtractorThread(indexDB, file);
            this.executor.execute(worker);
          } catch (RuntimeException e) {
            // TODO: logging
            // e.printStackTrace();
          }
        }
      }
    }

  }
}
