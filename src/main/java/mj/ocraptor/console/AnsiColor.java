package mj.ocraptor.console;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mj.ocraptor.tools.St;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public enum AnsiColor {
  // *INDENT-OFF*
  DEFAULT            ("\033[0m"),
  BOLD               ("\033[1m"),
  UNDERLINE          ("\033[4m"),
  BLINK_ON           ("\033[5m"),

  BLACK              ("\033[30m"),
  BLACK_BACKGROUND   ("\033[40m"),

  WHITE              ("\033[37m"),
  WHITE_BACKGROUND   ("\033[47m"),

  GREEN              ("\033[32m"),
  GREEN_BACKGROUND   ("\033[42m"),

  BLUE               ("\033[34m"),
  BLUE_BACKGROUND    ("\033[44m"),

  RED                ("\033[31m"),
  RED_BACKGROUND     ("\033[41m"),

  MAGENTA            ("\033[35m"),
  MAGENTA_BACKGROUND ("\033[45m"),

  CYAN               ("\033[36m"),
  CYAN_BACKGROUND    ("\033[46m"),

  YELLOW             ("\033[33m"),
  YELLOW_BACKGROUND  ("\033[43m");
  // *INDENT-ON*

  private String symbol;

  /**
   * @param symbol
   */
  AnsiColor(String symbol) {
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

  /**
   *
   *
   * @param formattedString
   * @return
   */
  public static String removeAnsiColors(String formattedString) {
    for (AnsiColor color : AnsiColor.values()) {
      formattedString = formattedString.replace(color.toString(), "");
    }
    return formattedString;
  }

  /**
   *
   *
   * @param formattedString
   * @return
   */
  public static int calculateOffset(final String formattedString) {
    int offset = 0;
    for (final AnsiColor color : AnsiColor.values()) {
      final int matchCount = StringUtils.countMatches(formattedString, color.toString());
      offset += (color.toString().length() * matchCount);
    }
    return offset;
  }

  /**
   *
   *
   * @param colors
   * @param shift
   * @return
   */
  public static SortedMap<Integer, AnsiColor[]> shiftColorIndex(
      final SortedMap<Integer, AnsiColor[]> colors, int shift) {
    if (colors == null) {
      return null;
    }

    // *INDENT-OFF*
    final SortedMap<Integer, AnsiColor[]> modifiedColors
      = new TreeMap<Integer, AnsiColor[]>(Collections.reverseOrder());
    // *INDENT-ON*

    boolean addLastAnsiColorBeforeShift = true;

    for (final Entry<Integer, AnsiColor[]> color : colors.entrySet()) {
      final Integer index = color.getKey();
      final AnsiColor[] ansiColors = color.getValue();
      int adjustedIndex = index - shift;

      if (adjustedIndex > 0) {
        modifiedColors.put(adjustedIndex, ansiColors);
      } else if (addLastAnsiColorBeforeShift) {
        if (!modifiedColors.containsKey(0)) {
          modifiedColors.put(0, ansiColors);
        }
        addLastAnsiColorBeforeShift = false;
      }
    }
    return modifiedColors;
  }

  /**
   *
   *
   * @return
   */
  public static SortedMap<Integer, AnsiColor[]> getColorIndex(String coloredString) {
    final SortedMap<Integer, AnsiColor[]> color = new TreeMap<Integer, AnsiColor[]>(Collections
        .reverseOrder());

    Pattern patt = Pattern.compile("\u001B\\[[;\\d]*m", Pattern.CASE_INSENSITIVE);
    Matcher matcher = patt.matcher(coloredString);

    while (matcher.find()) {
      int startIndex = matcher.start();
      String colorAsString = matcher.group();
      AnsiColor colorFromString = getFromString(colorAsString);
      coloredString = coloredString.replaceFirst(Pattern.quote(colorFromString.toString()), "");
      matcher = patt.matcher(coloredString);
      if (color.containsKey(startIndex)) {
        AnsiColor[] savedColors = color.get(startIndex);
        savedColors = ArrayUtils.add(savedColors, savedColors.length, colorFromString);
        color.put(startIndex, savedColors);
      } else {
        color.put(startIndex, new AnsiColor[] { colorFromString });
      }
    }
    return color;
  }

  /**
   *
   *
   * @param coloredString
   */
  public static void printColorIndex(final SortedMap<Integer, AnsiColor[]> ansiColors) {
    for (final Entry<Integer, AnsiColor[]> colorEntry : ansiColors.entrySet()) {
      final int index = colorEntry.getKey();
      final AnsiColor[] colors = colorEntry.getValue();
      System.out.println("\nIndex: " + index);
      System.out.println(St.arrayToString(colors) + "\n");
    }
  }

  /**
   *
   *
   * @param color
   * @return
   */
  public static AnsiColor getFromString(final String color) {
    for (final AnsiColor cl : AnsiColor.values()) {
      if (cl.toString().equals(color)) {
        return cl;
      }
    }
    return DEFAULT;
  }
}
