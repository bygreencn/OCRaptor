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
package mj.extraction.tika.parser.pdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import mj.MainController;
import mj.file_handler.utils.FileTools;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.io.RandomAccess;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.io.CloseShieldInputStream;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.PagedText;
import org.apache.tika.metadata.Property;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.PasswordProvider;
import org.ghost4j.Ghostscript;
import org.ghost4j.GhostscriptException;
import org.ghost4j.converter.PDFConverter;
import org.ghost4j.document.PSDocument;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * PDF parser.
 * <p>
 * This parser can process also encrypted PDF documents if the required password
 * is given as a part of the input metadata associated with a document. If no
 * password is given, then this parser will try decrypting the document using
 * the empty password that's often used with PDFs. If the PDF contains any
 * embedded documents (for example as part of a PDF package) then this parser
 * will use the {@link EmbeddedDocumentExtractor} to handle them.
 */
public class PDFParser extends AbstractParser {

  /** Serial version UID */
  private static final long serialVersionUID = -752276948656079347L;

  private PDFParserConfig defaultConfig = new PDFParserConfig();

  /**
   * Metadata key for giving the document password to the parser.
   *
   * @since Apache Tika 0.5
   * @deprecated Supply a {@link PasswordProvider} on the {@link ParseContext}
   *             instead
   */
  public static final String PASSWORD = "org.apache.tika.parser.pdf.password";

  private static final Set<MediaType> SUPPORTED_TYPES = Collections.singleton(MediaType
      .application("pdf"));

  public Set<MediaType> getSupportedTypes(ParseContext context) {
    return SUPPORTED_TYPES;
  }

  public void parse(InputStream stream, ContentHandler handler, Metadata metadata,
      ParseContext context) throws IOException, SAXException, TikaException {

    PDDocument pdfDocument = null;
    TemporaryResources tmp = new TemporaryResources();
    // config from context, or default if not set via context
    PDFParserConfig localConfig = context.get(PDFParserConfig.class, defaultConfig);

    try {
      // PDFBox can process entirely in memory, or can use a temp file
      // for unpacked / processed resources
      // Decide which to do based on if we're reading from a file or not
      // already
      TikaInputStream tstream = TikaInputStream.cast(stream);
      if (tstream != null && tstream.hasFile()) {
        // File based, take that as a cue to use a temporary file
        RandomAccess scratchFile = new RandomAccessFile(tmp.createTemporaryFile(), "rw");
        if (localConfig.getUseNonSequentialParser() == true) {
          pdfDocument = PDDocument.loadNonSeq(new CloseShieldInputStream(stream), scratchFile);
        } else {
          pdfDocument = PDDocument.load(new CloseShieldInputStream(stream), scratchFile, true);
        }
      } else {
        // Go for the normal, stream based in-memory parsing
        if (localConfig.getUseNonSequentialParser() == true) {
          pdfDocument = PDDocument.loadNonSeq(new CloseShieldInputStream(stream),
              new RandomAccessBuffer());
        } else {
          pdfDocument = PDDocument.load(new CloseShieldInputStream(stream), true);
        }
      }

      if (pdfDocument.isEncrypted()) {
        String password = null;

        // Did they supply a new style Password Provider?
        PasswordProvider passwordProvider = context.get(PasswordProvider.class);
        if (passwordProvider != null) {
          password = passwordProvider.getPassword(metadata);
        }

        // Fall back on the old style metadata if set
        if (password == null && metadata.get(PASSWORD) != null) {
          password = metadata.get(PASSWORD);
        }

        // If no password is given, use an empty string as the default
        if (password == null) {
          password = "";
        }

        try {
          pdfDocument.decrypt(password);
        } catch (Exception e) {
          // Ignore
        }
      }

      metadata.set(Metadata.CONTENT_TYPE, "application/pdf");
      extractMetadata(pdfDocument, metadata);
      PDF2XHTML.process(pdfDocument, handler, context, metadata, localConfig);

    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    } finally {

      if (pdfDocument != null) {
        pdfDocument.close();
      }
      if (tmp != null) {
        tmp.dispose();
        tmp.close();
      }
    }
    handler.endDocument();
  }

  private void extractMetadata(PDDocument document, Metadata metadata) throws TikaException {
    PDDocumentInformation info = document.getDocumentInformation();
    metadata.set(PagedText.N_PAGES, document.getNumberOfPages());
    addMetadata(metadata, TikaCoreProperties.TITLE, info.getTitle());
    addMetadata(metadata, TikaCoreProperties.CREATOR, info.getAuthor());
    addMetadata(metadata, TikaCoreProperties.CREATOR_TOOL, info.getCreator());
    addMetadata(metadata, TikaCoreProperties.KEYWORDS, info.getKeywords());
    addMetadata(metadata, "producer", info.getProducer());
    // TODO: Move to description in Tika 2.0
    addMetadata(metadata, TikaCoreProperties.TRANSITION_SUBJECT_TO_OO_SUBJECT, info.getSubject());
    addMetadata(metadata, "trapped", info.getTrapped());
    try {
      // TODO Remove these in Tika 2.0
      addMetadata(metadata, "created", info.getCreationDate());
      addMetadata(metadata, TikaCoreProperties.CREATED, info.getCreationDate());
    } catch (IOException e) {
      // Invalid date format, just ignore
    }
    try {
      Calendar modified = info.getModificationDate();
      addMetadata(metadata, Metadata.LAST_MODIFIED, modified);
      addMetadata(metadata, TikaCoreProperties.MODIFIED, modified);
    } catch (IOException e) {
      // Invalid date format, just ignore
    }

    // All remaining metadata is custom
    // Copy this over as-is
    List<String> handledMetadata = Arrays.asList(new String[] { "Author", "Creator",
        "CreationDate", "ModDate", "Keywords", "Producer", "Subject", "Title", "Trapped" });
    for (COSName key : info.getDictionary().keySet()) {
      String name = key.getName();
      if (!handledMetadata.contains(name)) {
        addMetadata(metadata, name, info.getDictionary().getDictionaryObject(key));
      }
    }
  }

  private void addMetadata(Metadata metadata, Property property, String value) {
    if (value != null) {
      metadata.add(property, value);
    }
  }

  private void addMetadata(Metadata metadata, String name, String value) {
    if (value != null) {
      metadata.add(name, value);
    }
  }

  private void addMetadata(Metadata metadata, String name, Calendar value) {
    if (value != null) {
      metadata.set(name, value.getTime().toString());
    }
  }

  private void addMetadata(Metadata metadata, Property property, Calendar value) {
    if (value != null) {
      metadata.set(property, value.getTime());
    }
  }

  /**
   * Used when processing custom metadata entries, as PDFBox won't do the
   * conversion for us in the way it does for the standard ones
   */
  private void addMetadata(Metadata metadata, String name, COSBase value) {
    if (value instanceof COSArray) {
      for (Object v : ((COSArray) value).toList()) {
        addMetadata(metadata, name, ((COSBase) v));
      }
    } else if (value instanceof COSString) {
      addMetadata(metadata, name, ((COSString) value).getString());
    } else if (value != null) {
      addMetadata(metadata, name, value.toString());
    }
  }

  public void setPDFParserConfig(PDFParserConfig config) {
    this.defaultConfig = config;
  }

  public PDFParserConfig getPDFParserConfig() {
    return defaultConfig;
  }

  /**
   * If true, the parser will use the NonSequentialParser. This may be faster
   * than the full doc parser. If false (default), this will use the full doc
   * parser.
   *
   * @deprecated use {@link #setPDFParserConfig(PDFParserConfig)}
   */
  public void setUseNonSequentialParser(boolean v) {
    defaultConfig.setUseNonSequentialParser(v);
  }

  /**
   * @see #setUseNonSequentialParser(boolean)
   * @deprecated use {@link #getPDFParserConfig()}
   */
  public boolean getUseNonSequentialParser() {
    return defaultConfig.getUseNonSequentialParser();
  }

  /**
   * If true (the default), the parser should estimate where spaces should be
   * inserted between words. For many PDFs this is necessary as they do not
   * include explicit whitespace characters.
   *
   * @deprecated use {@link #setPDFParserConfig(PDFParserConfig)}
   */
  public void setEnableAutoSpace(boolean v) {
    defaultConfig.setEnableAutoSpace(v);
  }

  /**
   * @see #setEnableAutoSpace.
   * @deprecated use {@link #getPDFParserConfig()}
   */
  public boolean getEnableAutoSpace() {
    return defaultConfig.getEnableAutoSpace();
  }

  /**
   * If true (the default), text in annotations will be extracted.
   *
   * @deprecated use {@link #setPDFParserConfig(PDFParserConfig)}
   */
  public void setExtractAnnotationText(boolean v) {
    defaultConfig.setExtractAnnotationText(v);
  }

  /**
   * If true, text in annotations will be extracted.
   *
   * @deprecated use {@link #getPDFParserConfig()}
   */
  public boolean getExtractAnnotationText() {
    return defaultConfig.getExtractAnnotationText();
  }

  /**
   * If true, the parser should try to remove duplicated text over the same
   * region. This is needed for some PDFs that achieve bolding by re-writing the
   * same text in the same area. Note that this can slow down extraction
   * substantially (PDFBOX-956) and sometimes remove characters that were not in
   * fact duplicated (PDFBOX-1155). By default this is disabled.
   *
   * @deprecated use {@link #setPDFParserConfig(PDFParserConfig)}
   */
  public void setSuppressDuplicateOverlappingText(boolean v) {
    defaultConfig.setSuppressDuplicateOverlappingText(v);
  }

  /**
   * @see #setSuppressDuplicateOverlappingText.
   *
   * @deprecated use {@link #getPDFParserConfig()}
   */
  public boolean getSuppressDuplicateOverlappingText() {
    return defaultConfig.getSuppressDuplicateOverlappingText();
  }

  /**
   * If true, sort text tokens by their x/y position before extracting text.
   * This may be necessary for some PDFs (if the text tokens are not rendered
   * "in order"), while for other PDFs it can produce the wrong result (for
   * example if there are 2 columns, the text will be interleaved). Default is
   * false.
   *
   * @deprecated use {@link #setPDFParserConfig(PDFParserConfig)}
   */
  public void setSortByPosition(boolean v) {
    defaultConfig.setSortByPosition(v);
  }

  /**
   * @see #setSortByPosition.
   *
   * @deprecated use {@link #getPDFParserConfig()}
   */
  public boolean getSortByPosition() {
    return defaultConfig.getSortByPosition();
  }

  /**
   *
   *
   * @param inputPDF
   * @return
   */
  public synchronized static File convertPostScriptToPDF(File inputPDF) {
    FileOutputStream fos = null;
    File tempFile = null;

    try {
      PSDocument document = new PSDocument();
      document.load(inputPDF);
      tempFile = FileTools.getRandomTempFile("ps", ".pdf");
      fos = new FileOutputStream(tempFile);

      // create converter
      PDFConverter converter = new PDFConverter();
      // converter.setMaxProcessCount(MainController.inst().getNumThreads());

      // set options
      converter.setPDFSettings(PDFConverter.OPTION_PDFSETTINGS_PREPRESS);
      converter.convert(document, fos);

    } catch (IOException e) {
      // TODO:
      // check why windows throws an error
    } catch (Exception e) {
      // TODO: logging
      e.printStackTrace();
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return tempFile;
  }
}
