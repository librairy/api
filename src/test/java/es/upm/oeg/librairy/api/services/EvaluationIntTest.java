package es.upm.oeg.librairy.api.services;


import es.upm.oeg.librairy.api.Application;
import es.upm.oeg.librairy.api.facade.AvroClient;
import es.upm.oeg.librairy.api.facade.model.avro.*;
import es.upm.oeg.librairy.api.service.EvaluationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
@WebAppConfiguration
public class EvaluationIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(EvaluationIntTest.class);

    @Autowired
    EvaluationService evaluationService;

    @Test
    public void execute()  {

        DataSource dataSource = DataSource.newBuilder()
                .setFormat(ReaderFormat.SOLR_CORE)
                .setCache(false)
                .setFilter("source_s:jrc && lang_s:en && labels_t:[* TO *]")
                .setDataFields(DataFields.newBuilder()
                        .setId("id")
                        .setLabels(Arrays.asList("root-labels_t"))
                        .setName("name_s")
                        .setText(Arrays.asList("txt_t"))
                        .build())
                .setOffset(0)
                .setSize(100)
                .setUrl("http://librairy.linkeddata.es/solr/jrc")
                .build()
                ;
        DataSink dataSink = DataSink.newBuilder()
                .setFormat(WriterFormat.SOLR_CORE)
                .setUrl("http://librairy.linkeddata.es/solr/test1")
                .build();

        //String model = "http://librairy.linkeddata.es/jrc-en-model";
        String model = "http://localhost:8080";

        Integer refSize = 50;

        List<Integer> intervals = Arrays.asList(3,5,10);
        evaluationService.create(dataSource, dataSink, model, refSize, intervals);
    }

}