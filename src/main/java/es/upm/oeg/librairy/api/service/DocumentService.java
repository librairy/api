package es.upm.oeg.librairy.api.service;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.builders.DateBuilder;
import es.upm.oeg.librairy.api.executors.ParallelExecutor;
import es.upm.oeg.librairy.api.facade.model.avro.DataSink;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.facade.model.avro.DocumentsRequest;
import es.upm.oeg.librairy.api.io.reader.Reader;
import es.upm.oeg.librairy.api.io.reader.ReaderFactory;
import es.upm.oeg.librairy.api.io.writer.Writer;
import es.upm.oeg.librairy.api.io.writer.WriterFactory;
import es.upm.oeg.librairy.api.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

@Component
public class DocumentService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    MailService mailService;

    @Autowired
    LanguageService languageService;

    @Autowired
    ReaderFactory readerFactory;

    @Autowired
    WriterFactory writerFactory;

    public boolean create(DocumentsRequest request){
        try{
            LOG.info("Ready to create documents from: " + request);
            try{
                DataSource datasource = request.getDataSource();
                Reader reader = readerFactory.newFrom(datasource);

                DataSink dataSink = request.getDataSink();
                Writer writer = writerFactory.newFrom(dataSink);

                Long maxSize = datasource.getSize();
                AtomicInteger counter = new AtomicInteger();
                Integer interval = maxSize > 0? maxSize > 100? maxSize.intValue()/100 : 1 : 100;
                Optional<Document> doc;
                reader.offset(datasource.getOffset().intValue());
                ParallelExecutor parallelExecutor = new ParallelExecutor();
                final String date = DateBuilder.now();

                final String source = Strings.isNullOrEmpty(datasource.getName())? datasource.getUrl() : datasource.getName().replace(" ","-");

                final ConcurrentHashMap<String,Integer> errors = new ConcurrentHashMap<>();
                while(( maxSize<0 || counter.get()<maxSize) &&  (doc = reader.next()).isPresent()){
                    final Document document = doc.get();
                    if (!document.isValid()) continue;
                    if (counter.incrementAndGet() % interval == 0) LOG.info(counter.get() + " documents indexed");
                    parallelExecutor.submit(() -> {
                        try {
                            String lang = languageService.getLanguage(document.getText());
                            if (lang.equalsIgnoreCase("unknown")){
                                lang = languageService.getLanguage(document.getName());
                            }
                            document.setSource(source);
                            document.setDate(date);
                            document.setLang(lang);
                            writer.save(document);
                        } catch (Exception e) {
                            LOG.error("Unexpected error creating document",e);
                            errors.put(e.getClass().getName(),1);
                        }
                    });
                }
                parallelExecutor.awaitTermination(5, TimeUnit.MINUTES);
                writer.close();
                if (errors.isEmpty()) mailService.notifyDocumentCreation(request, "Documents created");
                else mailService.notifyDocumentError(request, "Documents cannot be created");
                return true;
            }catch (Exception e){
                LOG.error("Unexpected error harvesting datasource: " + request, e);
                mailService.notifyDocumentError(request, "DataSource error. For details consult with your system administrator.");
                return false;
            }

        }catch (Exception e){
            LOG.error("Error creating documents",e);
            if (request != null) mailService.notifyDocumentError(request, "Documents not created. For details consult with your system administrator.  ");
            return false;
        }
    }

}
