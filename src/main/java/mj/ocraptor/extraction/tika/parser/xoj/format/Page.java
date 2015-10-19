/**
 * XOJ
 *
 */
package mj.ocraptor.extraction.tika.parser.xoj.format;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import mj.ocraptor.extraction.image_processing.ImageTools;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Contains the content of a single page
 *
 *
 * @author droberts
 */
public class Page {
  /**
   * XML node data for this page
   */
  private Node pagenode;

  /**
   * The <layer> node
   */
  private Node layerNode;

  /**
   * Construct page based on the page node in the XML it is based upon
   *
   * @param pageNode
   *          XML for a single page (<page> node)
   */
  public Page(Node pageNode) {
    // Store the node
    this.pagenode = pageNode;

    // Get all nodes attached
    NodeList children = this.pagenode.getChildNodes();

    // Track down the layer
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i).getNodeName().equalsIgnoreCase("layer")) {
        layerNode = children.item(i);
      }
    }

    if (layerNode == null) {
      throw new RuntimeException("Cannot find layer node in page");
    }
  }

  /**
   *
   *
   * @return
   */
  public List<BufferedImage> getImageFiles() {
    List<BufferedImage> imageFiles = new ArrayList<BufferedImage>();
    NodeList child = this.layerNode.getChildNodes();
    for (int z = 0; z < child.getLength(); z++) {
      if (child.item(z).getNodeName().equalsIgnoreCase("image")) {
        String textValue = child.item(z).getTextContent();
        try {
          BufferedImage image = ImageTools.decodeBase64ToImage(textValue);
          imageFiles.add(image);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return imageFiles;
  }

  /**
   *
   *
   * @return
   */
  public List<String> getTextSnippets() {
    List<String> snippets = new ArrayList<String>();
    NodeList child = this.layerNode.getChildNodes();
    for (int z = 0; z < child.getLength(); z++) {
      if (child.item(z).getNodeName().equalsIgnoreCase("text")) {
        String textValue = child.item(z).getTextContent();
        if (textValue != null && !textValue.trim().isEmpty()) {
          snippets.add(textValue);
        }
      }
    }
    return snippets;
  }

  /**
   * Get a set of all strokes from the page
   */
  public List<Stroke> getStrokes() {
    // Create a list to return of all strokes on page
    ArrayList<Stroke> strokes = new ArrayList<Stroke>();

    // Get all nodes attached to the layer
    NodeList children = layerNode.getChildNodes();

    for (int i = 0; i < children.getLength(); i++) {
      // Stroke data?
      if (children.item(i).getNodeName().equalsIgnoreCase("stroke")) {
        Node stroke = children.item(i);

        // Create and add new stroke
        strokes.add(new Stroke(stroke.getAttributes().getNamedItem("tool").getNodeValue(), stroke
            .getAttributes().getNamedItem("color").getNodeValue(), stroke.getAttributes()
            .getNamedItem("width").getNodeValue(), stroke.getTextContent()));
      }
    }

    return strokes;
  }

}
