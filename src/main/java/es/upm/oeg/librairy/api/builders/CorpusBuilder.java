package es.upm.oeg.librairy.api.builders;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.BuiltInLanguages;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import es.upm.oeg.librairy.api.io.reader.*;
import es.upm.oeg.librairy.api.io.reader.StringReader;
import es.upm.oeg.librairy.api.model.Document;
import es.upm.oeg.librairy.api.service.LanguageService;
import org.librairy.service.modeler.clients.LibrairyNlpClient;
import org.librairy.service.modeler.service.BoWService;
import org.librairy.service.nlp.facade.model.PoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class CorpusBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(CorpusBuilder.class);

    private final LanguageService languageService;

    private final LibrairyNlpClient librairyNlpClient;

    public static final String SEPARATOR = ";;";

    private static final String DEFAULT_LANG = "en";

    private BufferedWriter writer;
    private Path filePath;
    private Boolean isClosed = true;
    private AtomicInteger counter   = new AtomicInteger(0);
    private String updated = "";
    private String language = null;
    private AtomicInteger pendingDocs = new AtomicInteger();


    private final Escaper escaper = Escapers.builder()
            .addEscape('\'',"")
            .addEscape('\"',"")
            .addEscape('\n'," ")
            .addEscape('\r'," ")
            .addEscape('\t'," ")
            .build();


    public CorpusBuilder(Path filePath, LibrairyNlpClient librairyNlpClient, LanguageService languageService) throws IOException {

        this.filePath = filePath;

        this.librairyNlpClient = librairyNlpClient;

        this.languageService = languageService;
    }

    public Integer getNumDocs(){
        return counter.get();
    }

    public void add(Document document, Boolean multigrams, Boolean raw) throws IOException {
        if (Strings.isNullOrEmpty(document.getText()) || Strings.isNullOrEmpty(document.getText().replace("\n","").trim())) {
            LOG.warn("Document is empty: " + document.getId());
            return;
        }
        try{
            pendingDocs.incrementAndGet();
            StringBuilder row = new StringBuilder();
            row.append(document.getId()).append(SEPARATOR);
            row.append(escaper.escape(document.getId())).append(SEPARATOR);
            String labels = document.getLabels().stream().collect(Collectors.joining(" "));
            if (Strings.isNullOrEmpty(labels)) labels = "default";
            row.append(labels).append(SEPARATOR);
            updateLanguage(document.getText());
            // bow from nlp-service
            String content = StringReader.hardFormat(document.getText());
            String text = raw? content : BoWService.toText(librairyNlpClient.bow( content, language, Arrays.asList(PoS.NOUN, PoS.VERB, PoS.PROPER_NOUN, PoS.ADJECTIVE), multigrams));
            row.append(text);
            updated = DateBuilder.now();
            write(row.toString());
            LOG.info("Added document: [" + document.getId() +"] to corpus");
        }finally{
            pendingDocs.decrementAndGet();
        }
    }

    private synchronized void write(String text) {
        try {
            if (isClosed) {
                writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(filePath.toFile(), true))));
                setClosed(false);
            }
            writer.write(text);
            writer.newLine();
            counter.incrementAndGet();
        } catch (IOException e) {
            LOG.warn("Error writing on file: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Unexpected Error writing on file: " + e.getMessage(), e);
        }
    }


    public synchronized boolean load(){

        if (!filePath.toFile().exists()) return false;
        LOG.info("Loading an existing corpus..");
        BufferedReader reader = null;
        try{
            reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filePath.toFile()))));
            counter.set(Long.valueOf(reader.lines().count()).intValue());
            updated = DateBuilder.from(filePath.toFile().lastModified());
            return true;
        }catch (Exception e){
            LOG.debug("Error reading lines in existing file: " + filePath,e);
            return false;
        }finally{
            if (reader != null) try {
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String updateLanguage(String text){
        language = Strings.isNullOrEmpty(language)? languageService.getLanguage(text) : language;
        return language;
    }

    public void close() {
        while(pendingDocs.get() > 0){
            LOG.info("waiting for adding "+pendingDocs.get()+" pending docs to close it... ");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted",e);
            }
        }

        if (pendingDocs.get() >0) LOG.info("Pending docs: " + pendingDocs.get());

        setClosed(true);
        if (writer != null){
            try{
//                writer.flush();
                writer.close();
                LOG.info("Writer closed with " + counter.get() + " documents added");
            }catch (IOException e){
                LOG.error("Writer closing error",e);
            }
        }
        LOG.info("Corpus closed");
    }

    private synchronized void setClosed(Boolean status){
        this.isClosed = status;
    }

    public Path getFilePath() {
        return filePath;
    }
}
