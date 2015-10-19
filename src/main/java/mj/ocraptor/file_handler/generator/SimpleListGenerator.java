package mj.ocraptor.file_handler.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import mj.ocraptor.file_handler.events.FileHandler;
import mj.ocraptor.file_handler.structures.FileList;
import mj.ocraptor.file_handler.structures.SequentialDirectoryWalker;
import mj.ocraptor.file_handler.structures.SimpleFileList;
import mj.ocraptor.file_handler.utils.FileTools;

public class SimpleListGenerator {
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SimpleListGenerator.class);

  /**
   *
   */
  public SimpleListGenerator() {
  }

  /**
   *
   *
   * @param directory
   * @return
   * @throws FileNotFoundException
   */
  public FileList generateFileList(String directory, FileHandler handler)
      throws FileNotFoundException {
    File input = new File(directory);
    try {
      SimpleFileList fileList = new SimpleFileList();
      FileTools.directoryIsValid(input, "Directory to index");

      SequentialDirectoryWalker walker = new SequentialDirectoryWalker(handler);
      List<File> files = walker.getFiles(input);
      fileList.addAll(files);
      return fileList;
    } catch (Exception e) {
      LOGGER.error(null, e);
    }
    return null;
  }
}
