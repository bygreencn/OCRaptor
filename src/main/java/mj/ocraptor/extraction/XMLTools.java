package mj.ocraptor.extraction;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class XMLTools {

  /**
   *
   *
   * @param doc
   * @param out
   *
   * @throws IOException
   * @throws TransformerException
   */
  public static void printDocument(Document doc, OutputStream out) throws IOException,
      TransformerException {
    // printDocument(w3c.doc, new FileOutputStream(new
    // File("/home/foo/a/git2/xoj2pdf.xml")));
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer = tf.newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

    transformer.transform(new DOMSource(doc),
        new StreamResult(new OutputStreamWriter(out, "UTF-8")));
  }

}
