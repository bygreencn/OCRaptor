package mj.file_handler.executer.handler_impl;

import mj.file_handler.executer.CommandExEventHandler;

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
   * @return the stdout
   */
  public String getStdOut() {
    return stdBuffer.toString();
  }

  /**
   * @return the stdout
   */
  public String getErrOut() {
    return errBuffer.toString();
  }
}
