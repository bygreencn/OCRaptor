package mj.extraction.image_processing.language;

/**
 *
 *
 * @author
 */
public enum Language {
  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //
  UNKNOWN("---:---"), DEUTSCH("deu:de"), ENGLISH("eng:en");
  // ------------------------------------------------ //

  //

  private String symbol;

  /**
   *
   *
   * @param symbol
   */
  private Language(String symbol) {
    this.symbol = symbol;
  }

  /**
   * {@inheritDoc}
   *
   * @see Object#toString()
   */
  public String toString() {
    return symbol.split(":")[0];
  }

  /**
   *
   *
   * @return
   */
  private String getRawSymbol() {
    return symbol;
  }

  /**
   *
   *
   * @param mime
   * @return
   */
  public static Language get(String lang) {
    if (lang != null) {
      lang = lang.trim();
      for (Language language : Language.values()) {
        String symbol = language.getRawSymbol();
        if (symbol.startsWith(lang + ":") || symbol.endsWith(":" + lang)) {
          return language;
        }
      }
    }
    return Language.UNKNOWN;
  }
}
