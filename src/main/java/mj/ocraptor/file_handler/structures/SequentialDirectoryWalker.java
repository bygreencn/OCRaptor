package mj.ocraptor.file_handler.structures;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mj.ocraptor.file_handler.events.FileHandler;
import mj.ocraptor.file_handler.utils.FileTools;

import org.apache.commons.io.DirectoryWalker;

/**
 *
 *
 * @author
 */
public class SequentialDirectoryWalker extends DirectoryWalker<File> {
  private FileHandler handler;
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SequentialDirectoryWalker.class);

  /**
   * {@inheritDoc}
   *
   * @see DirectoryWalker#SimpleDirectoryWalker()
   */
  public SequentialDirectoryWalker(FileHandler handler) {
    // super(new XMLChildFileFilter(), -1);
    super();
    this.handler = handler;
  }

  /**
   * @param directory
   * @param depth
   * @param results
   * @return
   *
   * @throws IOException
   */
  @Override
  protected boolean handleDirectory(File directory, int depth, Collection<File> results)
      throws IOException {
    this.handler.handleDir(directory);
    return true;
  }

  /**
   * @param file
   * @param depth
   * @param results
   *
   * @throws IOException
   */
  @Override
  protected void handleFile(File file, int depth, Collection<File> results) throws IOException {
    this.handler.handleFile(file);
    results.add(file);
  }

  /**
   *
   *
   * @return
   * @throws FileNotFoundException
   */
  public List<File> getFiles(File directory) throws FileNotFoundException {
    List<File> files = new ArrayList<File>();
    try {
      FileTools.directoryIsValid(directory, "SequentialDirectoryWalker directory");
      walk(directory, files);
    } catch (Exception e) {
      LOGGER.error("Directory is not valid!", e);
    }
    return files;
  }
}
