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
package mj.extraction.tika.parser.microsoft.ooxml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import mj.extraction.tika.parser.pdf.PDFParser;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Office Open XML (OOXML) parser.
 */
public class OOXMLParser extends AbstractParser {

        /** Serial version UID */
        private static final long serialVersionUID = 6535995710857776481L;

        protected static final Set<MediaType> SUPPORTED_TYPES = Collections.unmodifiableSet(new HashSet<MediaType>(Arrays
                        .asList(MediaType.application("x-tika-ooxml"),
                                        MediaType.application("vnd.openxmlformats-officedocument.presentationml.presentation"),
                                        MediaType.application("vnd.ms-powerpoint.presentation.macroenabled.12"),
                                        MediaType.application("vnd.openxmlformats-officedocument.presentationml.template"),
                                        MediaType.application("vnd.openxmlformats-officedocument.presentationml.slideshow"),
                                        MediaType.application("vnd.ms-powerpoint.slideshow.macroenabled.12"),
                                        MediaType.application("vnd.ms-powerpoint.addin.macroenabled.12"),
                                        MediaType.application("vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                                        MediaType.application("vnd.ms-excel.sheet.macroenabled.12"),
                                        MediaType.application("vnd.openxmlformats-officedocument.spreadsheetml.template"),
                                        MediaType.application("vnd.ms-excel.template.macroenabled.12"),
                                        MediaType.application("vnd.ms-excel.addin.macroenabled.12"),
                                        MediaType.application("vnd.openxmlformats-officedocument.wordprocessingml.document"),
                                        MediaType.application("vnd.ms-word.document.macroenabled.12"),
                                        MediaType.application("vnd.openxmlformats-officedocument.wordprocessingml.template"),
                                        MediaType.application("vnd.ms-word.template.macroenabled.12"))));

        /**
         * We claim to support all OOXML files, but we actually don't support a
         * small number of them. This list is used to decline certain formats that
         * are not yet supported by Tika and/or POI.
         */
        protected static final Set<MediaType> UNSUPPORTED_OOXML_TYPES = Collections.unmodifiableSet(new HashSet<MediaType>(
                        Arrays.asList(MediaType.application("vnd.ms-excel.sheet.binary.macroenabled.12"),
                                        MediaType.application("vnd.ms-xpsdocument"))));

        public Set<MediaType> getSupportedTypes(ParseContext context) {
                return SUPPORTED_TYPES;
        }

        public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context)
                        throws IOException, SAXException, TikaException {
                // Have the OOXML file processed
                OOXMLExtractorFactory.parse(stream, handler, metadata, context);
        }

        /**
         * Main function.
         */
        public static void main(String[] args) {

                try {
                        // Tika tika = new Tika();
                        // File xpsFile = new File("/home/foo/a/temp/xlsx.xlsx");
                        // InputStream inputStream = new FileInputStream(xpsFile);
                        // String FileName = xpsFile.getName();
                        // Metadata metadata = new Metadata();
                        // if (FileName != null && FileName.length() > 0)
                        // metadata.add(Metadata.RESOURCE_NAME_KEY, FileName);
                        // String MimeType = tika.detect(inputStream, metadata);

                        // metadata.add(Metadata.CONTENT_TYPE, MimeType);
                        // inputStream.close();
                        // inputStream = new FileInputStream(xpsFile);
                        // Reader reader = tika.parse(inputStream, metadata);
                        // String content = IOUtils.toString(reader);

                        // System.out.println(new AutoDetectParser().getParsers().keySet());
                        // System.out.println("shit: " + tika.getParser() + " " + MimeType);
                        // System.out.println(content);
                        // inputStream.close();

                        ClassLoader loader = Thread.currentThread().getContextClassLoader();
                        TikaConfig config = new TikaConfig(new File("/home/foo/a/code/big_bang/tika-1.5/"
                                        + "tika-core/src/main/resources/org/apache/tika/mime/tika-mimetypes.xml"));

                        final AutoDetectParser autoDetectParser = new AutoDetectParser(config);

                        final Detector detector = config.getDetector();
                        final Tika tika = new Tika();


                        File xpsFile = new File("/home/foo/a/temp/xlsx.xlsx");
                        InputStream inputStream = new FileInputStream(xpsFile);
                        String FileName = xpsFile.getName();
                        Metadata metadata = new Metadata();
                        if (FileName != null && FileName.length() > 0)
                                metadata.add(Metadata.RESOURCE_NAME_KEY, FileName);

                        String MimeType = tika.detect(inputStream, metadata);
                        // metadata.add(Metadata.CONTENT_TYPE, MimeType);
                        // ContentHandler handler = new XHTMLContentHandler(System.out);

                        // ContentHandler bch = new BodyContentHandler(System.out);
                        // ContentHandler handler = new BodyContentHandler();
                        // ContentHandler xhtml = new XHTMLContentHandler(handler,
                        // metadata);

                        StringWriter sw = new StringWriter();

                        SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
                        TransformerHandler handler = factory.newTransformerHandler();
                        handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
                        handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "no");
                        handler.setResult(new StreamResult(sw));
                        BodyContentHandler bch = new BodyContentHandler(handler);
                        handler.startDocument();
                        inputStream.close();
                        inputStream = new FileInputStream(xpsFile);
                        autoDetectParser.parse(inputStream, bch, metadata);
                        String x = sw.toString();
                        System.out.println(x);

                        // Document doc = Jsoup.parse(x);

                        // Elements elements = doc.getElementsByTag("p");
                        // for (Element element : elements) {
                        //         System.out.println(element.text());
                        // }

                } catch (Exception e) {
                        e.printStackTrace();
                }
        }
}
