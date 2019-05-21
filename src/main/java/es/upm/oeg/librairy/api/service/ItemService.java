package es.upm.oeg.librairy.api.service;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.librairy.api.builders.HashTopicBuilder;
import es.upm.oeg.librairy.api.facade.model.avro.*;
import es.upm.oeg.librairy.api.io.searcher.Searcher;
import es.upm.oeg.librairy.api.io.searcher.SearcherFactory;
import es.upm.oeg.librairy.api.model.QueryDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

@Component
public class ItemService {

    private static final Logger LOG = LoggerFactory.getLogger(ItemService.class);

    @Autowired
    InferenceService inferenceService;


    public List<Item> getItemsByHash(ItemsRequest request) throws IOException, UnirestException {

        LOG.debug("Ready to create sets from: " + request);

        DataSource dataSource   = request.getDataSource();

        Searcher searcher       = SearcherFactory.newFrom(dataSource);

        Reference reference     = request.getReference();

        String idField      = dataSource.getDataFields().getId();
        String nameField    = dataSource.getDataFields().getName();


        // get reference hash
        Map<String,Object> hash;
        int offset = 0;
        int max = request.getSize();
        if (reference.getDocument() != null){
            // from existing repo
            List<QueryDocument> doc = searcher.getBy(
                    ImmutableMap.of(idField, reference.getDocument().getId()),
                    "",
                    Optional.of(HashTopicBuilder.fields()),
                    1,
                    false);
            if (doc.isEmpty()) throw new RuntimeException("Document not found by id: " + reference.getDocument().getId());
            if (doc.size()>1) throw new RuntimeException("More than one document by id: " + reference.getDocument().getId());
            hash = doc.get(0).getData();
            offset = 1;
            max++;
        }else{
            // from inference
            TextReference textReference = reference.getText();
            Map<Integer, List<String>> topicsMap = inferenceService.getTopicsByRelevance(textReference.getContent(), textReference.getModel());
            hash = HashTopicBuilder.from(topicsMap);
        }

        // query by hash
        // convert hash into query params

        List<QueryDocument> simDocs = searcher.getBy(
                hash,
                dataSource.getFilter(),
                Optional.of(Arrays.asList(nameField)),
                max,
                true);


        List<Item> items = simDocs.stream().skip(offset).map(qd -> Item.newBuilder().setId(qd.getId()).setName(String.valueOf(qd.getData().get(nameField)).equalsIgnoreCase("null")?null:String.valueOf(qd.getData().get(nameField))).setScore(qd.getScore()).build()).collect(Collectors.toList());

        return items;

    }

    public List<Item> getItemsByLabels(ItemsRequest request) throws IOException, UnirestException {

        LOG.debug("Ready to create sets from: " + request);

        DataSource dataSource   = request.getDataSource();

        Searcher searcher       = SearcherFactory.newFrom(dataSource);

        if (request.getReference().getDocument() == null || Strings.isNullOrEmpty(request.getReference().getDocument().getId())) throw new RuntimeException("Document ID is empty");

        Reference reference     = request.getReference();

        String id = reference.getDocument().getId();

        String idField      = dataSource.getDataFields().getId();
        String nameField    = dataSource.getDataFields().getName();


        List<QueryDocument> simDocs = searcher.getMoreLikeThis(
                id,
                "labels_t",
                dataSource.getFilter(),
                Optional.of(Arrays.asList(nameField)),
                request.getSize());

        List<Item> items = simDocs.stream().map(qd -> Item.newBuilder().setId(qd.getId()).setName(String.valueOf(qd.getData().get(nameField))).setScore(qd.getScore()).build()).collect(Collectors.toList());

        return items;

    }

}
