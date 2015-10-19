package mj.ocraptor.configuration;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 *
 *
 * @author
 */
public class PropertiesManager {
  private String configFile;
  private Properties properties;

  /**
   *
   */
  public PropertiesManager(String configFile) {
    this.configFile = configFile;
  }

  /**
   *
   *
   * @return
   *
   * @throws IOException
   */
  public Properties getProperties() throws IOException {
    if (configFile != null) {
      this.properties = new Properties();
      BufferedInputStream stream = new BufferedInputStream(new FileInputStream(configFile));
      InputStreamReader in = new InputStreamReader(stream, "UTF-8");
      BufferedReader buf = new BufferedReader(in);
      this.properties.load(buf);
      stream.close();
      return properties;
    }
    return null;
  }
}
