package mj.ocraptor.events;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import mj.ocraptor.database.DBFileStatus;
import mj.ocraptor.database.search.LuceneResult;
import mj.ocraptor.javafx.GUIController;

public class EventManager extends EventAbstr {
  private static EventManager instance;
  private List<EventAbstr> eventHandlers;
  private Logger parentLogger;

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   */
  private EventManager() {
    this.eventHandlers = new ArrayList<EventAbstr>();
    this.eventHandlers.add(new EventConsole());
  }

  /**
   *
   *
   */
  public void addGUIHandler() {
    if (instance == null) {
      throw new NullPointerException();
    }
    if (GUIController.instance() != null) {
      this.eventHandlers.add(new EventGUI());
    }
  }

  /**
   *
   *
   * @return
   */
  public static EventManager instance() {
    if (instance == null) {
      instance = new EventManager();
    }
    return instance;
  }

  /**
   * @return the eventHandlers
   */
  public List<EventAbstr> getEventHandlers() {
    return eventHandlers;
  }

  /**
   *
   *
   */
  public void initLoggerAppender() {
    if (this.parentLogger == null) {
      throw new NullPointerException();
    }

    for (EventAbstr handler : this.eventHandlers)
      handler.initLoggerAppender(this.parentLogger);
  }

  /**
   *
   *
   */
  public void printFolderFinished() {
    for (EventAbstr handler : this.eventHandlers)
      handler.printProcess(null, 0L, 0L, true, null);
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  @Override
  public void configFileNotFound(File invalidFile) {
    for (EventAbstr handler : this.eventHandlers)
      handler.configFileNotFound(invalidFile);
  }

  @Override
  public void startCountingFiles() {
    for (EventAbstr handler : this.eventHandlers)
      handler.startCountingFiles();
  }

  @Override
  public void countingFiles(Long currentCount, boolean finalCount) {
    for (EventAbstr handler : this.eventHandlers) {
      handler.countingFiles(currentCount, finalCount);
    }
  }

  @Override
  public void configFileNameInvalid() {
    for (EventAbstr handler : this.eventHandlers)
      handler.configFileNameInvalid();
  }

  @Override
  public void propertiesFileAlreadyExists() {
    for (EventAbstr handler : this.eventHandlers)
      handler.propertiesFileAlreadyExists();
  }

  @Override
  public void printProcess(File file, Long filesCount, Long processedCount, boolean finalCount,
      DBFileStatus status) {
    for (EventAbstr handler : this.eventHandlers)
      handler.printProcess(file, filesCount, processedCount, finalCount, status);
  }

  @Override
  public void cancelingIndexing() {
    for (EventAbstr handler : this.eventHandlers)
      handler.cancelingIndexing();
  }

  @Override
  public void printResult(LuceneResult results, String contentSearch, Integer idToShow,
      int maxSnippetLength) {
    for (EventAbstr handler : this.eventHandlers)
      handler.printResult(results, contentSearch, idToShow, maxSnippetLength);
  }

  /**
   *
   *
   * @param value
   */
  public void searchProgressIndicator(double value) {
    this.searchProgressIndicator(value, null);
  }

  @Override
  public void searchProgressIndicator(double value, String text) {
    for (EventAbstr handler : this.eventHandlers)
      handler.searchProgressIndicator(value, text);
  }

  @Override
  public void initLoggerAppender(Logger logger) {
    this.parentLogger = logger;
    this.initLoggerAppender();
  }

  @Override
  public void failedToProcessFile(String error, String filePath) {
    for (EventAbstr handler : this.eventHandlers)
      handler.failedToProcessFile(error, filePath);
  }

  @Override
  public void removingMissingFiles(Integer filesDeleted, Integer filesToLookAt) {
    for (EventAbstr handler : this.eventHandlers)
      handler.removingMissingFiles(filesDeleted, filesToLookAt);
  }

  @Override
  public void databaseConnectError(Exception e, String dataPath) {
    for (EventAbstr handler : this.eventHandlers)
      handler.databaseConnectError(e, dataPath);
  }

  @Override
  public void ocrEngineDeployError(Exception e) {
    for (EventAbstr handler : this.eventHandlers)
      handler.ocrEngineDeployError(e);
  }

  @Override
  public void serverStarted() {
    for (EventAbstr handler : this.eventHandlers)
      handler.serverStarted();
  }

  @Override
  public void serverProblem(final Exception e) {
    for (EventAbstr handler : this.eventHandlers)
      handler.serverProblem(e);
  }

  @Override
  public void cantConnectToClients() {
    for (EventAbstr handler : this.eventHandlers)
      handler.cantConnectToClients();
  }
}
