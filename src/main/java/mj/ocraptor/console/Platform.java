package mj.ocraptor.console;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;

import org.apache.commons.lang.SystemUtils;

public class Platform {

  /**
   *
   */
  public enum Os {
    WINDOWS, OSX, LINUX, UNKNOWN
  }

  public static Os getSystem() {
    if (SystemUtils.IS_OS_LINUX) {
      return Os.LINUX;
    }
    if (SystemUtils.IS_OS_WINDOWS) {
      return Os.WINDOWS;
    }
    if (SystemUtils.IS_OS_MAC) {
      return Os.OSX;
    }
    return Os.UNKNOWN;
  }

  /**
   *
   *
   */
  public static void setPathVariable(String command, String variable,
      String value) {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.environment().put(variable, value);
      processBuilder.start();
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    }
  }

  public static void openWebpage(URI uri) throws Exception {
    Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop()
        : null;
    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
      desktop.browse(uri);
    }
  }

  public static void openWebpage(URL url) throws Exception {
    openWebpage(url.toURI());
  }
}
