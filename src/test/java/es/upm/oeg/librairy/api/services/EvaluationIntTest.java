package es.upm.oeg.librairy.api.services;


import es.upm.oeg.librairy.api.Application;
import es.upm.oeg.librairy.api.facade.model.avro.*;
import es.upm.oeg.librairy.api.io.writer.Writer;
import es.upm.oeg.librairy.api.io.writer.WriterFactory;
import es.upm.oeg.librairy.api.service.AnnotationService;
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
import java.util.Collections;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
@WebAppConfiguration
public class EvaluationIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(EvaluationIntTest.class);

    @Autowired
    EvaluationService evaluationService;

    @Autowired
    AnnotationService annotationService;

    @Test
    public void monolingual()  {

        Integer testSize = 1000;

        DataSource dataSource = DataSource.newBuilder()
                .setFormat(ReaderFormat.SOLR_CORE)
                .setCache(false)
                .setFilter("source_s:jrc && lang_s:fr && root-labels_t:[* TO *]")
//                .setFilter("source_s:jrc && lang_s:de && root-labels_t:[* TO *]")
//                .setFilter("source_s:jrc && lang_s:en && root-labels_t:[* TO *]")
//                .setFilter("source_s:jrc && lang_s:es && root-labels_t:[* TO *]")
                .setDataFields(DataFields.newBuilder()
                        .setId("id")
                        .setLabels(Arrays.asList("root-labels_t"))
                        .setName("name_s")
                        .setText(Arrays.asList("txt_t"))
                        .build())
                .setOffset(19000)
                .setSize(testSize)
                .setUrl("http://librairy.linkeddata.es/solr/jrc")
                .build()
                ;
        DataSink dataSink = DataSink.newBuilder()
                .setFormat(WriterFormat.SOLR_CORE)
                .setUrl("http://librairy.linkeddata.es/solr/test1")
                .build();

        String model = "http://tbfy.librairy.linkeddata.es/jrc-fr-model";
//        String model = "http://tbfy.librairy.linkeddata.es/jrc-de-model";
//        String model = "http://tbfy.librairy.linkeddata.es/jrc-en-model";
//        String model = "http://tbfy.librairy.linkeddata.es/jrc-es-model";
//        String model = "http://localhost:8080";

        Integer refSize = testSize;

        List<Integer> intervals = Collections.emptyList();//Arrays.asList(3,5,10);
        evaluationService.create(dataSource, dataSink, model, refSize, intervals, true);
    }

    @Test
    public void multilingual() throws IOException {

        Integer testSize = 1000;

        DataSink dataSink = DataSink.newBuilder()
                .setFormat(WriterFormat.SOLR_CORE)
                .setUrl("http://librairy.linkeddata.es/solr/test1")
                .build();

        Writer writer       = new WriterFactory().newFrom(dataSink);
//        writer.reset();



//        int size = 0;
//        LOG.info("Annotating SPANISH documents..");
//        AnnotationsRequest ar1 = AnnotationsRequest.newBuilder()
//            .setDataSource(DataSource.newBuilder()
//                    .setFormat(ReaderFormat.SOLR_CORE)
//                    .setCache(false)
//                    .setFilter("source_s:jrc && root-labels_t:[* TO *] && lang_s:es")
//                    .setDataFields(DataFields.newBuilder()
//                            .setId("id")
//                            .setLabels(Arrays.asList("root-labels_t"))
//                            .setName("name_s")
//                            .setText(Arrays.asList("txt_t"))
//                            .build())
//                    .setOffset(19000)
//                    .setSize(testSize)
//                    .setUrl("http://librairy.linkeddata.es/solr/jrc")
//                    .build())
//            .setDataSink(dataSink)
//            .setModelEndpoint("http://librairy.linkeddata.es/jrc-es-model")
//            .setContactEmail("internal@mail.com")
//            .build();
//        size++;
//        annotationService.create(ar1);
//
//
//        LOG.info("Annotating ENGLISH documents..");
//        AnnotationsRequest ar2 = AnnotationsRequest.newBuilder()
//                .setDataSource(DataSource.newBuilder()
//                        .setFormat(ReaderFormat.SOLR_CORE)
//                        .setCache(false)
//                        .setFilter("source_s:jrc && root-labels_t:[* TO *] && lang_s:en")
//                        .setDataFields(DataFields.newBuilder()
//                                .setId("id")
//                                .setLabels(Arrays.asList("root-labels_t"))
//                                .setName("name_s")
//                                .setText(Arrays.asList("txt_t"))
//                                .build())
//                        .setOffset(19000)
//                        .setSize(testSize)
//                        .setUrl("http://librairy.linkeddata.es/solr/jrc")
//                        .build())
//                .setDataSink(dataSink)
//                .setModelEndpoint("http://librairy.linkeddata.es/jrc-en-model")
//                .setContactEmail("internal@mail.com")
//                .build();
//        size++;
//        annotationService.create(ar2);
//
//
//        LOG.info("Annotating GERMAN documents..");
//        AnnotationsRequest ar3 = AnnotationsRequest.newBuilder()
//                .setDataSource(DataSource.newBuilder()
//                        .setFormat(ReaderFormat.SOLR_CORE)
//                        .setCache(false)
//                        .setFilter("source_s:jrc && root-labels_t:[* TO *] && lang_s:de")
//                        .setDataFields(DataFields.newBuilder()
//                                .setId("id")
//                                .setLabels(Arrays.asList("root-labels_t"))
//                                .setName("name_s")
//                                .setText(Arrays.asList("txt_t"))
//                                .build())
//                        .setOffset(19000)
//                        .setSize(testSize)
//                        .setUrl("http://librairy.linkeddata.es/solr/jrc")
//                        .build())
//                .setDataSink(dataSink)
//                .setModelEndpoint("http://librairy.linkeddata.es/jrc-de-model")
//                .setContactEmail("internal@mail.com")
//                .build();
//        size++;
//        annotationService.create(ar3);
//
        LOG.info("Annotating FRENCH documents..");
        AnnotationsRequest ar4 = AnnotationsRequest.newBuilder()
                .setDataSource(DataSource.newBuilder()
                        .setFormat(ReaderFormat.SOLR_CORE)
                        .setCache(false)
                        .setFilter("source_s:jrc && root-labels_t:[* TO *] && lang_s:fr")
                        .setDataFields(DataFields.newBuilder()
                                .setId("id")
                                .setLabels(Arrays.asList("root-labels_t"))
                                .setName("name_s")
                                .setText(Arrays.asList("txt_t"))
                                .build())
                        .setOffset(19000)
                        .setSize(testSize)
                        .setUrl("http://librairy.linkeddata.es/solr/jrc")
                        .build())
                .setDataSink(dataSink)
                .setModelEndpoint("http://librairy.linkeddata.es/jrc-fr-model")
                .setContactEmail("internal@mail.com")
                .build();
        annotationService.create(ar4);



        Integer refSize = testSize*4;

        List<Integer> intervals = Collections.emptyList();//Arrays.asList(3,5,10);
        evaluationService.create(ar4.getDataSource(), dataSink, "", refSize, intervals, false);
    }

}