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
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class UpdateDocumentsIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateDocumentsIntTest.class);

    @Test
    public void update() throws UnirestException, IOException {


        DataSource fromSource = DataSource.newBuilder()
                .setCache(false)
                .setName("jrc")
                .setFilter("")
                .setDataFields(
                        DataFields.newBuilder()
                                .setId("id")
                                .setExtra(Arrays.asList("lang_s"))
                                .build()
                )
                .setFormat(ReaderFormat.SOLR_CORE)
                .setOffset(0)
                .setSize(-1)
                .setUrl("http://librairy.linkeddata.es/solr/jrc")
                .build();


        DataSink dataSink = DataSink.newBuilder()
                .setFormat(WriterFormat.SOLR_CORE)
                .setUrl("http://librairy.linkeddata.es/solr/jrc")
                .build();


        Reader reader       = new ReaderFactory().newFrom(fromSource);
        Optional<Document> doc = Optional.empty();

        Writer writer       = new WriterFactory().newFrom(dataSink);

        AtomicInteger counter = new AtomicInteger();

        ParallelExecutor executor = new ParallelExecutor();
        while( (doc = reader.next()).isPresent()){
            final Document document = doc.get();
            executor.submit(() -> {
                try{
                    String id = document.getId();
                    String lang = StringUtils.substringAfterLast(id,"-");
                    Map<String,Object> data = new HashMap<>();
                    data.put("lang_s", lang);

                    Map<String, String> extraData = document.getExtraData();

                    String docLang = extraData.get("lang_s");

                    if (!extraData.isEmpty() && !docLang.equalsIgnoreCase(lang)){
                        LOG.info("Document: " + id + " updated from: " + docLang + " to " + lang);
                        counter.incrementAndGet();
                        writer.update(document.getId(), data);
                    }


                }catch (Exception e){
                    LOG.error("Unexpected error",e);
                }

            });

        }
        executor.awaitTermination(1l, TimeUnit.HOURS);

        writer.close();

        LOG.info(counter.get() + " documents updated");


    }

}
