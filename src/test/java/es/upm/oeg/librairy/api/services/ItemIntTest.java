package es.upm.oeg.librairy.api.services;


import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.librairy.api.Application;
import es.upm.oeg.librairy.api.facade.model.avro.*;
import es.upm.oeg.librairy.api.io.writer.Writer;
import es.upm.oeg.librairy.api.io.writer.WriterFactory;
import es.upm.oeg.librairy.api.service.AnnotationService;
import es.upm.oeg.librairy.api.service.EvaluationService;
import es.upm.oeg.librairy.api.service.ItemService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.alps.Doc;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
@WebAppConfiguration
public class ItemIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(ItemIntTest.class);

    @Autowired
    ItemService itemService;

    @Test
    public void similar() throws IOException, UnirestException {

        DataSource dataSource = DataSource.newBuilder()
                .setFormat(ReaderFormat.SOLR_CORE)
                .setCache(false)
                .setDataFields(DataFields.newBuilder()
                        .setId("id")
                        .build())
                .setSize(10)
                .setUrl("http://librairy.linkeddata.es/solr/lynx")
                .build()
                ;

        String id = "BOE-A-1995-1049";

        ItemsRequest request = ItemsRequest.newBuilder().
                setReference(Reference.newBuilder().setDocument(DocReference.newBuilder().setId(id).build()).build()).
                setDataSource(dataSource)
                .build();
        List<Item> result = itemService.getItemsByHash(request);

        result.forEach(i -> LOG.info("Item: " + i));
    }

}