package es.upm.oeg.librairy.api.service;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.builders.HashTopicBuilder;
import es.upm.oeg.librairy.api.executors.ParallelExecutor;
import es.upm.oeg.librairy.api.facade.model.avro.AnnotationsRequest;
import es.upm.oeg.librairy.api.facade.model.avro.DataSink;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.io.reader.Reader;
import es.upm.oeg.librairy.api.io.reader.ReaderFactory;
import es.upm.oeg.librairy.api.io.writer.Writer;
import es.upm.oeg.librairy.api.io.writer.WriterFactory;
import es.upm.oeg.librairy.api.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class AnnotationService {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationService.class);

    @Autowired
    MailService mailService;

    @Autowired
    InferenceService inferenceService;

    @Autowired
    ReaderFactory readerFactory;

    @Autowired
    WriterFactory writerFactory;

    public void create(AnnotationsRequest annotationRequest){

        try{

            DataSource dataSource = annotationRequest.getDataSource();
            Reader reader = readerFactory.newFrom(dataSource);

            DataSink dataSink = annotationRequest.getDataSink();
            Writer writer = writerFactory.newFrom(dataSink);

            Long maxSize = dataSource.getSize();
            AtomicInteger counter = new AtomicInteger();
            Integer interval = maxSize > 0? maxSize > 100? maxSize.intValue()/100 : 1 : 100;
            Optional<Document> doc;
            reader.offset(dataSource.getOffset().intValue());
            ParallelExecutor parallelExecutor = new ParallelExecutor();
            while(( maxSize<0 || counter.get()<maxSize) &&  (doc = reader.next()).isPresent()){
                final Document document = doc.get();
                if (Strings.isNullOrEmpty(document.getText())) continue;
                if (counter.incrementAndGet() % interval == 0) LOG.info(counter.get() + " document/s annotated");
                parallelExecutor.submit(() -> {
                    try {
                        String id   = document.getId();
                        String txt  = document.getText().toLowerCase();

                        Map<Integer, List<String>> topicsMap = inferenceService.getTopicNamesByRelevance(txt, annotationRequest.getModelEndpoint());

                        Map<String,Object> data = HashTopicBuilder.from(topicsMap);

                        if (document.getLabels() != null && !document.getLabels().isEmpty()){
                            data.put("labels_t",document.getLabels().stream().sorted().collect(Collectors.joining(" ")));
                        }

                        if (!Strings.isNullOrEmpty(document.getName())){
                            data.put("name_s",document.getName());
                        }

                        writer.update(id,data);
                    } catch (Exception e) {
                        LOG.error("Unexpected error adding new document to corpus",e);
                    }
                });
            }
            parallelExecutor.awaitTermination(5, TimeUnit.MINUTES);
            writer.close();

            mailService.notifyAnnotation(annotationRequest,"Annotation completed");
            LOG.info("Annotation Completed!");
        }catch (Exception e){
            LOG.error("Unexpected error",e);
            mailService.notifyAnnotationError(annotationRequest, e.getMessage());
        }




    }

}
