package mj.configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *
 * @author
 */
public class PropertiesChanger {

  private final Map<String, String> properties = new HashMap<String, String>();
  private final List<String> propertyLines = new ArrayList<String>();
  private boolean allowNewProperties;

  /**
   *
   */
  public PropertiesChanger(final boolean allowNewProperties) {
    this.allowNewProperties = allowNewProperties;
  }

  /**
   *
   *
   * @param name
   * @param value
   */
  public void setProperty(String name, String value) {
    properties.put(name.trim(), value.trim());
  }

  /**
   *
   *
   * @param is
   *
   * @throws IOException
   */
  public void load(InputStream is) throws IOException {
    this.properties.clear();
    this.propertyLines.clear();

    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"));
    String line;
    while ((line = reader.readLine()) != null) {
      propertyLines.add(line);
    }
    is.close();
  }

  public static final String MODIFIED_DATA = "# MODIFIED ON: ";
  SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);

  /**
   *
   *
   * @param os
   */
  public void save(OutputStream os) {
    Pattern pattern = Pattern.compile("([^=]*)=.*");
    PrintStream printStream = new PrintStream(os);
    List<String> usedProps = new ArrayList<String>();
    final String currentDate = dateFormat.format(new Date());

    String modString = null;

    for (String propertyLine : propertyLines) {
      Matcher matcher = pattern.matcher(propertyLine);
      if (matcher.find()) {
        String propName = matcher.group(1);
        if (properties.containsKey(propName.trim())) {
          String value = properties.get(propName.trim());
          printStream.println(MODIFIED_DATA + currentDate + "\n" + propName + "= " + value);
          usedProps.add(propName.trim());
          modString = null;
        } else {
          if (modString != null) {
            printStream.println(modString);
            modString = null;
          }
          printStream.println(propertyLine);
        }
      } else {
        if (!propertyLine.startsWith(MODIFIED_DATA)) {
          printStream.println(propertyLine);
        } else {
          modString = propertyLine;
        }
      }
    }

    // finally add all new properties
    if (this.allowNewProperties) {
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        if (!usedProps.contains(entry.getKey())) {
          printStream.println("\n" + MODIFIED_DATA + currentDate + "\n" + entry.getKey() + " = " + entry.getValue());
        }
      }
    }

    printStream.close();
  }
}
