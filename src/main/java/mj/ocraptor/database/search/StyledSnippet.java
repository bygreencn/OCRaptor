package mj.ocraptor.database.search;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import mj.ocraptor.tools.Tp;

/**
 *
 *
 * @author
 */
public class StyledSnippet {
  private List<Tp<String, StyledSnippetType>> snippets;

  /**
   * {@inheritDoc}
   * @see Object#StyledSnippet()
   */
  public StyledSnippet() {
    this.snippets = new ArrayList<Tp<String, StyledSnippetType>>();
  }

  /**
   *
   *
   * @param snippet
   * @param type
   * @return
   */
  public StyledSnippet add(final String snippet, final StyledSnippetType type) {
    final String[] lines = snippet.split(Pattern.quote(TextProcessing.OCR_IMAGE_BREAKLINE));
    if (snippet.startsWith(TextProcessing.OCR_IMAGE_BREAKLINE)) {
      this.snippets.add(new Tp<String, StyledSnippetType>(TextProcessing.OCR_IMAGE_BREAKLINE,
          StyledSnippetType.LINE_SEPERATOR));
    }
    for (int i = 0; i < lines.length; i++) {
      this.snippets.add(new Tp<String, StyledSnippetType>(lines[i], type));
      if (i < lines.length - 1) {
        this.snippets.add(new Tp<String, StyledSnippetType>(TextProcessing.OCR_IMAGE_BREAKLINE,
            StyledSnippetType.LINE_SEPERATOR));
      }
    }
    if (snippet.endsWith(TextProcessing.OCR_IMAGE_BREAKLINE)) {
      this.snippets.add(new Tp<String, StyledSnippetType>(TextProcessing.OCR_IMAGE_BREAKLINE,
          StyledSnippetType.LINE_SEPERATOR));
    }
    return this;
  }

  /**
   * @return the snippets
   */
  public List<Tp<String, StyledSnippetType>> getSnippets() {
    return snippets;
  }

  /**
   *
   *
   * @return
   */
  public int getLength() {
    int length = 0;
    for (final Tp<String, StyledSnippetType> snippet : snippets) {
      length += snippet.getKey().replace("\n", "").length();
    }
    return length;
  }

  /**
   *
   *
   * @return
   */
  public int getAdjustedLength() {
    int length = 0;
    for (final Tp<String, StyledSnippetType> snippet : snippets) {
      length += snippet.getKey().length();
    }
    return length;
  }
}
