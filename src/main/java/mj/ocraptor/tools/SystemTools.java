package mj.ocraptor.tools;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;

import dnl.utils.text.table.TextTable;

public class SystemTools {

  /**
   *
   *
   * @param args
   *
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    printRuntimeStats();
    // System.out.println(getXmxParameterInMB());
    // weakReferenceTest(false);
    // weakReferenceTest(true);
    softReferenceTest(false);
    // softReferenceTest(true);
  }

  /**
   *
   *
   * @param withSystemGC
   *
   * @throws Exception
   */
  @SuppressWarnings("unused")
  private static void weakReferenceTest(boolean withSystemGC) throws Exception {
    System.out.println(StringUtils.repeat("#", 70));
    System.out.println(StringUtils.repeat("#", 70));

    System.out.println("\nWeakReference - SystemGC: " + withSystemGC);
    SystemTools systemTools = new SystemTools();
    WeakReferenceSer<String> weakReferenceString = null;
    for (int i = 0; i < 5; i++) {
      printRuntimeStats();
      weakReferenceString = new WeakReferenceSer<String>(StringUtils.repeat("k", 100000000));
      if (withSystemGC) {
        System.gc(); // will clear all weakreferences
      }
    }
    if (weakReferenceString.get() != null) {
      String substring = weakReferenceString.get().substring(0, 10);
      System.out.println("\nstring is NOT NULL: " + substring + "...\n");
    } else {
      System.out.println("\nstring is NULL\n");
    }
  }

  /**
   *
   *
   * @param withSystemGC
   *
   * @throws Exception
   */
  @SuppressWarnings("unused")
  private static void softReferenceTest(boolean withSystemGC) throws Exception {
    System.out.println(StringUtils.repeat("#", 70));
    System.out.println(StringUtils.repeat("#", 70));

    System.out.println("\nSoftReference - SystemGC: " + withSystemGC);
    SystemTools systemTools = new SystemTools();
    SoftReferenceSer<String> softRefrenceString = null;
    for (int i = 0; i < 5; i++) {
      System.out.println((double) SystemTools.getRuntimeTotalMemoryInMB()
          / (double) SystemTools.getXmxParameterInMB());
      printRuntimeStats();
      softRefrenceString = new SoftReferenceSer<String>(StringUtils.repeat("k", 100000000));
      if (withSystemGC) {
        // gc won't clear the softreferences if there is still memory left
        new SoftReferenceSer<String>(StringUtils.repeat("k", 300000000));
        System.gc(); // will clear all softreferences
      }
    }
    if (softRefrenceString.get() != null) {
      String substring = softRefrenceString.get().substring(0, 10);
      System.out.println("\nstring is NOT NULL: " + substring + "...\n");
    } else {
      System.out.println("\nstring is NULL\n");
    }
  }

  // ------------------------------------------------ //

  private Sigar sigar;

  private static final int KB = 1024;
  private static final int MB = KB * KB;

  // *INDENT-OFF*
  private static final String
    FREE_RAM_KEY    = "ActualFree" ,
    TOTAL_RAM_KEY   = "Total"      ,
    XMX_PREFIX      = "-xmx"       ,
    XMS_PREFIX      = "-xms"       ;
  // *INDENT-ON*

  // ------------------------------------------------ //

  /**
   */
  public SystemTools() {
    //
  }

  /**
   *
   *
   */
  private void initSigarTools() {
    if (this.sigar == null) {
      this.sigar = new Sigar();
    }
  }

  /**
   *
   *
   * @return
   *
   * @throws Exception
   */
  public double getCpuPercent() throws Exception {
    this.initSigarTools();
    final CpuPerc perc = sigar.getCpuPerc();
    return perc.getCombined() * 100;
  }

  /**
   *
   *
   * @return
   *
   * @throws Exception
   */
  public long getUsedRamInKB() throws Exception {
    return getMaxRamInKB() - getFreeRamInKB();
  }

  /**
   *
   *
   * @return
   *
   * @throws Exception
   */
  public long getUsedRamInMB() throws Exception {
    return getMaxRamInMB() - getFreeRamInMB();
  }

  /**
   *
   *
   * @return
   *
   * @throws Exception
   */
  public String getUsedRamInReadable() throws Exception {
    this.initSigarTools();
    final String size = Sigar.formatSize(Long.valueOf(getMemMap().get(TOTAL_RAM_KEY))
        - Long.valueOf(getMemMap().get(FREE_RAM_KEY)));
    return size;
  }

  /**
   *
   *
   * @return
   * @throws Exception
   */
  public long getFreeRamInKB() throws Exception {
    final long freeRamB = Long.valueOf(getMemMap().get(FREE_RAM_KEY));
    final long freeRamKB = freeRamB / 1024;
    return freeRamKB;
  }

  /**
   *
   *
   * @return
   *
   * @throws Exception
   */
  public long getFreeRamInMB() throws Exception {
    return getFreeRamInKB() / 1024;
  }

  /**
   *
   *
   * @return
   *
   * @throws Exception
   */
  public long getMaxRamInKB() throws Exception {
    final long totalRamB = Long.valueOf(getMemMap().get(TOTAL_RAM_KEY));
    final long totalRamKB = totalRamB / 1024;
    return totalRamKB;
  }

  /**
   *
   *
   * @return
   *
   * @throws Exception
   */
  public long getMaxRamInMB() throws Exception {
    return getMaxRamInKB() / 1024;
  }

  /**
   *
   *
   * @return
   *
   * @throws Exception
   */
  public String getFreeRamInReadable() throws Exception {
    this.initSigarTools();
    final String size = Sigar.formatSize(Long.valueOf(getMemMap().get(FREE_RAM_KEY)));
    return size;
  }

  /**
   *
   *
   * @return
   *
   * @throws Exception
   */
  public String getMaxRamInReadable() throws Exception {
    this.initSigarTools();
    final String size = Sigar.formatSize(Long.valueOf(getMemMap().get(TOTAL_RAM_KEY)));
    return size;
  }

  /**
   *
   *
   * @return
   *
   * @throws Exception
   */
  private Map<String, String> getMemMap() throws Exception {
    this.initSigarTools();
    final Mem mem = sigar.getMem();
    @SuppressWarnings("unchecked")
    final Map<String, String> memMap = mem.toMap();
    return memMap;
  }

  // ------------------------------------------------ //

  /**
   *
   *
   * @return
   */
  public static long getRuntimeUsedMemoryInMB() {
    return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / MB;
  }

  /**
   *
   *
   * @return
   */
  public static long getRuntimeFreeMemoryInMB() {
    return Runtime.getRuntime().freeMemory() / MB;
  }

  /**
   *
   *
   * @return
   */
  public static long getRuntimeTotalMemoryInMB() {
    return Runtime.getRuntime().totalMemory() / MB;
  }

  /**
   *
   *
   * @return
   */
  public static long getRuntimeMaxMemoryInMB() {
    return Runtime.getRuntime().maxMemory() / MB;
  }

  // ------------------------------------------------ //

  /**
   *
   *
   * @return
   */
  public static long getRuntimeUsedMemoryInKB() {
    return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / KB;
  }

  /**
   *
   *
   * @return
   */
  public static long getRuntimeFreeMemoryInKB() {
    return Runtime.getRuntime().freeMemory() / KB;
  }

  /**
   *
   *
   * @return
   */
  public static long getRuntimeTotalMemoryInKB() {
    return Runtime.getRuntime().totalMemory() / KB;
  }

  /**
   *
   *
   * @return
   */
  public static long getRuntimeMaxMemoryInKB() {
    return Runtime.getRuntime().maxMemory() / KB;
  }

  // ------------------------------------------------ //

  public static void printRuntimeStats() {
    // *INDENT-OFF*
    String[] columns = new String[]{"type", "in mb", "in kb"};
    String[][] data  = new String[][]{
      // ------------------------------------------------ //
      {"runtime - max "  , String.valueOf(getRuntimeMaxMemoryInMB()),
        String.valueOf(getRuntimeMaxMemoryInKB())},
      {"runtime - total ", String.valueOf(getRuntimeTotalMemoryInMB()),
        String.valueOf(getRuntimeTotalMemoryInKB())},
      {"runtime - used " , String.valueOf(getRuntimeUsedMemoryInMB()),
        String.valueOf(getRuntimeUsedMemoryInKB())},
      {"runtime - free " , String.valueOf(getRuntimeFreeMemoryInMB()),
        String.valueOf(getRuntimeFreeMemoryInKB() )},
      // ------------------------------------------------ //
      {"runtime - xmx " , String.valueOf(getXmxParameterInMB()),
        String.valueOf(getXmxParameterInKB())},
      {"runtime - xms " , String.valueOf(getXmsParameterInMB()),
        String.valueOf(getXmsParameterInKB())}
      // ------------------------------------------------ //
    };
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    new TextTable(columns, data).printTable(new PrintStream(outputStream), 0);
    try {
      System.out.println(outputStream.toString("utf-8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    // *INDENT-ON*
  }

  // ------------------------------------------------ //

  /**
   * @return
   *
   *
   */
  public static List<String> getRuntimeParameters() {
    final RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
    return bean.getInputArguments();
  }

  /**
   *
   *
   * @return
   */
  public static Long getXmxParameterInKB() {
    final Long xmx = getXmParameter(true);
    if (xmx != null && !xmx.equals(-1)) {
      return xmx / KB;
    }
    return null;
  }

  /**
   *
   *
   * @return
   */
  public static Long getXmsParameterInKB() {
    final Long xms = getXmParameter(false);
    if (xms != null && !xms.equals(-1)) {
      return xms / KB;
    }
    return null;
  }

  /**
   *
   *
   * @return
   */
  public static Long getXmxParameterInMB() {
    final Long xmx = getXmParameter(true);
    if (xmx != null && !xmx.equals(-1)) {
      return xmx / MB;
    }
    return null;
  }

  /**
   *
   *
   * @return
   */
  public static Long getXmsParameterInMB() {
    final Long xms = getXmParameter(false);
    if (xms != null && !xms.equals(-1)) {
      return xms / MB;
    }
    return null;
  }

  /**
   *
   *
   * @param max
   * @return
   */
  public static Long getXmParameter(final boolean max) {
    final String parPrefix = max ? XMX_PREFIX : XMS_PREFIX;
    final List<String> parameters = getRuntimeParameters();
    if (parameters != null) {
      for (String par : parameters) {
        par = par.toLowerCase();
        if (par.startsWith(parPrefix)) {
          final String fileSize = par.replace(parPrefix, "");
          return St.humanReadableFileSizetoBytes(fileSize);
        }
      }
    }
    return null;
  }

}
