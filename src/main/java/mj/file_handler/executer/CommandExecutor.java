package mj.file_handler.executer;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import mj.console.Platform.Os;

/**
*
*/
public class CommandExecutor implements Runnable {
  private Os os;
  private String command;
  private boolean commandStillRunning;
  private Process process = null;
  private static final String ROOT_PLACEHOLDER = "$$",
      WIN_NATIVE_LIB_FOLDER = "";
  private CommandExEventHandler eventHandler;

  /**
   *
   *
   * @param os
   * @param eventHandler
   */
  public CommandExecutor(Os os, CommandExEventHandler eventHandler) {
    this.os = os;
    this.eventHandler = eventHandler;

    if (eventHandler == null) {
      throw new NullPointerException("EVENTHANDLER IS NULL!");
    }
    if (os == null) {
      throw new NullPointerException("OS IS NULL!");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see Runnable#run()
   */
  public void run() {
    if (command == null) {
      throw new NullPointerException("NO COMMAND GIVEN");
    }
    commandStillRunning = true;
    try {
      eventHandler.executionStarted();

      if (os == Os.LINUX || os == Os.OSX) {
        process = Runtime.getRuntime().exec(
            new String[] { "bash", "-c", command });
      }

      if (os == Os.WINDOWS) {
        command = "cmd /c \""
            + command.replace(ROOT_PLACEHOLDER, WIN_NATIVE_LIB_FOLDER) + "\"";
        process = Runtime.getRuntime().exec(command);
      }

      getProcessOutput();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      commandStillRunning = false;
      eventHandler.executionStopped();
    }
  }

  /**
   *
   *
   */
  public void getProcessOutput() {
    try {
      if (process != null) {
        Thread thread1 = new Thread(new ProcessStandardOutput());
        Thread thread2 = new Thread(new ProcessErrorOutput());
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
      }
    } catch (InterruptedException e) {
      // TODO:
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   *
   */
  private class ProcessStandardOutput extends Thread {
    public void run() {
      try {
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(
            process.getInputStream()));
        String currentLine;
        while ((currentLine = stdInput.readLine()) != null && process != null) {
          eventHandler.standardOutput(currentLine);
        }
      } catch (Exception e) {
        // TODO:
      }
    }
  }

  /**
   *
   */
  private class ProcessErrorOutput extends Thread {
    public void run() {
      try {
        BufferedReader stdError = new BufferedReader(new InputStreamReader(
            process.getErrorStream()));

        String currentLine;
        while ((currentLine = stdError.readLine()) != null) {
          eventHandler.errorOutput(currentLine);
        }
      } catch (Exception e) {
        // TODO:
      }
    }
  }

  /**
   *
   *
   * @return
   */
  public boolean killProcess() {
    try {
      if (process != null) {
        eventHandler.processKilled();
        process.destroy();
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * @return the command
   */
  public String getCommand() {
    return command;
  }

  /**
   * @param command
   *          the command to set
   */
  public void setCommand(String command) {
    this.command = command;
  }

  /**
   * @return the commandStillRunning
   */
  public boolean isCommandStillRunning() {
    return commandStillRunning;
  }

  /**
   * @param commandStillRunning
   *          the commandStillRunning to set
   */
  public void setCommandStillRunning(boolean commandStillRunning) {
    this.commandStillRunning = commandStillRunning;
  }

}
