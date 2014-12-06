package mj.javafx;

public enum Theme {
  METRO_LIGHT("MetroLight.css");

  private String theme;

  /**
   *
   */
  Theme(String fileName) {
    this.theme = fileName;
  }

  /**
   * {@inheritDoc}
   *
   * @see Object#toString()
   */
  public String toString() {
    return this.theme;
  }

  /**
   *
   *
   * @param theme
   * @return
   */
  public static Theme getByName(String themeString) {
    for (Theme th : Theme.values()) {
      if (th.toString().equals(themeString + ".css")) {
        return th;
      }
    }
    return null;
  }

  /**
   *
   *
   * @return
   */
  public static String[] valuesAsString() {
    Theme[] themes = Theme.values();
    String[] themeStrings = new String[themes.length];
    for (int i = 0; i < themes.length; i++) {
      themeStrings[i] = themes[i].toString().replace(".css", "");
    }
    return themeStrings;
  }
}
