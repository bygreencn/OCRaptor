package mj.ocraptor.tools;

import ij.IJ;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 *
 * @author
 */
public class TicToc {
  public static long startTime;

  private static ConcurrentHashMap<String, Long> ticTocs = new ConcurrentHashMap<String, Long>();
  private static ConcurrentHashMap<String, Long> ticTocsAccumulated = new ConcurrentHashMap<String, Long>();

  /**
   *
   *
   */
  public static void tic() {
    tic("Default");
  }

  /**
   *
   *
   */
  public static void toc() {
    toc("Default");
  }

  /**
   *
   *
   * @param name
   */
  public static void tic(String name) {
    ticTocs.put(name, System.currentTimeMillis());
    if (!ticTocsAccumulated.containsKey(name)) {
      ticTocsAccumulated.put(name, 0L);
    }
  }

  /**
   *
   *
   * @param name
   */
  public static void toc(String name) {
    Long ticToc = ticTocs.get(name);
    if (ticToc != null) {
      long elapsedTime = System.currentTimeMillis() - ticToc;

      long accumulatedTime = 0;
      if (ticTocsAccumulated.containsKey(name)) {
        accumulatedTime = ticTocsAccumulated.get(name) + elapsedTime;
        ticTocsAccumulated.put(name, accumulatedTime);
      }

      String output = "TicToc : '" + name + "' - Elapsed time: %d.%03dsec";
      if (accumulatedTime > 0) {
        output += " (%d.%03dsec)";
      }
      System.out.println(String.format(output, elapsedTime / 1000, elapsedTime % 1000,
          accumulatedTime / 1000, accumulatedTime % 1000));
    }
  }

  /**
   * @return the ticTocs
   */
  public static ConcurrentHashMap<String, Long> getTicTocs() {
    return ticTocs;
  }

  /**
   * @param ticTocs
   *          the ticTocs to set
   */
  public static void setTicTocs(ConcurrentHashMap<String, Long> ticTocs) {
    TicToc.ticTocs = ticTocs;
  }

  /**
   * Let's test some shit!
   *
   * @throws InterruptedException
   */
  public static void main(String[] args) throws InterruptedException {
    TicToc.tic("foo");
    Thread.sleep(100);
    TicToc.toc("foo");
    TicToc.tic("foo");
    Thread.sleep(100);
    TicToc.toc("foo");
  }

}
