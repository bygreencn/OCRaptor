package mj.ocraptor.rmi_server;

import static mj.ocraptor.database.dao.ResultError.KILLED;
import static mj.ocraptor.database.dao.ResultError.KILLED_FORCED;

import java.io.File;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import mj.ocraptor.MainController;
import mj.ocraptor.MainController.Status;
import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigInteger;
import mj.ocraptor.console.AnsiColor;
import mj.ocraptor.console.COF;
import mj.ocraptor.database.dao.FileEntry;
import mj.ocraptor.database.dao.ResultError;
import mj.ocraptor.events.Event;
import mj.ocraptor.events.EventManager;
import mj.ocraptor.events.QueueMonitor;
import mj.ocraptor.rmi_client.RMIClient;
import mj.ocraptor.tools.SoftReferenceSer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class RMIServerImpl implements RMIServer {

  // ------------------------------------------------ //

  private ConcurrentHashMap<String, RMIClient> clients;
  private QueueMonitor<RMIClient> freeClients;
  private QueueMonitor<String> zombieClientIDs;

  private ConcurrentHashMap<File, RMIClient> assignedClients;
  private ConcurrentHashMap<File, Event<FileEntry>> assignedResultEvents;
  private ConcurrentHashMap<File, Long> startTimes;
  private ConcurrentHashMap<File, Integer> retriesCount;

  private QueueMonitor<File> filesToProcess;
  private QueueMonitor<File> backlogFiles;

  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
      .getLogger(RMIServerImpl.class);
  private boolean online = false;
  private Thread monitorThread;
  private Config cfg;
  private int timeout, numberOfProcesses, imageOCRCount;
  private MainController controller;
  private long allFulltextLength;

  // *INDENT-OFF*
  private static final int
    ONE_SECOND_IN_MS      = 1000,
    MAX_RETRIES_ON_KILL   = 0,
    QUEUE_SLOTS           = 1,
    BACKLOG_SLOTS         = 99;
  // *INDENT-ON*

  // *INDENT-OFF*
  private static final String[] acceptedMessages = new String[] {
    "log4j:warn "
  };
  // *INDENT-ON*

  // ------------------------------------------------ //

  @Override
  public void ping(RMIClient client) throws RemoteException {
  }

  @Override
  public void addClient(final RMIClient client) throws RemoteException {
    String id = client.getID();
    if (!clients.containsKey(id)) {
      if (cfg.verbose()) {
        COF.printLine(AnsiColor.MAGENTA.toString() + "Client connected with id: '" + id + "'");
      }
      clients.put(id, client);
      client.init(this.cfg);
      try {
        freeClients.put(client);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void removeClient(final RMIClient client) throws RemoteException {
    this.removeClient(getClientID(client));
  }

  @Override
  public void transmitResult(final RMIClient client, FileEntry result) throws RemoteException {
    try {
      File currentFile = null;
      for (File file : assignedClients.keySet()) {
        if (assignedClients.get(file).equals(client)) {
          currentFile = file;
        }
      }

      if (result == null) {
        result = new FileEntry(currentFile);
      }

      if (currentFile != null) {
        final Event<FileEntry> event = this.assignedResultEvents.get(currentFile);
        if (event != null && result != null) {
          event.put(result);
          final SoftReferenceSer<String> fullText = new SoftReferenceSer<String>(result
              .getFullTextString());
          if (fullText.get() != null) {
            try {
              if (fullText.get().length() < 100) {
                if (fullText.get().startsWith(ResultError.PREFIX)) {
                  return;
                }
              }
              allFulltextLength += fullText.get().length();
            } catch (Exception e) {
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // ------------------------------------------------ //

  /**
   *
   */
  public RMIServerImpl(final int numberOfProcesses) {
    this.init(numberOfProcesses);
  }

  /**
   *
   *
   */
  private void init(final int numberOfProcesses) {
    // *INDENT-OFF*
    this.cfg                  = Config.inst();
    this.controller           = MainController.inst();
    this.clients              = new ConcurrentHashMap<String, RMIClient>();
    this.assignedClients      = new ConcurrentHashMap<File, RMIClient>();
    this.assignedResultEvents = new ConcurrentHashMap<File, Event<FileEntry>>();
    this.startTimes           = new ConcurrentHashMap<File, Long>();
    this.retriesCount         = new ConcurrentHashMap<File, Integer>();
    // blocking on put
    this.filesToProcess       = new QueueMonitor<File>(QUEUE_SLOTS, true);
    // not blocking on put
    this.backlogFiles         = new QueueMonitor<File>(BACKLOG_SLOTS, false);
    this.freeClients          = new QueueMonitor<RMIClient>(numberOfProcesses, false);
    this.zombieClientIDs      = new QueueMonitor<String>(10, false);
    this.timeout              = this.cfg.getProp(ConfigInteger.PROCESSING_TIMEOUT_IN_SECONDS)
                                * ONE_SECOND_IN_MS;
    this.imageOCRCount        = 0;
    this.allFulltextLength    = 0;

    COF.printEmptySeparator();
    COF.printLineStretched(AnsiColor.BLUE_BACKGROUND.toString() + AnsiColor.WHITE.toString() + "Starting RMI Server", true);
    COF.printEmptySeparator();
    // *INDENT-ON*
  }

  /**
   *
   *
   * @return
   */
  public boolean isBusy() {
    if (filesToProcess.isEmpty()) {
      return false;
    }
    return true;
  }

  /**
   *
   *
   */
  public void connect() {
    // System.err.println("connecting");
    Integer serverPort = cfg.getProp(ConfigInteger.RMI_SERVER_PORT);
    try {
      LocateRegistry.createRegistry(serverPort);
    } catch (Exception e) {
    }

    try {
      // TODO: a vpn connection can cause deadlock here
      Remote stub = UnicastRemoteObject.exportObject(this, 0);
      Registry reg = LocateRegistry.getRegistry(Config.SERVER_HOST, serverPort);
      reg.rebind(Config.SERVER_NAME, stub);
      EventManager.instance().serverStarted();
      this.online = true;
      this.monitorThread = new Thread(new Monitor());
      this.monitorThread.start();
      this.listenToClients();
    } catch (RemoteException e) {
      this.online = false;
      EventManager.instance().serverProblem(e);
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   *
   *
   */
  public void disconnect() {
    try {
      // System.err.println("disconnect");
      if (this.monitorThread != null && !this.monitorThread.isInterrupted()) {
        this.monitorThread.interrupt();
      }
      Integer serverPort = cfg.getProp(ConfigInteger.RMI_SERVER_PORT);
      Registry registry = LocateRegistry.getRegistry(Config.SERVER_HOST, serverPort);
      registry.unbind(Config.SERVER_NAME);
      UnicastRemoteObject.unexportObject(this, true);
    } catch (UnmarshalException e) {
      LOGGER.error("Unmarshall exception.", e);
    } catch (RemoteException e) {
      LOGGER.error("Remote object exception occured when connecting to server.", e);
    } catch (NotBoundException e) {
      LOGGER.error("Not Bound Exception occured when connecting to server.", e);
    }
    this.online = false;
  }

  /**
   *
   *
   * @param clientID
   */
  public void removeClient(final String clientID) {
    if (clientID != null && this.clients.containsKey(clientID)) {
      COF.printLine(AnsiColor.MAGENTA + "Client disconnected with id: '" + clientID + "'");
      final RMIClient client = clients.get(clientID);
      this.freeClients.remove(client);
      this.clients.remove(clientID);
    }
  }

  /**
   *
   *
   * @param file
   */
  private void removeFileAssignments(final File file) {
    this.assignedClients.remove(file);
    this.assignedResultEvents.remove(file);
    this.startTimes.remove(file);
    this.filesToProcess.remove(file);
  }

  /**
   *
   *
   *
   * @throws RemoteException
   */
  public void sendShutdownMessage() throws RemoteException {
    for (final String clientID : clients.keySet()) {
      RMIClient clientToShutdown = clients.get(clientID);
      if (clientToShutdown != null) {
        sendShutdownMessage(clientToShutdown);
      }
    }
  }

  /**
   *
   *
   * @param clientToShutdown
   *
   * @throws RemoteException
   */
  public void sendShutdownMessage(final RMIClient clientToShutdown) throws RemoteException {
    if (clientToShutdown != null) {
      removeClient(clientToShutdown);
      try {
        clientToShutdown.shutdown();
      } catch (Exception e) {
      }
    }
  }

  /**
   *
   *
   *
   * @throws Exception
   */
  public void resetTasks() {
    clients.clear();
    freeClients.reset();
    zombieClientIDs.reset();
    backlogFiles.reset();

    assignedClients.clear();
    startTimes.clear();
    retriesCount.clear();
  }

  /**
   *
   *
   * @param client
   */
  public String getClientID(final RMIClient client) {
    if (client != null) {
      for (String clientID : clients.keySet()) {
        if (clients.get(clientID).equals(client)) {
          return clientID;
        }
      }
    }
    return null;
  }

  /**
   *
   *
   * @param file
   * @return
   *
   * @throws RemoteException
   */
  public Event<FileEntry> requestTextExtraction(final File file) {
    return requestTextExtraction(file, null, false);
  }

  /**
   *
   *
   * @param file
   * @return
   *
   * @throws RemoteException
   */
  public Event<FileEntry> requestTextExtraction(final File file, Event<FileEntry> event,
      boolean backlog) {
    try {
      if (event == null) {
        event = new Event<FileEntry>();
      }
      assignedResultEvents.put(file, event);
      if (backlog) {
        if (!backlogFiles.contains(file)) {
          backlogFiles.put(file);
        }
      } else {
        if (!filesToProcess.contains(file)) {
          filesToProcess.put(file);
        }
      }
      return event;
    } catch (InterruptedException e) {
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * @return the filesToProcess
   */
  public QueueMonitor<File> getFilesToProcess() {
    return filesToProcess;
  }

  /**
   * @return the freeClients
   */
  public QueueMonitor<RMIClient> getFreeClients() {
    return freeClients;
  }

  /**
   * @return the zombieClientIDs
   */
  public QueueMonitor<String> getZombieClientIDs() {
    return zombieClientIDs;
  }

  /**
   * @return the started
   */
  public boolean isOnline() {
    return online;
  }

  /**
   * @return the timeout
   */
  public int getTimeout() {
    return timeout;
  }

  /**
   * @return the numberOfProcesses
   */
  public int getNumberOfProcesses() {
    return numberOfProcesses;
  }

  /**
   * @return the imageOCRCount
   */
  public int getImageOCRCount() {
    return imageOCRCount;
  }

  /**
   * @return the allFulltextLength
   */
  public long getAllFulltextLength() {
    return allFulltextLength;
  }

  /**
   *
   *
   * @return
   */
  public int getConnectedClientsSize() {
    return clients.size();
  }

  /**
   *
   *
   */
  private void listenToClients() {
    while (!Thread.currentThread().isInterrupted()) {
      // ------------------------------------------------ //
      File fileToProcess = null;
      RMIClient assignedClient = null;
      try {
        assignedClient = freeClients.get();

        if (assignedClient != null && getClientID(assignedClient) != null) {
          if (!backlogFiles.isEmpty()) {
            fileToProcess = backlogFiles.get();
          } else {
            fileToProcess = filesToProcess.get();
          }
          if (fileToProcess != null) {
            assignedClients.put(fileToProcess, assignedClient);
            startTimes.put(fileToProcess, System.currentTimeMillis());
            // it is possible that the client does not exist here anymore
            if (clients.contains(assignedClient)) {
              assignedClient.handleFile(fileToProcess);
            } else if (!backlogFiles.contains(fileToProcess)) {
              backlogFiles.put(fileToProcess);
            }
          }
        }
      } catch (InterruptedException e) {
        break;
      } catch (RemoteException e) {
      } catch (Exception e) {
        LOGGER.error("Clients listener failed", e);
      }
      // ------------------------------------------------ //
    }

    COF.printEmptySeparator();
    COF.printLineStretched(AnsiColor.BLUE_BACKGROUND.toString() + AnsiColor.WHITE.toString()
        + "Stopping RMI Server", true);
  }

  /**
   *
   *
   * @param client
   * @return
   */
  private File getAssignedFile(final RMIClient client) {
    for (final File file : assignedClients.keySet()) {
      if (assignedClients.get(file).equals(client)) {
        return file;
      }
    }
    return null;
  }

  /**
   *
   *
   * @param event
   * @param file
   * @param message
   */
  private void finalyzeResults(final Event<FileEntry> event, final File file, final String message,
      final ResultError error) {
    final FileEntry timeoutResult = new FileEntry(file);
    // ------------------------------------------------ //
    if (message != null) {
      timeoutResult.setFullText(message);
    }
    if (error != null) {
      timeoutResult.setError(error);
    }
    // ------------------------------------------------ //
    if (event != null && !event.fired()) {
      try {
        event.put(timeoutResult);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    removeFileAssignments(file);
    // ------------------------------------------------ //
  }

  /**
   *
   *
   */
  private void printHealthStatus() {
    // ------------------------------------------------ //
    // *INDENT-OFF*
    int numberOfProcesses = getConnectedClientsSize();
    System.out.println(
      StringUtils.repeat("-", 100)                                                           + "\n"
      + "Status:\t\t\t"             + controller.getStatus()                                 + "\n"
      + "Free clients:\t\t"         + freeClients.size()           + "/" + numberOfProcesses + "\n"
      + "Working clients:\t"        + assignedClients.size()       + "/" + numberOfProcesses + "\n"
      + "Files in queue:\t\t"       + filesToProcess.size()        + "/" + QUEUE_SLOTS       + "\n"
      + "Files in backlog queue:\t" + backlogFiles.size()          + "/" + BACKLOG_SLOTS     + "\n"
      + "Timeout tracker:\t"        + startTimes.size()            + "/" + numberOfProcesses + "\n"
      + "Added result events\t"     + assignedResultEvents.size()  + "/" + numberOfProcesses + "\n"
      + StringUtils.repeat("-", 100)                                                         + "\n"
    );
    // *INDENT-ON*
    // ------------------------------------------------ //
  }

  private boolean indexing() {
    return controller.getStatus() == Status.INDEXING;
  }

  private boolean stopped() {
    return controller.getStatus() == Status.STOPPED;
  }

  private boolean paused() {
    return controller.getStatus() == Status.PAUSED;
  }

  private boolean finished() {
    return controller.getStatus() == Status.INDEXING_FINISHED;
  }

  private final int MONITOR_TIC_INTERVAL = 500;
  private long waitingTimeForClients = System.currentTimeMillis();
  private final long clientsWaitingTimeout = 20000;

  /**
   *
   */
  private class Monitor implements Runnable {
    @Override
    public void run() {
      Thread.currentThread().setName(
          Config.APP_NAME + "JavaFX: RMIServer monitor - " + new Random().nextInt(99999));

      try {
        while (!Thread.currentThread().isInterrupted()) {
          // ------------------------------------------------ //
          if (indexing()) {
            if (clients.isEmpty()) {
              if ((System.currentTimeMillis() - waitingTimeForClients) > clientsWaitingTimeout) {
                COF.printLine(AnsiColor.RED_BACKGROUND.toString() + AnsiColor.WHITE.toString()
                    + "Can not connect to clients!");
                controller.shutdown(false);
                EventManager.instance().cantConnectToClients();
              } else {
                COF.printLine(AnsiColor.MAGENTA.toString() + "Waiting for clients...");
              }
            } else {
              waitingTimeForClients = System.currentTimeMillis();
            }
          }
          // ------------------------------------------------ //
          else if (stopped() || paused() || finished()) {
            try {
              sendShutdownMessage();
            } catch (RemoteException e) {
            }

            try {
              resetTasks();
              // reset all
              for (final File file : assignedResultEvents.keySet()) {
                final Event<FileEntry> event = assignedResultEvents.get(file);
                if (!event.fired()) {
                  if (stopped()) {
                    finalyzeResults(event, file, null, KILLED);
                    filesToProcess.reset();
                  } else {
                    requestTextExtraction(file, event, true);
                  }
                }
              }

              // printHealthStatus();
            } catch (Exception e) {
              e.printStackTrace();
            }

            Status lastStatus = controller.getStatus();
            while (paused() || stopped() || finished() && !Thread.currentThread().isInterrupted()) {
              // if the status switches from paused to stopped
              if (lastStatus != controller.getStatus()) {
                break;
              }
              Thread.sleep(100);
              waitingTimeForClients = System.currentTimeMillis();
            }

          }

          // ------------------------------------------------ //

          // printHealthStatus();

          // ------------------------------------------------ //
          Thread.sleep(MONITOR_TIC_INTERVAL);
          // ------------------------------------------------ //

          // ------------------------------------------------ //
          final LinkedHashSet<RMIClient> clientsToKill = new LinkedHashSet<RMIClient>();

          for (final File file : assignedResultEvents.keySet()) {
            final RMIClient assignedClient = assignedClients.get(file);
            if (file == null || assignedClient == null) {
              // TODO: log
              continue;
            }

            final Event<FileEntry> event = assignedResultEvents.get(file);
            if (event.fired()) {
              // ------------------------------------------------ //
              // -- result was successfully generated
              // ------------------------------------------------ //
              try {
                // wait max 1000ms for client to remove busy flag,
                // or else the client is considered to be zombified
                for (int i = 1; i <= 20; i++) {
                  if (!assignedClient.isBusy()) {
                    freeClients.put(assignedClient);
                    break;
                  } else {
                    if (i == 20) {
                      LOGGER.info("Event fired, client is still busy --> killing.");
                      clientsToKill.add(assignedClient);
                    }
                  }
                  Thread.sleep(50);
                }
              } catch (RemoteException e) {
                LOGGER.info("Event fired, client does not respond--> killing.");
                clientsToKill.add(assignedClient);
              }
              removeFileAssignments(file);
            } else {
              // ------------------------------------------------ //
              // -- [timeout]
              // ------------------------------------------------ //
              long duration = 0;
              if (file != null && startTimes != null) {
                try {
                  long startTime = startTimes.get(file);
                  duration = System.currentTimeMillis() - startTime;
                } catch (Exception e) {
                }
              }

              if (duration > timeout || startTimes == null) {
                try {
                  // TODO: kill manually if neccessary
                  assignedClient.shutdownDelayed(60000);
                } catch (RemoteException e) {
                }
                // finalyzeResults(event, file, TIMEOUT.getErrorCode());
                startTimes.put(file, System.currentTimeMillis());
                LOGGER.error("Parsing timeout: " + file.getAbsolutePath());
              }
            }
          }

          for (final RMIClient clientToKill : clientsToKill) {
            try {
              sendShutdownMessage(clientToKill);
            } catch (RemoteException e) {
              e.printStackTrace();
            }
          }

          for (final String clientID : clients.keySet()) {
            try {
              clients.get(clientID).ping();
            } catch (Exception e) {
              final RMIClient client = clients.get(clientID);
              LOGGER.error("Can't connect to client, removing [1]...");
              try {
                sendShutdownMessage(clients.get(clientID));
              } catch (RemoteException e1) {
              }
              final File assignedFile = getAssignedFile(client);

              if (assignedFile != null) {
                final Event<FileEntry> event = assignedResultEvents.get(assignedFile);
                if (event != null && !event.fired()) {
                  if (!retriesCount.containsKey(assignedFile)) {
                    retriesCount.put(assignedFile, 0);
                  }
                  final int retries = retriesCount.get(assignedFile);
                  if (retries < MAX_RETRIES_ON_KILL) {
                    removeFileAssignments(assignedFile);
                    requestTextExtraction(assignedFile, event, true);
                    retriesCount.put(assignedFile, retries + 1);
                  } else {
                    finalyzeResults(event, assignedFile, null, KILLED_FORCED);
                  }
                }
              }
            }
          }
        }
      } catch (InterruptedException e) {
      }
    }
  }

  @Override
  public void sendDebugInfo(final RMIClient client, final String msg, final Throwable e,
      final boolean consoleOnly) throws RemoteException {
    if (consoleOnly) {
      System.out.println(msg);
      if (e != null) {
        System.out.println(ExceptionUtils.getStackTrace(e));
      }
    } else {
      LOGGER.info("Client (id: " + client.getID() + ")\n" + msg, e);
    }
  }

  @Override
  public void sendDebugError(final RMIClient client, final String msg, final Throwable e,
      final boolean consoleOnly) throws RemoteException {
    if (consoleOnly) {
      System.err.println(msg);
      if (e != null) {
        System.err.println(ExceptionUtils.getStackTrace(e));
      }
    } else {
      boolean logMessage = true;
      for (final String acceptedMessage : acceptedMessages) {
        if (msg.toLowerCase().contains(acceptedMessage)) {
          logMessage = false;
        }
      }

      if (logMessage) {
        LOGGER.error("Client (id: " + client.getID() + ")\n" + msg, e);
      }
    }
  }

  @Override
  public void incrementImageCount(final RMIClient client) throws RemoteException {
    this.imageOCRCount++;
  }

}
