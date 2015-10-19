/**
 * XOJ
 *
 */
package mj.ocraptor.extraction.tika.parser.xoj.format;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Given the entirety of the Xournal XML, this is intended to generate objects
 * for each page
 *
 * @author droberts
 */
public class PageGenerator {
  /**
   * Xml document
   */
  private Document Xml;

  /**
   * Constructor
   *
   * @param xml
   *          entire DOM document
   */
  public PageGenerator(Document xml) {
    // Store the xml
    Xml = xml;
  }

  /**
   * Create a set of pages from the given document
   */
  public List<Page> paginate() {
    // Get a node of all pages
    NodeList xmlPages = Xml.getElementsByTagName("page");

    // Create a list of actual page objects
    ArrayList<Page> pages = new ArrayList<Page>();

    // Iterate all the xml page nodes
    for (int i = 0; i < xmlPages.getLength(); i++) {
      pages.add(new Page(xmlPages.item(i)));
    }

    return pages;
  }

}
