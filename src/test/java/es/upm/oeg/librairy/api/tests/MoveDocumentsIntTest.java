package es.upm.oeg.librairy.api.tests;

import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.librairy.api.executors.ParallelExecutor;
import es.upm.oeg.librairy.api.facade.model.avro.*;
import es.upm.oeg.librairy.api.io.reader.Reader;
import es.upm.oeg.librairy.api.io.reader.ReaderFactory;
import es.upm.oeg.librairy.api.io.writer.Writer;
import es.upm.oeg.librairy.api.io.writer.WriterFactory;
import es.upm.oeg.librairy.api.model.Document;
import es.upm.oeg.librairy.api.service.InferenceService;
import es.upm.oeg.librairy.api.utils.WriterUtils;
import es.upm.oeg.librairy.service.modeler.facade.rest.model.Topic;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class MoveDocumentsIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(MoveDocumentsIntTest.class);

    @Test
    public void execute() throws UnirestException, IOException {

        String model = "http://librairy.linkeddata.es/jrc-en-model";

        DataSource fromSource = DataSource.newBuilder()
                .setCache(false)
                .setFilter("source_s:oo-api")
                .setDataFields(
                        DataFields.newBuilder()
                                .setId("id")
                                .setName("name_s")
                                .setText(Arrays.asList("txt_t"))
                                .setLabels(Arrays.asList("labels_t"))
                                .setExtra(Arrays.asList("size_i","format_s","lang_s","source_s"))
                                .build()
                )
                .setFormat(ReaderFormat.SOLR_CORE)
                .setOffset(0)
                .setSize(-1)
                .setUrl("http://librairy.linkeddata.es/solr/documents")
                .build();


        DataSink dataSink = DataSink.newBuilder()
                .setFormat(WriterFormat.SOLR_CORE)
                .setUrl("http://librairy.linkeddata.es/solr/tbfy")
                .build();

        Reader reader       = ReaderFactory.newFrom(fromSource);
        Optional<Document> doc = Optional.empty();

        Writer writer       = WriterFactory.newFrom(dataSink);

        InferenceService inferenceService = new InferenceService();

        AtomicInteger counter = new AtomicInteger();

        ParallelExecutor executor = new ParallelExecutor();
        while( (doc = reader.next()).isPresent()){
            final Document document = doc.get();
            executor.submit(() -> {
                try{
                    Map<String,Object> data = new HashMap<>();
                    data.put("name_s",document.getName());
                    data.put("txt_t",document.getText());
                    data.put("labels_t",document.getLabels().stream().collect(Collectors.joining(" ")));
                    data.put("size_i", document.getExtraData().get("size_i"));
                    data.put("format_s", document.getExtraData().get("format_s"));
                    data.put("lang_s", document.getExtraData().get("lang_s"));
                    data.put("source_s", document.getExtraData().get("source_s"));

                    if (Integer.valueOf((String) data.get("size_i")) !=0){
                        LOG.info("[" + counter.incrementAndGet()+"] getting topics from : " + document.getId());
                        Map<Integer, List<String>> topics = inferenceService.getTopicsByRelevance(document.getText(), model);
                        topics.entrySet().forEach(entry  -> data.put("topics"+entry.getKey()+"_t", entry.getValue().stream().sorted().collect(Collectors.joining(" "))));
                    }

                    writer.save(document.getId(), data);

                }catch (Exception e){
                    LOG.error("Unexpected error",e);
                }

            });

        }
        executor.awaitTermination(1l, TimeUnit.HOURS);

        writer.close();

        LOG.info(counter.get() + " documents moved");


    }

}