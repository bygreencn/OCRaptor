package mj.file_handler.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import mj.file_handler.events.FileHandler;
import mj.file_handler.structures.FileList;
import mj.file_handler.structures.SequentialDirectoryWalker;
import mj.file_handler.structures.SimpleFileList;
import mj.file_handler.utils.FileTools;

public class SimpleListGenerator {

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
    FileTools.directoryIsValidAndNotNull(input, true,
        "GIVEN DIRECTORY PATH STRING IS NULL!");

    SimpleFileList fileList = new SimpleFileList();
    SequentialDirectoryWalker walker = new SequentialDirectoryWalker(handler);
    try {
      List<File> files = walker.getFiles(input);
      fileList.addAll(files);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return fileList;
  }
}
