/**
 * XOJ
 *
 */
package mj.extraction.tika.parser.xoj.format;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Loads an XOJ file
 *
 * ZIP -> XML -> Parse
 *
 * @author droberts
 */
public class Loader {
  /**
   * Load from a given filename
   *
   * @param filename
   *          filename to load from
   * @throws ParserConfigurationException
   *           parser error (in XML)
   * @throws SAXException
   *           SAX exception (in XML)
   */
  public Document load(InputStream stream) throws ParserConfigurationException, SAXException {
    try {
      // Open up the XOJ (zip)
      GZIPInputStream zip = new GZIPInputStream(stream);

      // Create temp file for extracted content
      File temp = File.createTempFile("extractedxoj", ".tmp");
      temp.deleteOnExit();

      // Open up to write to the temp file
      OutputStream extracted = new FileOutputStream(temp.getAbsolutePath());

      // Transfer bytes from the compressed file to the output file
      byte[] buf = new byte[1024];
      int len;

      while ((len = zip.read(buf)) > 0) {
        extracted.write(buf, 0, len);
      }

      // Kill off gzip
      zip.close();

      // Kill of creation of temp xml
      extracted.close();

      // Create factory et al. to parse the xml
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      // Parse the document
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document xml = builder.parse(temp.getAbsolutePath());

      // Normalize
      xml.getDocumentElement().normalize();

      // Done.
      return xml;
    } catch (IOException e) {
      // Presumably we get this for "Not a gzip"
      //
      throw new RuntimeException("Failed to load from xoj-file does not exist or not a .XOJ? ");
    }
  }

}
