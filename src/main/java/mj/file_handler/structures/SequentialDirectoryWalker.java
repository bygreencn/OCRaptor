package mj.file_handler.structures;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mj.file_handler.events.FileHandler;
import mj.file_handler.utils.FileTools;

import org.apache.commons.io.DirectoryWalker;

/**
 *
 *
 * @author
 */
public class SequentialDirectoryWalker extends DirectoryWalker<File> {
  private FileHandler handler;

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
  protected boolean handleDirectory(File directory, int depth,
      Collection<File> results) throws IOException {
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
  protected void handleFile(File file, int depth, Collection<File> results)
      throws IOException {
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
    FileTools.directoryIsValidAndNotNull(directory, true, " --- ");
    List<File> files = new ArrayList<File>();
    try {
      walk(directory, files);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return files;
  }
}
