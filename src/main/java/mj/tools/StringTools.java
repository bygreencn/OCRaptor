package mj.tools;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mj.configuration.Config;
import mj.console.Platform;
import mj.console.Platform.Os;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

/**
 *
 *
 * @author Michael
 */
public class StringTools {

  private static SecureRandom random = new SecureRandom();

  // @formatter:off
  private static final String
    UMLAUT_UE_LOW       = "ü"   ,
    UMLAUT_UE_LOW_NOR   = "u¨"  ,
    UMLAUT_UE_LOW_NOR2  = "ue"  ,
    UMLAUT_UE           = "Ü"   ,
    UMLAUT_UE_NOR       = "U¨"  ,
    UMLAUT_UE_NOR2      = "Ue"  ,
    UMLAUT_AE_LOW       = "ä"   ,
    UMLAUT_AE_LOW_NOR   = "a¨"  ,
    UMLAUT_AE_LOW_NOR2  = "ae"  ,
    UMLAUT_AE           = "Ä"   ,
    UMLAUT_AE_NOR       = "A¨"  ,
    UMLAUT_AE_NOR2      = "Ae"  ,
    UMLAUT_OE_LOW       = "ö"   ,
    UMLAUT_OE_LOW_NOR   = "o¨"  ,
    UMLAUT_OE_LOW_NOR2  = "oe"  ,
    UMLAUT_OE           = "Ö"   ,
    UMLAUT_OE_NOR       = "O¨"  ,
    UMLAUT_OE_NOR2      = "Oe"
  ;
  // @formatter:on

  /**
   *
   *
   * @param text
   * @return
   */
  public static String replaceUmlaute(String text, boolean ascii, boolean reverse) {
    // @formatter:off
    final String[] umlaute = new String[] {
      UMLAUT_UE_LOW,
      UMLAUT_UE,
      UMLAUT_AE_LOW,
      UMLAUT_AE,
      UMLAUT_OE_LOW,
      UMLAUT_OE
    };
    final String[] umlauteNormalized = new String[] {
      UMLAUT_UE_LOW_NOR,
      UMLAUT_UE_NOR,
      UMLAUT_AE_LOW_NOR,
      UMLAUT_AE_NOR,
      UMLAUT_OE_LOW_NOR,
      UMLAUT_OE_NOR
    };
    final String[] umlauteAscii = new String[] {
      UMLAUT_UE_LOW_NOR2,
      UMLAUT_UE_NOR2,
      UMLAUT_AE_LOW_NOR2,
      UMLAUT_AE_NOR2,
      UMLAUT_OE_LOW_NOR2,
      UMLAUT_OE_NOR2
    };
    // @formatter:on

    if (reverse) {
      if (!ascii) {
        text = StringUtils.replaceEach(text, umlauteNormalized, umlaute);
      }
    } else {
      if (ascii) {
        text = StringUtils.replaceEach(text, umlauteNormalized, umlauteAscii);
        text = StringUtils.replaceEach(text, umlaute, umlauteAscii);
      } else {
        text = StringUtils.replaceEach(text, umlaute, umlauteNormalized);
      }
    }
    return text;
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  // @formatter:off
  private static final String PLAIN_ASCII
      = "AaEeIiOoUu" // grave
      + "AaEeIiOoUuYy" // acute
      + "AaEeIiOoUuYy" // circumflex
      + "AaOoNn" // tilde
      + "AaEeIiOoUuYy" // umlaut
      + "Aa" // ring
      + "Cc" // cedilla
      + "OoUu" // double acute
  ;

  private static final String UNICODE
      = "\u00C0\u00E0\u00C8\u00E8\u00CC\u00EC\u00D2\u00F2\u00D9\u00F9"
      + "\u00C1\u00E1\u00C9\u00E9\u00CD\u00ED\u00D3\u00F3\u00DA\u00FA"
      + "\u00DD\u00FD\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4"
      + "\u00DB\u00FB\u0176\u0177\u00C3\u00E3\u00D5\u00F5\u00D1\u00F1"
      + "\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC"
      + "\u0178\u00FF\u00C5\u00E5\u00C7\u00E7\u0150\u0151\u0170\u0171"
  ;
  // @formatter:on

  /**
   * Remove accentued from a string and replace with ascii equivalent.
   *
   * @param s
   * @return
   */
  public static String convertNonAscii(String s) {
    if (s == null)
      return null;
    StringBuilder sb = new StringBuilder();
    int n = s.length();
    for (int i = 0; i < n; i++) {
      char c = s.charAt(i);
      int pos = UNICODE.indexOf(c);
      if (pos > -1) {
        sb.append(PLAIN_ASCII.charAt(pos));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  // ------------------------------------------------ //
  // --
  // ------------------------------------------------ //

  /**
   *
   *
   * @param documentText
   * @return
   */
  public static String removeAllNonAsciiCharacters(String documentText) {
    return documentText.replaceAll("[^\\x00-\\x7F]", "");
  }

  /**
   *
   *
   * @param documentText
   * @return
   */
  public static String normalizeDocumentText(String documentText) {
    documentText = StringTools.replaceUmlaute(documentText, true, false);
    documentText = convertNonAscii(documentText);
    documentText = StringUtils.remove(documentText, "\n");
    documentText = documentText.replaceAll("\\xA0", " ").trim();
    return documentText;
  }

  /**
   *
   *
   * @param textWithDelimiters
   * @return
   */
  public static String removeSearchDelimiter(String textWithDelimiters) {
    textWithDelimiters = StringUtils.remove(textWithDelimiters, Config.SEARCH_DELIMITER_START);
    textWithDelimiters = StringUtils.remove(textWithDelimiters, Config.SEARCH_DELIMITER_END);

    while (textWithDelimiters.endsWith(Config.SEARCH_DELIMITER_END_SINGLE)) {
      textWithDelimiters = StringUtils.removeEnd(textWithDelimiters,
          Config.SEARCH_DELIMITER_END_SINGLE);
    }
    while (textWithDelimiters.startsWith(Config.SEARCH_DELIMITER_END_SINGLE)) {
      textWithDelimiters = StringUtils.removeStart(textWithDelimiters,
          Config.SEARCH_DELIMITER_END_SINGLE);
    }
    return textWithDelimiters;
  }

  /**
   *
   *
   * @param stringBuffer
   * @param places
   * @return
   */
  public static StringBuffer removeLastCharacters(StringBuffer stringBuffer, int places) {
    return stringBuffer.replace(stringBuffer.length() - places, stringBuffer.length(), "");
  }

  /**
   *
   *
   * @param string
   * @param places
   * @return
   */
  public static String removeLastCharacters(String string, int places) {
    return string.substring(0, string.length() - places);
  }

  /**
   *
   *
   * @param string
   * @param places
   * @return
   */
  public static String removeFirstCharacters(String string, int places) {
    return string.substring(places, string.length());
  }

  /**
   *
   *
   * @param stringBuffer
   * @param places
   * @return
   */
  public static StringBuffer removeFirstCharacters(StringBuffer stringBuffer, int places) {
    return stringBuffer.replace(0, places, "");
  }

  /**
   *
   *
   * @param string
   * @return
   */
  public static String removeLastLineBreak(String string) {
    if (string.endsWith("\n"))
      return removeLastCharacters(string, 1);
    return string;
  }

  /**
   *
   *
   * @param text
   * @return
   */
  public static String removeAllNonWordCharacters(String text) {
    return text.replaceAll("[^\\p{L}\\p{Nd}]+", " ").replaceAll("\\s+", " ");
  }

  /**
   *
   *
   * @param text
   * @return
   */
  // TODO:
  public static String removeRareCharacters(String text) {
    return text.replaceAll("\n+", "|").replaceAll("\\|\\s+(?=[^|]*\\|)", "")
        .replaceAll("\\|+", " | ").replace("“", "\"").replace("'", "\"").replaceAll(
            "[^|^;^:^!^?^-^\"^\\.^,^\\p{L}\\p{Nd}]+", " ").replaceAll("\\s+", " ").replaceAll(
            "\\.+", ".").replaceAll("\\,+", ",").replaceAll("\\?+", "?").replaceAll("\\!+", "!")
        .replaceAll("\"+", "\"").trim();
  }

  /**
   *
   *
   * @return
   */
  public static String nextSessionId() {
    return new BigInteger(130, random).toString(32);
  }

  /**
   *
   *
   * @return
   */
  public static String generatePassword(int numBits, int length) {
    return new BigInteger(numBits, random).toString(length);
  }

  /**
   *
   *
   * @param par
   * @return
   */
  public static boolean parseBoolean(String par) {
    if (par != null && par.trim().equalsIgnoreCase("true")) {
      return true;
    }
    return false;
  }

  /**
   *
   *
   * @param par
   * @return
   */
  public static Integer parseInteger(String par) {
    Integer integer = null;
    if (par != null) {
      try {
        integer = Integer.parseInt(par.trim());
      } catch (NumberFormatException e) {
      }
    }
    return integer;
  }

  /**
   *
   *
   * @return
   */
  public static String stripHtmlTags(String text) {
    if (text != null) {
      // strip html tags
      text = text.replaceAll("<[^>]*>", "");
    }
    return text;
  }

  /**
   *
   *
   * @param text
   * @param leftIndex
   * @return
   */
  public static boolean trailingCharacter(String text, int index, boolean left) {
    boolean hasValidTrailingCharacter = false;

    if ((left && index == 0) || (!left && index == text.length()))
      return true;

    while ((left && index != 0) || (!left && index != text.length())) {
      Character c = left ? text.charAt(--index) : text.charAt(index++);
      if (!Character.isWhitespace(c)) {
        if (c == '.')
          return left ? false : true;
        if (c == ';' || c == ',' || c == '`' || c == ')' || c == '(')
          hasValidTrailingCharacter = true;
        index = left ? 0 : text.length();
      } else {
        hasValidTrailingCharacter = true;
      }
    }

    if (hasValidTrailingCharacter)
      return true;
    return false;
  }

  /**
   *
   *
   * @return
   */
  public static String stripHtmlTagsForQuestion(String text) {
    if (text != null) {
      text = text.replaceAll("(?i)<script.*?</script>", "");
      text = text.replaceAll("(?i)<javascript.*?</javascript>", "");
      text = text.replaceAll("(?i)<style.*?</style>", "");

      String REGEX_FIELD = "<[^>]*>";
      Matcher matcher = Pattern.compile(REGEX_FIELD, Pattern.CASE_INSENSITIVE).matcher(text);
      while (matcher.find()) {
        String snippet = matcher.group();
        String snippetNoSpaces = snippet.replaceAll("[\\s\"\'0-9]", "");

        if (!snippetNoSpaces.equals("<br>") && !snippetNoSpaces.equals("<br/>")
            && !snippetNoSpaces.equals("</span>") && !snippetNoSpaces.equals("<span>")) {

          if (snippetNoSpaces.startsWith("<span")) {
            if (!snippetNoSpaces.equals("<spanstyle=font-style:italic;>")
                && !snippetNoSpaces.equals("<spanstyle=text-decoration:underline;>")
                && !snippetNoSpaces.equals("<spanstyle=text-decoration:line-through;>")
                && !snippetNoSpaces.equals("<spanstyle=color:rgb(,,);>")) {
              text = text.replace(snippet, "<span>");
            }
          } else {
            text = text.replaceFirst(snippet, "");
          }
        }
      }
    }
    return text;
  }

  /**
   *
   *
   * @return
   */
  public static String stripHtmlTagsForScenario(String text) {
    if (text != null) {
      text = text.replaceAll("(?i)<script.*?</script>", "");
      text = text.replaceAll("(?i)<javascript.*?</javascript>", "");
      text = text.replaceAll("(?i)<style.*?</style>", "");

      String REGEX_FIELD = "<[^>]*>";
      Matcher matcher = Pattern.compile(REGEX_FIELD, Pattern.CASE_INSENSITIVE).matcher(text);
      while (matcher.find()) {
        String snippet = matcher.group();
        String snippetNoSpaces = snippet.toLowerCase().replaceAll("[\\s\"\'0-9]", "");

        if (!snippetNoSpaces.equals("<br>") && !snippetNoSpaces.equals("<br/>")
            && !snippetNoSpaces.equals("</span>") && !snippetNoSpaces.equals("<span>")
            && !snippetNoSpaces.equals("</font>") && !snippetNoSpaces.equals("<font>")) {

          if (snippetNoSpaces.startsWith("<span")) {
            if (!snippetNoSpaces.equals("<spanstyle=font-style:italic;>")
                && !snippetNoSpaces.equals("<spanstyle=text-decoration:underline;>")
                && !snippetNoSpaces.equals("<spanstyle=text-decoration:line-through;>")
                && !snippetNoSpaces.equals("<spanstyle=color:rgb(,,);>")
                && !snippetNoSpaces.equals("<spanstyle=font-weight:bold;>")) {
              text = text.replace(snippet, "<span>");
            }
          } else if (snippetNoSpaces.startsWith("<font")) {
            if (!snippetNoSpaces.equals("<fontsize=>")) {
              text = text.replace(snippet, "<font>");
            }
          } else {
            text = text.replaceFirst(snippet, "");
          }
        }
      }
    }
    return text;
  }

  /**
   *
   *
   * @param path
   * @return
   */
  public static String normalizeFileName(String path) {
    String newName = FilenameUtils.removeExtension(path).toLowerCase();
    return normalize(newName);
  }

  /**
   *
   *
   * @param path
   * @return
   */
  public static String deleteDate(String path) {
    if (path != null) {
      return path.replaceAll("[0-9]{4}_[0-9]{2}_[0-9]{2}-[0-9]{2}_[0-9]{2}_[0-9]{2}", "");
    }
    return null;
  }

  /**
   *
   *
   * @param string
   * @param length
   * @return
   */
  public static String trimToLengthIndicatorRight(String s, int length) {
    if (s != null) {
      if (length < 5) {
        return "[...]";
      }
      if (s.length() > 5 && s.length() > length) {
        return s.substring(0, length - 5) + "[...]";
      } else {
        return s;
      }
    }
    return null;
  }

  /**
   *
   *
   * @param s
   * @param length
   * @return
   */
  public static String trimToLengthIndicatorLeft(String s, int length) {
    if (s != null) {
      if (length < 5) {
        return "[...]";
      }
      if (s.length() > 5 && s.length() > length) {
        return "[...]" + s.substring(s.length() - (length - 5), s.length());
      } else {
        return s;
      }
    }
    return null;
  }

  /**
   *
   *
   * @param s
   * @param length
   * @return
   */
  public static String trimToLength(String s, int length) {
    if (s != null) {
      if (s.length() > length) {
        return s.substring(0, length);
      } else {
        return s;
      }
    }
    return null;
  }

  /**
   *
   *
   * @param line
   * @param lineBreakWidth
   * @return
   */
  public static String addLineBreaks(String line, int lineBreakWidth) {
    return addLineBreaks(line, "\n", lineBreakWidth);
  }

  /**
   *
   *
   * @param line
   * @param lineBreakWidth
   * @return
   */
  public static String addLineBreaks(String line, String breaker, int lineBreakWidth) {
    StringBuffer output = new StringBuffer();
    int countLineBreaks = 1;

    int indexToInsertLineBreak = 0;
    while (countLineBreaks * lineBreakWidth < line.length()) {
      indexToInsertLineBreak = countLineBreaks++ * lineBreakWidth;
      output.append(line.substring(indexToInsertLineBreak - lineBreakWidth, indexToInsertLineBreak)
          + breaker);
    }
    output.append(line.substring(indexToInsertLineBreak, line.length()));

    return output.toString();
  }

  /**
   *
   *
   * @param stringToSearch
   * @param stringToFind
   * @param indicatorString
   * @param index
   * @param length
   * @return
   */
  public static String[] findSn(String stringToSearch, String stringToFind, String indicatorString,
      int index, int length) {
    String[] subStrings = new String[2];

    // if (stringToSearch.length() < length) {
    // return subStrings;
    // }

    StringBuffer outputLeft = new StringBuffer();
    StringBuffer outputRight = new StringBuffer();

    char[] charsRight = stringToSearch.substring(index + stringToFind.length()).toCharArray();
    char[] charsLeft = stringToSearch.substring(0, index).toCharArray();

    int leftIndex = 1;
    int rightIndex = 0;

    for (int i = 0; i < length - stringToFind.length();) {
      int count = 0;
      if (charsLeft.length >= leftIndex) {
        outputLeft.insert(0, charsLeft[charsLeft.length - leftIndex++]);
        count++;
      }
      if (charsRight.length > rightIndex) {
        outputRight.append(charsRight[rightIndex++]);
        count++;
      }
      i += count > 0 ? count : 1;
    }

    int indicatorLength = indicatorString.length();

    if (charsLeft.length >= leftIndex) {
      outputLeft = outputLeft.replace(0, indicatorLength, "");

      while (outputLeft.toString().startsWith(Config.SEARCH_DELIMITER_START_SINGLE)
          || outputLeft.toString().startsWith(Config.SEARCH_DELIMITER_END_SINGLE)) {
        outputLeft = removeFirstCharacters(outputLeft, 1);
      }

      int end = outputLeft.indexOf(Config.SEARCH_DELIMITER_END);
      int start = outputLeft.indexOf(Config.SEARCH_DELIMITER_START);

      if (end != -1 && start != -1 && end < start) {
        outputLeft = outputLeft.insert(0, Config.SEARCH_DELIMITER_START);
      }

      outputLeft.insert(0, indicatorString);
    }

    if (charsRight.length > rightIndex) {
      outputRight = outputRight.replace(outputRight.length() - indicatorLength, outputRight
          .length(), "");

      while (outputRight.toString().endsWith(Config.SEARCH_DELIMITER_END_SINGLE)
          || outputRight.toString().endsWith(Config.SEARCH_DELIMITER_START_SINGLE)) {
        outputRight = removeLastCharacters(outputRight, 1);
      }

      int end = outputRight.lastIndexOf(Config.SEARCH_DELIMITER_END);
      int start = outputRight.lastIndexOf(Config.SEARCH_DELIMITER_START);

      if (end != -1 && start != -1 && end < start) {
        outputRight = outputRight.append(Config.SEARCH_DELIMITER_END);
      }

      outputRight.append(indicatorString);
    }

    subStrings[0] = outputLeft.toString();
    subStrings[1] = outputRight.toString();

    return subStrings;
  }

  /**
   *
   *
   * @param stringToSearch
   * @param stringToFind
   * @param length
   * @return
   */
  public static String findSnippet(String stringToSearch, String stringToFind, int length) {
    return findSnippet(stringToSearch, stringToFind, "[...]", 0, length);
  }

  /**
   *
   *
   * @param stringToSearch
   * @param length
   * @return
   */
  public static String findSnippet(String stringToSearch, String stringToFind,
      String indicatorString, int firstIndex, int length) {
    try {
      int stringToSearchLength = stringToSearch.length();
      int stringToFindLength = stringToFind.length();
      int newLeftIndex = firstIndex - length;
      int newRightIndex = firstIndex + stringToFindLength + length;
      int indicatorLength = indicatorString.length();

      if (stringToSearchLength < length) {
        return stringToSearch;
      }

      String temp = stringToSearch.substring(firstIndex, firstIndex + stringToFindLength);

      if (newLeftIndex > 0 && newRightIndex < stringToSearchLength - 1) {
        stringToSearch = stringToSearch.substring(newLeftIndex, newRightIndex);
      }

      if (newLeftIndex <= 0 && newRightIndex + length < stringToSearchLength - 1) {
        stringToSearch = stringToSearch.substring(0, newRightIndex + length);
      }

      if (newLeftIndex - length > 0 && newRightIndex >= stringToSearchLength - 1) {
        stringToSearch = stringToSearch.substring(newLeftIndex - length, stringToSearchLength);
      }

      stringToFind = stringToFind.trim().toLowerCase();
      stringToSearch = stringToSearch.trim();

      if (!stringToSearch.toLowerCase().contains(stringToFind)) {
        return trimToLengthIndicatorRight(stringToSearch, length);
      }

      int mainLength = stringToSearch.length();
      int snippetLength = stringToFind.length();

      if (length > indicatorLength && snippetLength < length) {
        // int index =
        // stringToSearch.toLowerCase().indexOf(stringToFind.toLowerCase());
        int index = firstIndex;

        if (snippetLength < length && snippetLength + (indicatorLength * 2) > length) {
          stringToFind = temp;
          if (snippetLength >= length - indicatorLength) {
            return stringToFind.substring(0, length - indicatorLength) + indicatorString;
          } else {
            return stringToFind + indicatorString;
          }
        }

        boolean leftTrim = false, rightTrim = false;
        boolean trimRight = false;
        int spaceLeft = 0, spaceRight = 0;

        spaceLeft = index;
        spaceRight = mainLength - (spaceLeft + snippetLength);

        while ((mainLength > length - (indicatorLength * 2))
            || (mainLength > length - indicatorLength && index == 0)) {
          if (trimRight || index == 0) {
            if (spaceRight > 0) {
              stringToSearch = stringToSearch.substring(0, stringToSearch.length() - 1);
              rightTrim = true;
            }
            if (spaceLeft > spaceRight || spaceRight == 0) {
              trimRight = false;
            }
          } else {
            if (spaceLeft > 0) {
              stringToSearch = stringToSearch.substring(1, stringToSearch.length());
              leftTrim = true;
            }
            if (spaceLeft <= spaceRight || spaceLeft == 0) {
              trimRight = true;
            }
          }

          mainLength = stringToSearch.length();
          spaceLeft = stringToSearch.toLowerCase().indexOf(stringToFind.toLowerCase());
          spaceRight = mainLength - (spaceLeft + snippetLength);
        }

        if (rightTrim) {
          stringToSearch += indicatorString;
        }
        if (leftTrim) {
          stringToSearch = indicatorString + stringToSearch;
        }

        return stringToSearch;
      } else {
        return trimToLengthIndicatorRight(stringToSearch, length);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return stringToSearch;
    }
  }

  /**
   *
   *
   * @param query
   * @return
   */
  public static String extractIDFromAutoComplete(String query) {
    if (query != null) {
      String[] temp = query.split("]:");
      if (temp.length == 2) {
        return temp[0].replace("[", "").trim();
      }
    }
    return null;
  }

  /**
   *
   *
   * @param path
   * @return
   */
  public static String normalize(String path) {
    if (path != null) {
      try {
        String newName = path.toLowerCase();
        // delete date-field
        newName = newName.replaceAll("[ü}]", "ue").replaceAll("[ä]", "ae").replaceAll("[ö]", "oe")
            .replaceAll("[ß]", "ss").replaceAll("[^a-z0-9&-]", "_").replaceAll("[_]{2,}", "_");

        while (newName.endsWith("_")) {
          newName = newName.substring(0, newName.length() - 1);
        }
        while (newName.startsWith("_")) {
          newName = newName.substring(1, newName.length());
        }
        return newName;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return path;
  }

  /**
   *
   *
   * @param number
   * @param width
   * @return
   */
  public static String zeroPad(int number, int width) {
    if (number < 100) {
      int wrapAt = (int) Math.pow(10, width);
      return String.valueOf(number % wrapAt + wrapAt).substring(1);
    } else {
      return String.valueOf(number);
    }
  }

  /**
   *
   *
   * @param number
   * @param width
   * @return
   */
  public static String zeroPadSpaces(int number, int width) {
    return zeroPadSpaces(number, width, false);
  }

  /**
   *
   *
   * @param number
   * @param width
   * @return
   */
  public static String zeroPadSpaces(int number, int width, boolean onEnd) {
    String zeroString = String.valueOf(number);
    if (zeroString.length() < width) {
      if (!onEnd) {
        zeroString = StringUtils.repeat(" ", width - zeroString.length()) + zeroString;
      } else {
        zeroString = zeroString + StringUtils.repeat(" ", width - zeroString.length());
      }
    }
    return zeroString;
  }

  /**
   *
   *
   * @param string
   * @param limit
   * @return
   */
  public static String breakToSpace(String line, String startLineWith, int startLineBreakWidth,
      int lineBreakWidth) {
    StringBuffer output = new StringBuffer();
    StringTokenizer defaultTokenizer = new StringTokenizer(line);
    int lineWidth = 0;
    boolean firstLine = true;
    int currentLineMaxWidth = startLineBreakWidth;

    while (defaultTokenizer.hasMoreTokens()) {
      String currentWord = defaultTokenizer.nextToken();

      if (lineWidth + currentWord.length() >= currentLineMaxWidth) {
        output.append("\n" + startLineWith);
        lineWidth = 0;
        if (firstLine) {
          currentLineMaxWidth = lineBreakWidth;
          firstLine = false;
        }
      } else {
        output.append(" ");
        lineWidth++;
      }

      output.append(currentWord);
      lineWidth += currentWord.length();
    }

    return output.toString();
  }

  /**
   *
   *
   * @param d
   * @return
   */
  public static String formatDouble(double d, int places) {
    DecimalFormat f = new DecimalFormat("#0." + StringUtils.repeat("0", places));
    return f.format(d);
  }

  private static final char[] ILLEGAL_FILE_NAME_CHARACTERS = { //
  '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };

  /**
   *
   *
   * @return
   */
  public static boolean isValidFileName(String name, int maxLength) {
    if (name == null || name.trim().isEmpty()) {
      return false;
    }

    if (name.trim().length() > maxLength) {
      return false;
    }

    for (char ch : ILLEGAL_FILE_NAME_CHARACTERS) {
      if (name.contains(String.valueOf(ch))) {
        return false;
      }
    }
    return true;
  }

  /**
   *
   *
   * @param path
   * @return
   */
  // public static String normalizeText(String path) {
  // if (path != null) {
  // try {
  // path = path.replaceAll("[ü}]", "ue").replaceAll("[ä]",
  // "ae").replaceAll("[ö]", "oe")
  // .replaceAll("[ß]", "ss").replaceAll("\n+",
  // "\n").replaceAll("\\b(?!\n\\b)\\s+\\b", " ");
  // return path;
  // } catch (Exception e) {
  // e.printStackTrace();
  // }
  // }
  // return path;
  // }

  /**
   *
   *
   * @param path
   * @return
   */
  // public static String normalizeTextNoLineBreak(String path) {
  // if (path != null) {
  // try {
  // path = path.replaceAll("[ü}]", "ue").replaceAll("[ä]",
  // "ae").replaceAll("[ö]", "oe")
  // .replaceAll("[ß]", "ss").replaceAll("\\s+", " ");
  // return path;
  // } catch (Exception e) {
  // e.printStackTrace();
  // }
  // }
  // return path;
  // }

  /**
   *
   */
  public static int getSubstringOccurrence(String stringToSearchIn, String stringToSearchFor) {
    stringToSearchIn = stringToSearchIn.toLowerCase();
    stringToSearchFor = stringToSearchFor.toLowerCase();

    int lastIndex = 0;
    int count = 0;
    try {
      while (lastIndex != -1) {
        lastIndex = stringToSearchIn.indexOf(stringToSearchFor, lastIndex);
        if (lastIndex != -1) {
          count++;
          lastIndex += stringToSearchFor.length();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return count;
  }

  /**
   *
   *
   * @param file
   * @return
   */
  public static String getFileNameWithoutExtension(File file) {
    if (file != null && file.isFile()) {
      return FilenameUtils.getBaseName(file.getName());
    }
    return null;
  }

  /**
   *
   *
   * @param fileName
   * @return
   */
  public static String getFileNameWithoutExtension(String fileName) {
    return FilenameUtils.getBaseName(fileName);
  }

  /**
   *
   *
   * @param path
   * @return
   */
  public static String shortenHomePathInDirectory(String path) {
    if (path.startsWith(SystemUtils.USER_HOME)
        && (Platform.getSystem() == Os.LINUX || Platform.getSystem() == Os.OSX)) {
      path = path.replaceFirst(SystemUtils.USER_HOME, "~");
    }
    return path;
  }

  /**
   *
   *
   * @param path
   * @return
   */
  public static String resolveFilePath(String path) {
    if (path.startsWith("~")
        && (Platform.getSystem() == Os.LINUX || Platform.getSystem() == Os.OSX)) {
      path = path.replaceFirst("~", SystemUtils.USER_HOME);
    }
    return path;
  }

  /**
   *
   *
   * @param file
   * @return
   */
  public static String getFileExtension(File file) {
    if (file != null && file.isFile()) {
      String name = file.getName();
      int lastIndex = name.lastIndexOf(".");
      if (lastIndex != -1 && lastIndex != name.length() - 1) {
        return name.substring(lastIndex + 1, name.length());
      }
    }
    return null;
  }

  /**
   *
   *
   * @param array
   * @return
   */
  public static String arrayToString(Object[] array) {
    if (array != null) {
      StringBuffer arrayString = new StringBuffer();
      for (int i = 0; i < array.length; i++) {
        arrayString.append(zeroPad(i, 3) + " -- " + array[i].toString() + "\n");
      }
      return removeLastLineBreak(arrayString.toString());
    }
    return null;
  }
}
