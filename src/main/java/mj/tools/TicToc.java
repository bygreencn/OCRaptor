package mj.tools;

import ij.IJ;

public class TicToc {
  public static long startTime;

  public static void tic() {
    startTime = System.currentTimeMillis();
  }

  public static void toc() {
    long elapsedTime = System.currentTimeMillis() - startTime;
    IJ.log(String.format("Elapsed time: %d.%03dsec", elapsedTime / 1000, elapsedTime % 1000));
  }
}
