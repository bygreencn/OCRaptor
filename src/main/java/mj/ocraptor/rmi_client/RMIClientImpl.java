package mj.ocraptor.rmi_client;

import static mj.ocraptor.database.dao.ResultError.NOT_SUPPORTED;
import static mj.ocraptor.database.dao.ResultError.PARSING;
import static mj.ocraptor.database.dao.ResultError.TIMEOUT;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.database.dao.FileEntry;
import mj.ocraptor.file_handler.TextExtractorSub;
import mj.ocraptor.rmi_server.RMIServer;
import mj.ocraptor.tools.SystemTools;

public class RMIClientImpl implements RMIClient {

  // ------------------------------------------------ //

  /**
   * Let's test some shit!
   *
   * @throws RemoteException
   */
  public static void main(String[] args) {
    if (args == null || args.length < 2) {
      throw new NullPointerException("Two arguments expected [0]=clientId, [1]=port");
    }

    try {
      final String clientID = args[0];
      final Integer port = Integer.valueOf(args[1]);

      RMIClientImpl client = RMIClientImpl.init(clientID, port);
      client.connect();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  // ------------------------------------------------ //

  // *INDENT-OFF*
  private String    id;
  private RMIServer server = null;
  private boolean   busy;
  private Integer   port;
  private File      currentFile;
  final Long        clientXmx;
  // *INDENT-ON*

  private static RMIClientImpl rmiClient;

  // *INDENT-OFF*
  private int
    countConnectionErrors             = 0,
    amountOfExtractionsBeforeRestart  = 1,
    extractionsCounter                = 0;
  // *INDENT-ON*

  // ------------------------------------------------ //

  /**
   *
   *
   * @return
   */
  public static RMIClientImpl instance() {
    if (rmiClient == null) {
      throw new NullPointerException("RMI Client instance is null");
    }
    return rmiClient;
  }

  /**
   *
   *
   * @param id
   * @param port
   * @return
   *
   * @throws RemoteException
   */
  public static RMIClientImpl init(final String id, final Integer port) throws RemoteException {
    rmiClient = new RMIClientImpl(id, port);
    return rmiClient;
  }

  /**
   * @param id
   * @param port
   * @throws RemoteException
   *
   */
  private RMIClientImpl(final String id, final Integer port) throws RemoteException {
    this.id = id;
    this.port = port;
    UnicastRemoteObject.exportObject(this, 0);
    clientXmx = SystemTools.getXmxParameterInMB();

    if (clientXmx != null && clientXmx < 512) {
      amountOfExtractionsBeforeRestart = 1;
    } else {
      amountOfExtractionsBeforeRestart = 10;
    }
  }

  /**
   *
   *
   * @return
   * @throws RemoteException
   */
  public void connect() throws RemoteException {
    // ------------------------------------------------ //
    try {
      Thread.currentThread().setName(Config.APP_NAME + "RMIClient start thread");
      Registry reg = LocateRegistry.getRegistry(Config.SERVER_HOST, port);
      this.server = (RMIServer) reg.lookup(Config.SERVER_NAME);
      this.server.addClient(this);
      this.monitor();

      // throw new RemoteException();
    } catch (Exception e) {
      e.printStackTrace();
      this.shutdown();
    }
    // ------------------------------------------------ //
  }

  /**
   *
   *
   */
  @Override
  public void shutdown() throws RemoteException {
    try {
      if (this.server != null)
        if (Config.inst().isClientDelayedShutdown()) {
          final FileEntry timeoutResult = new FileEntry(currentFile);
          timeoutResult.setError(TIMEOUT);
          this.server.transmitResult(this, timeoutResult);
        }
      this.server.removeClient(this);
    } catch (Exception e) {
    } finally {
      System.exit(0);
    }
  }

  @Override
  public String getID() throws RemoteException {
    return this.id;
  }

  @Override
  public void handleFile(final File file) throws RemoteException {
    if (file != null) {
      this.currentFile = file;
      new Thread(new FileProcessor(this, file)).start();
    } else {
      this.server.sendDebugError(this, "Given filepath is null!", null, false);
    }
  }

  @Override
  public void init(Config config) throws RemoteException {
    if (config != null) {
      try {
        Config.init(config);
      } catch (Exception e) {
        this.server.sendDebugError(this, "Config init error", e, false);
        e.printStackTrace();
      }
    }
  }

  /**
   *
   */
  private class FileProcessor implements Runnable {
    private File file;
    private RMIClient client;

    /**
     *
     */
    public FileProcessor(RMIClient client, File file) {
      this.file = file;
      this.client = client;
    }

    @Override
    public void run() {
      Thread.currentThread().setName(Config.APP_NAME + "RMIClientImpl: Textextraction");
      try {
        busy = true;
        FileEntry result = null;

        // ------------------------------------------------ //
        if (file.exists() && file.canRead() && file.isFile()) {
          try {
            final TextExtractorSub extractor = new TextExtractorSub();
            result = extractor.extractTextTika(file);
            // if the filetype was not supported,
            // do not count it as a text extraction
            if (result != null && !result.getFullTextString().equals(NOT_SUPPORTED.getErrorCode())) {
              extractionsCounter++;
            }
          } catch (Exception e) {
            result = new FileEntry(file);
            result.setError(PARSING);
            server.sendDebugError(client, "Textextraction failed: " + file, e, false);
            e.printStackTrace();
          }
        } else {
          server.sendDebugError(client, "Given file is not valid: " + file, null, false);
        }
        // ------------------------------------------------ //

        server.transmitResult(this.client, result);
      } catch (RemoteException e) {
        e.printStackTrace();
        try {
          shutdown();
        } catch (RemoteException e1) {
        }
      } finally {
        boolean memoryRunsOut = false;

        // TODO: more testing
        if (clientXmx != null && clientXmx > 0) {
          final long freeMemory = SystemTools.getRuntimeFreeMemoryInMB();
          final double totalMemory = (double) SystemTools.getRuntimeTotalMemoryInMB();
          memoryRunsOut = freeMemory < 100 && ((totalMemory / (double) clientXmx) > 0.6);

          // try {
          // server.sendDebugError(client, "Memory footprint: " + freeMemory +
          // "mb, " + totalMemory
          // + "mb, " + memoryRunsOut, null);
          // } catch (RemoteException e) {
          // }
        }

        if (extractionsCounter >= amountOfExtractionsBeforeRestart || memoryRunsOut) {
          try {
            shutdown();
          } catch (RemoteException e1) {
          }
        } else {
          busy = false;
        }
      }
    }
  }

  /**
   * @throws RemoteException
   *
   *
   */
  private void monitor() throws RemoteException {
    while (countConnectionErrors < 4) {
      try {
        if (this.server == null) {
          System.exit(0);
        }
        Thread.sleep(500);
        if (this.server != null) {
          this.server.ping(this);
        }
      } catch (RemoteException e) {
        this.countConnectionErrors++;
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    shutdown();
  }

  @Override
  public void ping() throws RemoteException {
  }

  /**
   * @return the server
   */
  public RMIServer getServer() {
    return server;
  }

  @Override
  public boolean isBusy() throws RemoteException {
    return this.busy;
  }

  /**
   * @param delay
   *
   * @throws RemoteException
   */
  @Override
  public void shutdownDelayed(final int delay) throws RemoteException {
    Config.inst().setClientDelayedShutdown(true);
    new Thread(new ShutdownWorker(delay)).start();
  }

  /**
   *
   */
  private class ShutdownWorker implements Runnable {
    public ShutdownWorker(final int delay) {
      Thread.currentThread().setName(Config.APP_NAME + "ShutdownWorker-Thread");
      try {
        Thread.sleep(delay);
        shutdown();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    @Override
    public void run() {
      // TODO Auto-generated method stub
    }
  }

  /**
   *
   *
   * @param msg
   * @param e
   * @param consoleOnly
   */
  public void sendDebugInfoToServer(final String msg, final Throwable e, final boolean consoleOnly) {
    try {
      this.server.sendDebugInfo(this, msg, e, true);
    } catch (RemoteException e1) {
    }
  }

  /**
   *
   *
   * @param msg
   * @param e
   * @param consoleOnly
   */
  public void sendDebugErrorToServer(final String msg, final Throwable e, final boolean consoleOnly) {
    try {
      this.server.sendDebugError(this, msg, e, true);
    } catch (RemoteException e1) {
    }
  }
}
