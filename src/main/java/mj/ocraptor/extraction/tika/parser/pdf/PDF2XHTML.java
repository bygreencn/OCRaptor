/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mj.ocraptor.extraction.tika.parser.pdf;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import mj.ocraptor.configuration.Config;
import mj.ocraptor.configuration.properties.ConfigBool;
import mj.ocraptor.extraction.image_processing.ImageTools;
import mj.ocraptor.extraction.image_processing.TikaImageHelper;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectForm;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.apache.tika.io.IOExceptionWithCause;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.EmbeddedContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Utility class that overrides the {@link PDFTextStripper} functionality to
 * produce a semi-structured XHTML SAX events instead of a plain text stream.
 */
class PDF2XHTML extends PDFTextStripper {

  private final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(getClass());

  /**
   * format used for signature dates
   */
  private final SimpleDateFormat dateFormat = new SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ssZ");

  /**
   * Maximum recursive depth during AcroForm processing. Prevents theoretical
   * AcroForm recursion bomb.
   */
  private final static int MAX_ACROFORM_RECURSIONS = 10;

  // TODO: remove once PDFBOX-1130 is fixed:
  private boolean inParagraph = false;

  private Metadata metadata;

  /**
   * Converts the given PDF document (and related metadata) to a stream of XHTML
   * SAX events sent to the given content handler.
   *
   * @param document
   *          PDF document
   * @param handler
   *          SAX content handler
   * @param metadata
   *          PDF metadata
   * @throws SAXException
   *           if the content handler fails to process SAX events
   * @throws TikaException
   *           if the PDF document can not be processed
   */
  public static void process(PDDocument document, ContentHandler handler,
                             ParseContext context, Metadata metadata, PDFParserConfig config)
  throws SAXException, TikaException {
    try {
      // Extract text using a dummy Writer as we override the
      // key methods to output to the given content
      // handler.
      PDF2XHTML pdf2XHTML = new PDF2XHTML(handler, context, metadata, config);

      pdf2XHTML.writeText(document, new Writer() {
        @Override
        public void write(char[] cbuf, int off, int len) {
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
      });

    } catch (IOException e) {
      if (e.getCause() instanceof SAXException) {
        throw (SAXException) e.getCause();
      } else {
        throw new TikaException("Unable to extract PDF content", e);
      }
    }
  }

  private final ContentHandler originalHandler;
  private final ParseContext context;
  private final XHTMLContentHandler handler;
  private final PDFParserConfig config;
  private final Config cfg;

  private PDF2XHTML(ContentHandler handler, ParseContext context,
                    Metadata metadata, PDFParserConfig config) throws IOException {
    // source of config (derives from context or PDFParser?) is
    // already determined in PDFParser. No need to check context here.

    this.config = config;
    this.originalHandler = handler;
    this.context = context;
    this.handler = new XHTMLContentHandler(handler, metadata);
    this.cfg = Config.inst();
    this.metadata = metadata;

    setForceParsing(true);
    setSortByPosition(config.getSortByPosition());
    if (config.getEnableAutoSpace()) {
      setWordSeparator(" ");
    } else {
      setWordSeparator("");
    }
    // TODO: maybe expose setting these too:
    // setAverageCharTolerance(1.0f);
    // setSpacingTolerance(1.0f);
    setSuppressDuplicateOverlappingText(config
                                        .getSuppressDuplicateOverlappingText());
  }

  void extractBookmarkText() throws SAXException {
    PDDocumentOutline outline = document.getDocumentCatalog()
                                .getDocumentOutline();
    if (outline != null) {
      extractBookmarkText(outline);
    }
  }

  void extractBookmarkText(PDOutlineNode bookmark) throws SAXException {
    PDOutlineItem current = bookmark.getFirstChild();
    if (current != null) {
      handler.startElement("ul");
      while (current != null) {
        handler.startElement("li");
        handler.characters(current.getTitle());
        handler.endElement("li");
        // Recurse:
        extractBookmarkText(current);
        current = current.getNextSibling();
      }
      handler.endElement("ul");
    }
  }

  @Override
  protected void startDocument(PDDocument pdf) throws IOException {
    try {
      handler.startDocument();
    } catch (SAXException e) {
      throw new IOExceptionWithCause("Unable to start a document", e);
    }
  }

  @Override
  protected void endDocument(PDDocument pdf) throws IOException {
    try {
      // Extract text for any bookmarks:
      extractBookmarkText();

      if (this.cfg.getProp(ConfigBool.ENABLE_IMAGE_OCR)) {
        extractImageText(pdf);
      }
      extractEmbeddedDocuments(pdf, originalHandler);

      // extract acroform data at end of doc
      if (config.getExtractAcroFormContent() == true) {
        extractAcroForm(pdf, handler);
      }

      handler.endDocument();
    } catch (TikaException e) {
      throw new IOExceptionWithCause("Unable to end a document", e);
    } catch (SAXException e) {
      throw new IOExceptionWithCause("Unable to end a document", e);
    }
  }

  @Override
  protected void startPage(PDPage page) throws IOException {
    try {
      handler.startElement("div", "class", "page");
    } catch (SAXException e) {
      throw new IOExceptionWithCause("Unable to start a page", e);
    }
    writeParagraphStart();
  }

  @Override
  protected void endPage(PDPage page) throws IOException {
    try {
      writeParagraphEnd();
      // TODO: remove once PDFBOX-1143 is fixed:
      if (config.getExtractAnnotationText()) {
        for (Object o : page.getAnnotations()) {
          if (o instanceof PDAnnotationLink) {
            PDAnnotationLink annotationlink = (PDAnnotationLink) o;
            if (annotationlink.getAction() != null) {
              PDAction action = annotationlink.getAction();
              if (action instanceof PDActionURI) {
                PDActionURI uri = (PDActionURI) action;
                String link = uri.getURI();
                if (link != null) {
                  handler.startElement("div", "class", "annotation");
                  handler.startElement("a", "href", link);
                  handler.endElement("a");
                  handler.endElement("div");
                }
              }
            }
          }

          if (o instanceof PDAnnotationMarkup) {
            PDAnnotationMarkup annot = (PDAnnotationMarkup) o;
            String title = annot.getTitlePopup();
            String subject = annot.getSubject();
            String contents = annot.getContents();
            // TODO: maybe also annot.getRichContents()?
            if (title != null || subject != null || contents != null) {
              handler.startElement("div", "class", "annotation");

              if (title != null) {
                handler.startElement("div", "class", "annotationTitle");
                handler.characters(title);
                handler.endElement("div");
              }

              if (subject != null) {
                handler.startElement("div", "class", "annotationSubject");
                handler.characters(subject);
                handler.endElement("div");
              }

              if (contents != null) {
                handler.startElement("div", "class", "annotationContents");
                handler.characters(contents);
                handler.endElement("div");
              }

              handler.endElement("div");
            }
          }
        }
      }
      handler.endElement("div");
    } catch (SAXException e) {
      throw new IOExceptionWithCause("Unable to end a page", e);
    }
  }

  @Override
  protected void writeParagraphStart() throws IOException {
    // TODO: remove once PDFBOX-1130 is fixed
    if (inParagraph) {
      // Close last paragraph
      writeParagraphEnd();
    }
    assert !inParagraph;
    inParagraph = true;
    try {
      handler.startElement("p");
    } catch (SAXException e) {
      throw new IOExceptionWithCause("Unable to start a paragraph", e);
    }
  }

  @Override
  protected void writeParagraphEnd() throws IOException {
    // TODO: remove once PDFBOX-1130 is fixed
    if (!inParagraph) {
      writeParagraphStart();
    }
    assert inParagraph;
    inParagraph = false;
    try {
      handler.endElement("p");
    } catch (SAXException e) {
      throw new IOExceptionWithCause("Unable to end a paragraph", e);
    }
  }

  @Override
  protected void writeString(String text) throws IOException {
    try {
      handler.characters(text);
    } catch (SAXException e) {
      throw new IOExceptionWithCause("Unable to write a string: " + text, e);
    }
  }

  @Override
  protected void writeCharacters(TextPosition text) throws IOException {
    try {
      handler.characters(text.getCharacter());
    } catch (SAXException e) {
      throw new IOExceptionWithCause("Unable to write a character: "
                                     + text.getCharacter(), e);
    }
  }

  @Override
  protected void writeWordSeparator() throws IOException {
    try {
      handler.characters(getWordSeparator());
    } catch (SAXException e) {
      throw new IOExceptionWithCause("Unable to write a space character", e);
    }
  }

  @Override
  protected void writeLineSeparator() throws IOException {
    try {
      handler.newline();
    } catch (SAXException e) {
      throw new IOExceptionWithCause("Unable to write a newline character", e);
    }
  }

  private void extractEmbeddedDocuments(PDDocument document,
                                        ContentHandler handler) throws IOException, SAXException, TikaException {
    PDDocumentCatalog catalog = document.getDocumentCatalog();
    PDDocumentNameDictionary names = catalog.getNames();
    if (names == null) {
      return;
    }
    PDEmbeddedFilesNameTreeNode embeddedFiles = names.getEmbeddedFiles();

    if (embeddedFiles == null) {
      return;
    }

    EmbeddedDocumentExtractor embeddedExtractor = context
        .get(EmbeddedDocumentExtractor.class);
    if (embeddedExtractor == null) {
      embeddedExtractor = new ParsingEmbeddedDocumentExtractor(context);
    }

    Map<String, COSObjectable> embeddedFileNames = embeddedFiles.getNames();
    // For now, try to get the embeddedFileNames out of embeddedFiles or its
    // kids.
    // This code follows: pdfbox/examples/pdmodel/ExtractEmbeddedFiles.java
    // If there is a need we could add a fully recursive search to find a
    // non-null
    // Map<String, COSObjectable> that contains the doc info.
    if (embeddedFileNames != null) {
      processEmbeddedDocNames(embeddedFileNames, embeddedExtractor);
    } else {
      List<PDNameTreeNode> kids = embeddedFiles.getKids();
      if (kids == null) {
        return;
      }
      for (PDNameTreeNode n : kids) {
        Map<String, COSObjectable> childNames = n.getNames();
        if (childNames != null) {
          processEmbeddedDocNames(childNames, embeddedExtractor);
        }
      }
    }
  }

  private void processEmbeddedDocNames(
    Map<String, COSObjectable> embeddedFileNames,
    EmbeddedDocumentExtractor embeddedExtractor) throws IOException,
    SAXException, TikaException {
    if (embeddedFileNames == null) {
      return;
    }
    for (Map.Entry<String, COSObjectable> ent : embeddedFileNames.entrySet()) {
      PDComplexFileSpecification spec = (PDComplexFileSpecification) ent
                                        .getValue();
      PDEmbeddedFile file = spec.getEmbeddedFile();

      Metadata metadata = new Metadata();
      // TODO: other metadata?
      metadata.set(Metadata.RESOURCE_NAME_KEY, ent.getKey());
      metadata.set(Metadata.CONTENT_TYPE, file.getSubtype());
      metadata.set(Metadata.CONTENT_LENGTH, Long.toString(file.getSize()));

      if (embeddedExtractor.shouldParseEmbedded(metadata)) {
        TikaInputStream stream = TikaInputStream.get(file.createInputStream());
        try {
          embeddedExtractor.parseEmbedded(stream, new EmbeddedContentHandler(
                                            handler), metadata, false);
        } finally {
          stream.close();
        }
      }
    }
  }

  private void extractAcroForm(PDDocument pdf, XHTMLContentHandler handler)
  throws IOException, SAXException {
    // Thank you, Ben Litchfield, for
    // org.apache.pdfbox.examples.fdf.PrintFields
    // this code derives from Ben's code
    PDDocumentCatalog catalog = pdf.getDocumentCatalog();

    if (catalog == null)
      return;

    PDAcroForm form = catalog.getAcroForm();
    if (form == null)
      return;

    @SuppressWarnings("rawtypes")
    List fields = form.getFields();

    if (fields == null)
      return;

    @SuppressWarnings("rawtypes")
    ListIterator itr = fields.listIterator();

    if (itr == null)
      return;

    handler.startElement("div", "class", "acroform");
    handler.startElement("ol");
    while (itr.hasNext()) {
      Object obj = itr.next();
      if (obj != null && obj instanceof PDField) {
        processAcroField((PDField) obj, handler, 0);
      }
    }
    handler.endElement("ol");
    handler.endElement("div");
  }

  private void processAcroField(PDField field, XHTMLContentHandler handler,
                                final int recurseDepth) throws SAXException, IOException {

    if (recurseDepth >= MAX_ACROFORM_RECURSIONS) {
      return;
    }

    addFieldString(field, handler);

    @SuppressWarnings("rawtypes")
    List kids = field.getKids();
    if (kids != null) {

      @SuppressWarnings("rawtypes")
      Iterator kidsIter = kids.iterator();
      if (kidsIter == null) {
        return;
      }
      int r = recurseDepth + 1;
      handler.startElement("ol");
      while (kidsIter.hasNext()) {
        Object pdfObj = kidsIter.next();
        if (pdfObj != null && pdfObj instanceof PDField) {
          PDField kid = (PDField) pdfObj;
          // recurse
          processAcroField(kid, handler, r);
        }
      }
      handler.endElement("ol");
    }
  }

  private void addFieldString(PDField field, XHTMLContentHandler handler)
  throws SAXException {
    // Pick partial name to present in content and altName for attribute
    // Ignoring FullyQualifiedName for now
    String partName = field.getPartialName();
    String altName = field.getAlternateFieldName();

    StringBuilder sb = new StringBuilder();
    AttributesImpl attrs = new AttributesImpl();

    if (partName != null) {
      sb.append(partName).append(": ");
    }
    if (altName != null) {
      attrs.addAttribute("", "altName", "altName", "CDATA", altName);
    }
    // return early if PDSignature field
    if (field instanceof PDSignatureField) {
      handleSignature(attrs, (PDSignatureField) field, handler);
      return;
    }
    try {
      // getValue can throw an IOException if there is no value
      String value = field.getValue();
      if (value != null && !value.equals("null")) {
        sb.append(value);
      }
    } catch (Exception e) {
      // swallow
    }

    if (attrs.getLength() > 0 || sb.length() > 0) {
      handler.startElement("li", attrs);
      handler.characters(sb.toString());
      handler.endElement("li");
    }
  }

  private void handleSignature(AttributesImpl parentAttributes,
                               PDSignatureField sigField, XHTMLContentHandler handler)
  throws SAXException {

    PDSignature sig = sigField.getSignature();
    if (sig == null) {
      return;
    }
    Map<String, String> vals = new TreeMap<String, String>();
    vals.put("name", sig.getName());
    vals.put("contactInfo", sig.getContactInfo());
    vals.put("location", sig.getLocation());
    vals.put("reason", sig.getReason());

    Calendar cal = sig.getSignDate();
    if (cal != null) {
      dateFormat.setTimeZone(cal.getTimeZone());
      vals.put("date", dateFormat.format(cal.getTime()));
    }
    // see if there is any data
    int nonNull = 0;
    for (String val : vals.keySet()) {
      if (val != null && !val.equals("")) {
        nonNull++;
      }
    }
    // if there is, process it
    if (nonNull > 0) {
      handler.startElement("li", parentAttributes);

      AttributesImpl attrs = new AttributesImpl();
      attrs.addAttribute("", "type", "type", "CDATA", "signaturedata");

      handler.startElement("ol", attrs);
      for (Map.Entry<String, String> e : vals.entrySet()) {
        if (e.getValue() == null || e.getValue().equals("")) {
          continue;
        }
        attrs = new AttributesImpl();
        attrs.addAttribute("", "signdata", "signdata", "CDATA", e.getKey());
        handler.startElement("li", attrs);
        handler.characters(e.getValue());
        handler.endElement("li");
      }
      handler.endElement("ol");
      handler.endElement("li");
    }
  }

  private int imageCount, pageCount;

  /**
   *
   *
   * @param pdf
   *
   * @throws SAXException
   */
  private void extractImageText(PDDocument pdf) {
    List<?> pages = pdf.getDocumentCatalog().getAllPages();
    Iterator<?> pageIterator = pages.iterator();
    pageCount = pages.size();
    imageCount = 0;
    int currentPage = 0;
    TikaImageHelper helper = new TikaImageHelper(this.metadata);
    try {
      while (pageIterator.hasNext()) {
        PDPage page = (PDPage) pageIterator.next();
        PDResources resources = page.getResources();
        processResources(resources, helper);
        helper.addTextToHandler(handler, ++currentPage, pageCount);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (helper != null) {
        helper.close();
      }
    }
  }

  /**
   *
   *
   * @param resources
   *
   * @throws IOException
   */
  private void processResources(PDResources resources, TikaImageHelper helper)
  throws IOException {
    if (resources == null)
      return;

    try {
      Map<String, PDXObject> xobjects = resources.getXObjects();
      if (xobjects != null) {
        Iterator<String> xobjectIter = xobjects.keySet().iterator();
        while (xobjectIter.hasNext()) {
          String key = xobjectIter.next();
          PDXObject xobject = xobjects.get(key);
          // write the images
          if (xobject instanceof PDXObjectImage) {
            PDXObjectImage image = (PDXObjectImage) xobject;
            if (ImageTools.imagePerPageRatioValid(imageCount++, pageCount)
                && ImageTools.imageSizeValid(image.getHeight(), image.getWidth())) {
              BufferedImage buffImage = image.getRGBImage();
              helper.addImage(buffImage);
            }
          }
          // maybe there are more images embedded in a form object
          else if (xobject instanceof PDXObjectForm) {
            if (ImageTools.imagePerPageRatioValid(imageCount, pageCount)) {
              PDXObjectForm xObjectForm = (PDXObjectForm) xobject;
              PDResources formResources = xObjectForm.getResources();
              processResources(formResources, helper);
            }
          }
        }
      }
    } catch (Exception e) {
      LOG.info("Shit happened", e);
    }

  }
}
