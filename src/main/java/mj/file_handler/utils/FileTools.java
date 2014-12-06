package mj.file_handler.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class FileTools {

  private static final Logger LOG = Logger.getLogger(FileTools.class.getName());

  /**
   *
   *
   * @param dir
   * @param dirDescription
   * @return
   * @throws FileNotFoundException
   */
  public static void directoryIsValid(File dir, boolean logger,
      String dirDescription) throws FileNotFoundException {
    if (!dir.exists() || !dir.isDirectory()) {
      final String errorMessage = "\n" + dirDescription.toUpperCase()
          + " NOT VALID: \n\"" + dir.getAbsolutePath() + "\"\n";
      if (logger) {
        LOG.fatal(errorMessage);
      }
      throw new FileNotFoundException(errorMessage);
    }
    if (!dir.canWrite()) {
      final String errorMessage = "\nNO WRITING-PERMISSIONS FOR "
          + dirDescription.toUpperCase() + ": \n\"\n" + dir.getAbsolutePath()
          + "\"";
      if (logger) {
        LOG.fatal(errorMessage);
      }
      throw new IllegalAccessError(errorMessage);
    }
  }

  /**
   *
   *
   * @param dir
   * @param dirDescription
   * @return
   * @throws FileNotFoundException
   */
  public static void directoryIsValidAndNotNull(File dir, boolean logger,
      String dirDescription) throws FileNotFoundException {
    if (dir == null) {
      final String errorMessage = "\n" + dirDescription.toUpperCase()
          + " IS NULL!";
      if (logger) {
        LOG.fatal(errorMessage);
      }
      throw new NullPointerException(errorMessage);
    }
    directoryIsValid(dir, logger, dirDescription);
  }

  /**
   *
   *
   * @param file
   * @return
   */
  public static double getFileSizeInKB(File file) {
    double bytes = file.length();
    double kilobytes = (bytes / 1024);
    return kilobytes;
  }

  /**
   *
   *
   * @param prefix
   * @param suffix
   * @return
   */
  public static File getRandomTempFile(String prefix, String suffix) {
    String randomFileName = prefix + "_"
        + new Random().nextInt(Integer.MAX_VALUE) + "_" + suffix;
    File outputFile = new File(FileUtils.getTempDirectory(), randomFileName);
    return outputFile;
  }
}
