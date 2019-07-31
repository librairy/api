package es.upm.oeg.librairy.api.parser;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.builders.DateBuilder;
import es.upm.oeg.librairy.api.model.Document;
import es.upm.oeg.librairy.api.service.LanguageService;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public abstract class OfficeFileReader {

    private static final Logger LOG = LoggerFactory.getLogger(FileReader.class);

    private static final Integer MAXIMUM_TEXT_CHUNK_SIZE = 100000;

    @Value("#{environment['INPUT_SOURCE']?:'${input.source}'}")
    String source;

    @Autowired
    LanguageService languageService;

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
        try {
            Thread.sleep(10000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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


    public Document parse(Path path) {

        Document document = new Document();

        try {
            String fileName = path.getFileName().toString();
            String text = getTextChunks(new FileInputStream(path.toFile()), fileName);
            if (!Strings.isNullOrEmpty(text)){
                document.setId(UUID.randomUUID().toString());
                document.setName(fileName);
                document.setFile(fileName);
                document.setText(text);
                document.setDate(DateBuilder.now());
                document.setFormat(StringUtils.substringAfterLast(fileName,"."));

                String lang = languageService.getLanguage(text.substring(0,100));
                document.setLang(lang);
                document.setSource(source);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return document;
    }
}
