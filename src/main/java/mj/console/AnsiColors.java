package mj.console;

public enum AnsiColors {
  DEFAULT("\033[0m"), BOLD("\033[1m"), BLUE("\033[34m"), WHITE("\033[37m"), //
  BLACK_BACKGROUND("\033[40m"), GREEN_BACKGROUND("\033[42m"), //
  RED_BACKGROUND("\033[41m"), BLUE_BACKGROUND("\033[44m"), WHITE_BACKGROUND("\033[47m"), //
  MAGENTA_BACKGROUND("\033[45m"), CYAN_BACKGROUND("\033[46m"), //
  BLACK("\033[30m"), RED("\033[31m"), GREEN("\033[32m"), YELLOW("\033[33m"), UNDERLINE("\033[4m");

  private String symbol;

  /**
   * @param symbol
   */
  AnsiColors(String symbol) {
    this.symbol = symbol;
  }

  /**
   * {@inheritDoc}
   *
   * @see Object#toString()
   */
  public String toString() {
    return this.symbol;
  }

  /**
   *
   *
   * @return
   */
  public String toStringEscaped() {
    return this.symbol.replace("[", "\\[");
  }

}
