package es.upm.oeg.librairy.api.tests;

import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.librairy.api.executors.ParallelExecutor;
import es.upm.oeg.librairy.api.facade.model.avro.*;
import es.upm.oeg.librairy.api.io.reader.Reader;
import es.upm.oeg.librairy.api.io.reader.ReaderFactory;
import es.upm.oeg.librairy.api.io.writer.SolrWriter;
import es.upm.oeg.librairy.api.io.writer.Writer;
import es.upm.oeg.librairy.api.io.writer.WriterFactory;
import es.upm.oeg.librairy.api.model.Document;
import es.upm.oeg.librairy.api.service.InferenceService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class SaveDocumentsIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(SaveDocumentsIntTest.class);

    @Test
    public void save() throws UnirestException, IOException {

        String url = "http://librairy.linkeddata.es/solr/lynx";

        String id = "BOE-A-2003-10715-test";

        BufferedReader br = Files.newBufferedReader(Paths.get("src/test/resources/sample.txt"));
        String txt = br.readLine();
        br.close();


        DataSink dataSink = DataSink.newBuilder()
                .setUrl(url)
                .setFormat( WriterFormat.SOLR_CORE)
                .build();

        Map<String,Object> data = new HashMap<>();
        data.put("txt_t", txt);
        data.put("size_i", txt.length());

        SolrWriter solrWriter = new SolrWriter(dataSink);

        solrWriter.save(id, data);
        solrWriter.close();

        LOG.info("saved!");



    }

}
