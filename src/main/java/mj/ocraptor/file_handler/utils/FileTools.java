package mj.ocraptor.file_handler.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.tools.St;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class FileTools {

  private static final Logger LOG = Logger.getLogger(FileTools.class.getName());

  /**
   *
   *
   * @param textFile
   * @return
   */
  public static int countTextfileLines(final File textFile) {
    BufferedReader reader = null;
    int lines = 0;
    try {
      reader = new BufferedReader(new FileReader(textFile));
      while (reader.readLine() != null)
        lines++;
    } catch (Exception e) {
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
    }
    return lines;
  }

  /**
   *
   *
   * @param dir
   * @param dirDescription
   * @throws NullPointerException
   * @throws IllegalAccessError
   * @throws FileNotFoundException
   */
  public static void directoryIsValid(final String dir, final String dirDescription)
      throws FileNotFoundException, NullPointerException, IllegalAccessError {
    directoryIsValid(dir == null ? null : new File(dir), dirDescription);
  }

  /**
   *
   *
   * @param dir
   * @param dirDescription
   * @throws FileNotFoundException
   * @throws IllegalAccessError
   * @throws NullPointerException
   */
  public static void directoryIsValid(final File dir, final String dirDescription)
      throws FileNotFoundException, NullPointerException, IllegalAccessError {

    if (dir == null) {
      final String errorMessage = "\n" + dirDescription + " is null!";
      throw new NullPointerException(errorMessage);
    }

    if (!dir.exists() || !dir.isDirectory()) {
      final String errorMessage = dirDescription + "is not valid: \"" + dir.getAbsolutePath()
          + "\"";
      throw new FileNotFoundException(errorMessage);
    }

    if (!dir.canWrite()) {
      final String errorMessage = "\nno writing-permissions for " + dirDescription + ": \""
          + dir.getAbsolutePath() + "\"";
      throw new IllegalAccessError(errorMessage);
    }
  }

  /**
   *
   *
   * @param file
   * @return
   */
  public static double getFileSizeInKB(final File file) {
    double bytes = file.length();
    double kilobytes = (bytes / 1024);
    return kilobytes;
  }

  /**
   *
   *
   * @param file
   * @return
   * @throws IOException
   */
  public static String calculateMD5FromFile(final File file) {
    String md5 = null;
    try {
      FileInputStream fis = new FileInputStream(file);
      md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return md5;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param prefix
   * @param addRandomString
   * @return
   */
  public static File getTempFolder(final String prefix, final boolean addRandomString) {
    return getTempFile(prefix, "", addRandomString);
  }

  /**
   *
   *
   * @param fileNameToAppend
   * @return
   */
  public static File getTempFile(final String fileNameToAppend, final boolean addRandomString) {
    final String normalizedFileName = St.normalizeFileName(fileNameToAppend);
    final String fileExtension = getFileExtension(fileNameToAppend);
    return getTempFile(normalizedFileName, fileExtension, addRandomString);
  }

  /**
   *
   *
   * @param prefix
   * @param suffix
   * @return
   */
  public static File getTempFile(final String prefix, final String suffix,
      final boolean addRandomString) {
    final String randomNumber = String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
    final String randomFileName = prefix + (addRandomString ? "_" + randomNumber : "") + suffix;
    final File outputFile = new File(getTempDirectory(), randomFileName);
    return outputFile;
  }

  /**
   *
   *
   * @return
   */
  private static File getTempDirectory() {
    final File tempFolder = new File(FileUtils.getTempDirectory(), Config.APP_NAME_LOWER);
    if (tempFolder.isFile()) {
      // TODO: log
    }
    if (!tempFolder.exists()) {
      tempFolder.mkdir();
    }
    return tempFolder;
  }

  /**
   *
   *
   */
  public static void clearTempDirectory() {
    final File tempFolder = getTempDirectory();
    deleteFolderContents(tempFolder);
  }

  /**
   *
   *
   * @param folder
   */
  public static void deleteFolderContents(final File folder) {
    final File[] files = folder.listFiles();
    if (files != null) {
      for (final File f : files) {
        if (f.isDirectory()) {
          deleteFolderContents(f);
        }
        f.delete();
      }
    }
  }

  // ------------------------------------------------ //

  /**
   *
   *
   * @param file
   * @return
   */
  public static String getFileExtension(File file) {
    return getFileExtension(file.getName());
  }

  /**
   *
   *
   * @param fileName
   * @return
   */
  public static String getFileExtension(String fileName) {
    if (fileName != null) {
      int lastIndex = fileName.lastIndexOf(".");
      if (lastIndex != -1 && lastIndex != fileName.length() - 1) {
        return fileName.substring(lastIndex + 1, fileName.length());
      }
    }
    return null;
  }

  /**
   *
   *
   * @param fileToWriteTo
   * @param stringToWrite
   * @throws IOException
   */
  public static void stringToFile(final String stringToWrite, final File fileToWriteTo)
      throws IOException {
    BufferedWriter out = null;
    try {
      final int outputBufferSize = 32768;
      out = new BufferedWriter(new FileWriter(fileToWriteTo), outputBufferSize);
      out.write(stringToWrite);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  /**
   *
   *
   * @return
   * @throws IOException
   */
  public static String fileToString(final File file) throws IOException {
    return IOUtils.toString(file.toURI());
  }


  /**
   *
   *
   * @param file
   * @return
   */
  public static String multiplatformPath(final File file) {
    try {
      return multiplatformPath(file.getCanonicalPath());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return file.getAbsolutePath();
  }

  /**
   *
   *
   * @param path
   * @return
   */
  public static String multiplatformPath(final String path) {
    return path.replace("\\", "/");
  }

  /**
   * Taken from:
   * http://rosettacode.org/wiki/Find_common_directory_path
   *
   * @param paths
   * @return
   */
  // *INDENT-OFF*
  public static String getCommonPath(String... paths){
    String commonPath = "";
    String[][] folders = new String[paths.length][];
    for(int i = 0; i < paths.length; i++){
      folders[i] = paths[i].split("/"); //split on file separator
    }
    for(int j = 0; j < folders[0].length; j++){
      String thisFolder = folders[0][j]; //grab the next folder name in the first path
      boolean allMatched = true; //assume all have matched in case there are no more paths
      for(int i = 1; i < folders.length && allMatched; i++){ //look at the other paths
        if(folders[i].length < j){ //if there is no folder here
          allMatched = false; //no match
          break; //stop looking because we've gone as far as we can
        }
        //otherwise
        allMatched &= folders[i][j].equals(thisFolder); //check if it matched
      }
      if(allMatched){ //if they all matched this folder name
        commonPath += thisFolder + "/"; //add it to the answer
      }else{//otherwise
        break;//stop looking
      }
    }
    return commonPath;
  }
  // *INDENT-ON*
}
