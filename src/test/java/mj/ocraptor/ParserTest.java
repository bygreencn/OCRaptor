package mj.ocraptor;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.database.dao.FileEntry;
import mj.ocraptor.database.search.TextProcessing;
import mj.ocraptor.file_handler.TextExtractorSub;

public class ParserTest {

  public static void test(String test) {
    test = "3";
  }

  /**
   * Let's test some shit!
   *
   * @throws IOException
   */
  public static void main(String[] args) throws Exception {
    String configFilePath = "src/test/resources/default.properties";

    // ------------------------------------------------ //
    String res = "src/test/resources/test-files/";
    String fileToParse = null;
    // fileToParse = res + "huckleberry.gif";
    // fileToParse = res + "miscellaneous/russian_text.txt";
    fileToParse = res + "huckleberry.pdf";
    // fileToParse = "/home/foo/a/crypt/documents/allgemeine_infos.pdf";
    // ------------------------------------------------ //

    // ------------------------------------------------ //
    Config.init(
      false, false, true, false, false, false,
      configFilePath, null, null, null);
    // ------------------------------------------------ //


    // ------------------------------------------------ //
    final TextExtractorSub extractor = new TextExtractorSub();
    FileEntry result = extractor.extractTextTika(new File(fileToParse));
    if (result == null) {
      System.out.println("result is null");
      System.exit(0);
    }
    String original = result.getFullTextString();
    String stripped = TextProcessing.postProcess(original);
    // ------------------------------------------------ //

    System.out.println("=== Stripped text " + StringUtils.repeat("=", 72));
    System.out.println(stripped);
    System.out.println(StringUtils.repeat("=", 90));

    final String encodedXml = TextProcessing.encodePagePositions(original);
    // Map<Integer, Integer> positions = TextProcessing.decodePagePositions(encodedXml);

    // String stringToSearchFor = "know about me without you have";
    // stringToSearchFor = "see no advantage in going";
    // stringToSearchFor = "persons attempting to find";
    // stringToSearchFor = "application";
    // stringToSearchFor = "date";

    // int index = TextProcessing.getPage(positions, stripped.indexOf(stringToSearchFor));
    // System.out.println(index);
  }
}
