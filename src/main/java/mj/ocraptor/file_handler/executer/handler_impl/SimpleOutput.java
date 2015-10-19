package mj.ocraptor.file_handler.executer.handler_impl;

import mj.ocraptor.file_handler.executer.CommandExEventHandler;

public class SimpleOutput extends CommandExEventHandler {
  private StringBuffer stdBuffer;
  private StringBuffer errBuffer;

  /**
   *
   */
  public SimpleOutput() {
    stdBuffer = new StringBuffer();
    errBuffer = new StringBuffer();
  }

  @Override
  public void standardOutput(String line) {
    stdBuffer.append(line + "\n");
  }

  @Override
  public void errorOutput(String line) {
    errBuffer.append(line + "\n");
  }

  @Override
  public void executionStarted() {

  }

  @Override
  public void executionStopped() {

  }

  @Override
  public void processKilled() {

  }


  /**
   *
   *
   * @return
   */
  public String getErrOut() {
    return this.getErrOut(false);
  }

  /**
   *
   *
   * @param clear
   * @return
   */
  public String getStdOut() {
    return this.getStdOut(false);
  }


  /**
   * @return the stdout
   */
  public String getStdOut(boolean clear) {
    try {
      return stdBuffer.toString();
    } finally {
      if (clear) {
        stdBuffer = new StringBuffer();
      }
    }
  }

  /**
   * @return the stdout
   */
  public String getErrOut(boolean clear) {
    try {
      return errBuffer.toString();
    } finally {
      if (clear) {
        errBuffer = new StringBuffer();
      }
    }
  }
}
