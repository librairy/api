package es.upm.oeg.librairy.api.service;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.builders.CorpusBuilder;
import es.upm.oeg.librairy.api.executors.ParallelExecutor;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.facade.model.avro.TopicsRequest;
import es.upm.oeg.librairy.api.io.reader.Reader;
import es.upm.oeg.librairy.api.io.reader.ReaderFactory;
import es.upm.oeg.librairy.api.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

@Component
public class CollectorService {

    private static final Logger LOG = LoggerFactory.getLogger(CollectorService.class);

    @Autowired
    MailService mailService;

    @Autowired
    ReaderFactory readerFactory;

    public boolean collect(CorpusBuilder corpusBuilder, TopicsRequest request) {

        try{
            Boolean multigrams  = request.getParameters() != null && request.getParameters().containsKey("multigrams")? Boolean.valueOf(request.getParameters().get("multigrams")) : false;

            Boolean lowerCase   = request.getParameters() != null && request.getParameters().containsKey("lowercase")? Boolean.valueOf(request.getParameters().get("lowercase")) : false;


            DataSource datasource = request.getDataSource();

            LOG.info("Building bag-of-words with "+ (multigrams? "multigrams" : "unigrams") + " from: "  + datasource);

            Reader reader = readerFactory.newFrom(datasource);

            Long maxSize = datasource.getSize();
            AtomicInteger counter = new AtomicInteger();
            Integer interval = maxSize > 0? maxSize > 100? maxSize.intValue()/100 : 1 : 100;
            Optional<Document> doc;
            reader.offset(datasource.getOffset().intValue());
            final Boolean raw = request.getParameters().containsKey("raw")? Boolean.valueOf(request.getParameters().get("raw")) : false;
            final Map<String,Long> stopwords = request.getParameters().containsKey("stopwords")? Arrays.stream(request.getParameters().get("stopwords").split(" ")).collect(Collectors.groupingBy(Function.identity(), Collectors.counting())) : new HashMap<>();
            ParallelExecutor parallelExecutor = new ParallelExecutor();
            while(( maxSize<0 || counter.get()<maxSize) &&  (doc = reader.next()).isPresent()){
                final Document document = doc.get();
                if (Strings.isNullOrEmpty(document.getText())) continue;
                if (counter.incrementAndGet() % interval == 0) LOG.info(counter.get() + " documents indexed");
                parallelExecutor.submit(() -> {
                    try {

                        corpusBuilder.add(document, multigrams, raw, lowerCase, stopwords);
                    } catch (Exception e) {
                        LOG.error("Unexpected error adding new document to corpus",e);
                    }
                });
            }
            parallelExecutor.awaitTermination(1, TimeUnit.HOURS);
//            mailService.notifyModelCreation(request, "Datasource analyzed. Ready to create a new topic model.");
            return true;
        }catch (Exception e){
            LOG.error("Unexpected error harvesting datasource: " + request, e);
            mailService.notifyModelError(request, "Datasource error. For details consult with your system administrator.");
            return false;
        }finally{
            corpusBuilder.close();
        }
    }
}
