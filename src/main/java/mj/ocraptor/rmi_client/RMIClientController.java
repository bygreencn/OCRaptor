package mj.ocraptor.rmi_client;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

import mj.ocraptor.MainController;
import mj.ocraptor.MainController.Status;
import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigInteger;
import mj.ocraptor.console.AnsiColor;
import mj.ocraptor.console.COF;
import mj.ocraptor.console.Platform;
import mj.ocraptor.console.Platform.Os;
import mj.ocraptor.file_handler.PausableExecutor;
import mj.ocraptor.file_handler.executer.CommandExecutor;
import mj.ocraptor.file_handler.executer.handler_impl.SimpleOutput;
import mj.ocraptor.rmi_server.RMIServerImpl;

public class RMIClientController {
  private PausableExecutor executor;
  private ConcurrentHashMap<String, SubProcess> processes;
  private Config cfg;
  private String javaPath = "java";

  // *INDENT-OFF*
  private static final String
    PARSER_MAIN_CLASS    = "mj.ocraptor.rmi_client.RMIClientImpl",
    JAVA_BASE_PATH       = Config.getBinsFolder() + File.separator +
                           "portable-java" + File.separator,
    JAVA_LINUX_SUBPATH   = "lin-x86-64" + File.separator + "bin" + File.separator,
    JAVA_OSX_SUBPATH     = "osx-x86-64" + File.separator + "bin" + File.separator,
    JAVA_WIN_SUBPATH     = "win-x86-64" + File.separator + "bin" + File.separator,
    // JAVA_WIN_SUBPATH     = "win-x86-64\\bin\\",
    JAVA_BINARY_NAME     = "ocraptor-client",
    JAVA_BINARY_NAME_WIN = "OCRaptor-Client.exe";
  // *INDENT-ON*

  private static final int ONE_SECOND_IN_MS = 1000;
  private static final int CHECK_TIMEOUT_INTERVAL = 10;

  private final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
      .getLogger(RMIClientController.class);

  private MainController controller;

  /**
   *
   *
   */
  public RMIClientController init() {
    this.processes = new ConcurrentHashMap<String, SubProcess>();
    this.cfg = Config.inst();
    this.controller = MainController.inst();

    // *INDENT-OFF*
    if (this.cfg.useBuildInJRE()) {
      Os os = Platform.getSystem();
      if (os == Os.LINUX) {
        javaPath = JAVA_BASE_PATH + JAVA_LINUX_SUBPATH + JAVA_BINARY_NAME;
      } else if (os == Os.OSX) {
        javaPath = JAVA_BASE_PATH + JAVA_OSX_SUBPATH   + JAVA_BINARY_NAME;
      } else if (os == Os.WINDOWS) {
        javaPath = JAVA_BASE_PATH + JAVA_WIN_SUBPATH   + JAVA_BINARY_NAME_WIN;
      }
    }
    // *INDENT-ON*
    return this;
  }

  /**
   * @param numberOfThreads
   *
   */
  public RMIClientController(int numberOfThreads) {
    final ThreadFactory factory = new ThreadFactory() {
      @Override
      public Thread newThread(Runnable target) {
        final Thread thread = new Thread(target);
        LOGGER.debug("Creating new worker thread");
        thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
          @Override
          public void uncaughtException(Thread t, Throwable e) {
            LOGGER.error("Uncaught Exception", e);
          }
        });
        return thread;
      }
    };
    this.executor = new PausableExecutor(numberOfThreads, factory);
  }

  /**
   *
   *
   */
  public void addSubProcess(final String id) {
    if (id == null) {
      throw new NullPointerException();
    }
    if (!this.executor.isShutdown() && !this.executor.isTerminating()
        && !this.executor.isTerminated()) {
      final SubProcess subProcess = new SubProcess(id);
      this.processes.put(id, subProcess);
      this.executor.execute(subProcess);
    }
  }

  /**
   *
   *
   */
  public void shutdown() {
    this.executor.shutdown();
    for (int i = 0; i < 20; i++) {
      if (executor.isTerminating()) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          break;
        }
      }
    }
    this.executor.shutdownNow();
  }

  /**
   *
   *
   * @param id
   */
  public void killSubProcess(final String id) {

  }

  /**
   * @return the processes
   */
  public Map<String, SubProcess> getProcesses() {
    return processes;
  }

  /**
   *
   */
  private class SubProcess implements Runnable {
    private String id;

    /**
     *
     */
    public SubProcess(final String id) {
      this.id = id;
    }

    @Override
    public void run() {
      Thread.currentThread().setName(Config.APP_NAME + "Indexing RMI client manager subprocess");
      this.init();
    }

    /**
     *
     *
     */
    private void init() {
      RMIServerImpl server = controller.getServer();

      // ------------------------------------------------ //
      // waiting for the server to wake up
      while (!Thread.currentThread().isInterrupted() && (server == null || !server.isOnline())
          && controller.getStatus() != Status.STOPPED && controller.getStatus() != Status.KILLED) {
        try {
          server = controller.getServer();
          COF.printLine(AnsiColor.MAGENTA.toString() + AnsiColor.BOLD.toString()
              + "Waiting for Server to start");
          Thread.sleep(1000);

          if (controller.getStatus() == Status.KILLED) {
            return;
          }
        } catch (InterruptedException e) {
          break;
        }
      }
      // ------------------------------------------------ //
      // getting heap informations specified by user
      final String xms = String.valueOf(cfg.getProp(ConfigInteger.PROCESS_XMS));
      final String xmx = String.valueOf(cfg.getProp(ConfigInteger.PROCESS_XMX));
      // the port is static at the moment
      // TODO: implement an option to switch ports automatically if already used
      final String serverPort = String.valueOf(cfg.getProp(ConfigInteger.RMI_SERVER_PORT));
      // ------------------------------------------------ //
      // *INDENT-OFF*
      String command =
        javaPath + " -Dfile.encoding=UTF-8"
         + " -Xms" + xms + "m"
         + " -Xmx" + xmx + "m"
         + " -cp \""
         + Config.getParserBinPath()
         + (Platform.getSystem() == Os.WINDOWS ? ";" : ":")
         + Config.getLibraryFolderPath() + "/*\" "
         + PARSER_MAIN_CLASS
         + " " + this.id // generated id
         + " " + String.valueOf(serverPort);

      String errOutput = null,
             stdOutput = null;

      // *INDENT-ON*
      // ------------------------------------------------ //

      if (Config.DEBUG) {
        COF.printText("Client start command\n[" + command + "]");
      }

      final SimpleOutput eventHandler = new SimpleOutput();
      final CommandExecutor bashExecuter = new CommandExecutor(Platform.getSystem(), eventHandler);
      bashExecuter.setCommand(command);
      final Thread executorThread = new Thread(bashExecuter);

      if (command != null && !command.trim().isEmpty()) {
        executorThread.start();
        final long startTime = System.currentTimeMillis();
        boolean pause = false;

        // *INDENT-OFF*
        final String[] killMessages = new String[] {
          "java.lang.outofmemoryerror",
          "java.rmi.remoteexception"
        };
        // *INDENT-ON*

        // ------------------------------------------------ //
        while (!Thread.currentThread().isInterrupted() && executorThread.isAlive()) {
          long duration = System.currentTimeMillis() - startTime;
          if (!pause) {
            pause = (MainController.inst().getStatus() == Status.PAUSED);
          }

          try {
            Thread.sleep(CHECK_TIMEOUT_INTERVAL);
            // ------------------------------------------------ //
            // kill subprocess if out of memory
            final String errOut = eventHandler.getErrOut(true).trim();
            if (!errOut.isEmpty()) {
              LOGGER.info("Client error (id: " + this.id + "):\n" + errOut);

              if (Config.DEBUG) {
                COF.printLine("Client error (id: " + this.id + "):");
                COF.printEmptySeparator();
                COF.printLines(errOut);
              }

              for (final String killMessage : killMessages) {
                if (errOut.toLowerCase().contains(killMessage)) {
                  Thread.sleep(1000); // wait for client to shutdown itself
                  if (bashExecuter.isCommandStillRunning()) {
                    bashExecuter.killProcess();
                  }
                  break;
                }
              }
            }
            // ------------------------------------------------ //
            // monitoring, if timeout occures --> kill process
            final int timeout = cfg.getProp(ConfigInteger.PROCESSING_TIMEOUT_IN_SECONDS)
                * ONE_SECOND_IN_MS;
            // if (duration > timeout || cfg.indexingStopped() || pause) {
            if (duration > timeout) {
              // bashExecuter.killProcess();
              if (duration > timeout) {
                // EventManager.instance().failedToProcessFile(
                // "Timeout processing image with OCR-Engine.", originalPath);
              }
              // break;
            }
            // ------------------------------------------------ //
          } catch (InterruptedException e) {
            break;
          }
        }
        // ------------------------------------------------ //
      }

      errOutput = eventHandler.getErrOut();
      stdOutput = eventHandler.getStdOut();

      if (bashExecuter.isCommandStillRunning()) {
        executorThread.interrupt();
      }

      // String[] acceptedMessages = new String[] {
      // "using default language params",
      // "weak margin",
      // "test blob"
      // };

      // if (errOutput != null && !errOutput.trim().isEmpty()) {
      // for (String acceptedMessage : acceptedMessages) {
      // if (errOutput.toLowerCase().contains(acceptedMessage)) {
      // }
      // }
      // LOGGER.error(errOutput);
      // }

      // System.out.println(stdOutput);
      // System.out.println(errOutput);
      // return stdOutput;

      // System.out.println("err: " + errOutput);
      // System.out.println("out: " + stdOutput);

      // System.out.println("remove: " + this.id);
      processes.remove(this.id);
    }
  }

}
