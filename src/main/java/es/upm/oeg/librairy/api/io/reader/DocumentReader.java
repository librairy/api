package es.upm.oeg.librairy.api.io.reader;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.builders.DateBuilder;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.model.Document;
import es.upm.oeg.librairy.api.service.LanguageService;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class DocumentReader implements Reader {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentReader.class);

    private static final Integer MAXIMUM_TEXT_CHUNK_SIZE = 100000;
    private final String source;
    private final String filePath;
    private final LanguageService langService;
    private final String format;
    private boolean finish;

    public DocumentReader(DataSource dataSource, LanguageService languageService, String format) {
        filePath = dataSource.getUrl();
        source = dataSource.getName();
        langService = languageService;
        finish = false;
        this.format = format;
    }

    @Override
    public Optional<Document> next() {
        if (finish) return Optional.empty();
        Document document = new Document();

        try {
            InputStream inputStream = filePath.toLowerCase().startsWith("http")? new URL(filePath).openStream() : new FileInputStream(filePath);

//            String text = getTextChunks(inputStream, filePath);
            String fileName = StringUtils.substringAfterLast(filePath,"/");

            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            parser.parse(inputStream, handler, metadata);
            inputStream.close();

            String text = StringReader.hardFormat(handler.toString());
            if (!Strings.isNullOrEmpty(text)){
                document.setId(UUID.randomUUID().toString());
                String title = Strings.isNullOrEmpty(metadata.get("title"))? "untitled" : metadata.get("title");
                document.setName(title);
                document.setFile(fileName);
                document.setText(text);
                document.setDate(DateBuilder.now());
                document.setFormat(format);

                String lang = langService.getLanguage(text);
                document.setLang(lang);
                document.setSource(source);
            }


        } catch (Exception e) {
            LOG.error("Unexpected error parsing file: " + filePath + " - " + e.getMessage());
        }
        this.finish = true;
        return Optional.of(document);
    }

    @Override
    public void offset(Integer numLines) {

    }

    public static String getText(InputStream inputStream, String filename){
        Metadata metadata = new Metadata();
        metadata.set(Metadata.RESOURCE_NAME_KEY, filename);
        try {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            parser.parse(inputStream, handler, metadata);
            return handler.toString();
        } catch (Exception e) {
            LOG.error("Error parsing document: '" + filename + "'", e);
            return "";
        }

    }

    public static String getTextChunks(InputStream inputStream, String filename){
        final List<String> chunks = new ArrayList<>();
        chunks.add("");
        ContentHandlerDecorator handler = new ContentHandlerDecorator() {
            @Override
            public void characters(char[] ch, int start, int length) {
                String lastChunk = chunks.get(chunks.size() - 1);
                String thisStr = new String(ch, start, length);

                if (lastChunk.length() + length > MAXIMUM_TEXT_CHUNK_SIZE) {
                    chunks.add(thisStr);
                } else {
                    chunks.set(chunks.size() - 1, lastChunk + thisStr);
                }
            }
        };

        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        metadata.set(Metadata.RESOURCE_NAME_KEY, filename);
        try {
            parser.parse(inputStream, handler, metadata);
            return chunks.stream().collect(Collectors.joining(" "));
        } catch (Exception e) {
            LOG.error("Error parsing document: '" + filename + "'", e);
            return "";
        }
    }

}
