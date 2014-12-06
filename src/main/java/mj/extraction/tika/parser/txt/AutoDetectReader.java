package mj.extraction.tika.parser.txt;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.tika.config.LoadErrorHandler;
import org.apache.tika.config.ServiceLoader;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.utils.CharsetUtils;
import org.xml.sax.InputSource;

/**
 * An input stream reader that automatically detects the character encoding
 * to be used for converting bytes to characters.
 *
 * @since Apache Tika 1.2
 */
public class AutoDetectReader extends BufferedReader {

    private static final ServiceLoader DEFAULT_LOADER =
            new ServiceLoader(AutoDetectReader.class.getClassLoader());

    private static Charset detect(
            InputStream input, Metadata metadata,
            List<EncodingDetector> detectors, LoadErrorHandler handler)
            throws IOException, TikaException {
        // Ask all given detectors for the character encoding
        for (EncodingDetector detector : detectors) {
            try {
                Charset charset = detector.detect(input, metadata);
                if (charset != null) {
                    return charset;
                }
            } catch (NoClassDefFoundError e) {
                // TIKA-1041: Detector dependencies not present.
                handler.handleLoadError(detector.getClass().getName(), e);
            }
        }

        // Try determining the encoding based on hints in document metadata
        MediaType type = MediaType.parse(metadata.get(Metadata.CONTENT_TYPE));
        if (type != null) {
            String charset = type.getParameters().get("charset");
            if (charset != null) {
                try {
                    return CharsetUtils.forName(charset);
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        return Charset.defaultCharset();
        // throw new TikaException(
        //         "Failed to detect the character encoding of a document");
    }

    private final Charset charset;

    private AutoDetectReader(InputStream stream, Charset charset)
            throws IOException {
        super(new InputStreamReader(stream, charset));
        this.charset = charset;

        // TIKA-240: Drop the BOM if present
        mark(1);
        if (read() != '\ufeff') { // zero-width no-break space
            reset();
        }
    }

    private AutoDetectReader(
            BufferedInputStream stream, Metadata metadata,
            List<EncodingDetector> detectors, LoadErrorHandler handler)
            throws IOException, TikaException {
        this(stream, detect(stream, metadata, detectors, handler));
    }

    public AutoDetectReader(
            InputStream stream, Metadata metadata,
            ServiceLoader loader) throws IOException, TikaException {
        this(new BufferedInputStream(stream), metadata,
                loader.loadServiceProviders(EncodingDetector.class),
                loader.getLoadErrorHandler());
    }

    public AutoDetectReader(InputStream stream, Metadata metadata)
            throws IOException, TikaException {
        this(new BufferedInputStream(stream), metadata, DEFAULT_LOADER);
    }

    public AutoDetectReader(InputStream stream)
            throws IOException, TikaException {
        this(stream, new Metadata());
    }

    public Charset getCharset() {
        return charset;
    }

    public InputSource asInputSource() {
        InputSource source = new InputSource(this);
        source.setEncoding(charset.name());
        return source;
    }

}
