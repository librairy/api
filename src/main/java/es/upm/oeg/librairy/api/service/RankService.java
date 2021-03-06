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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

@Component
public class RankService {

    private static final Logger LOG = LoggerFactory.getLogger(RankService.class);

    @Autowired
    InferenceService inferenceService;

    @Autowired
    SearcherFactory searcherFactory;


    public String getItemsByHash(ItemsRequest request) throws IOException, UnirestException {

        LOG.debug("Ready to create a rank from: " + request);

        DataSource dataSource   = request.getDataSource();

        Searcher searcher       = searcherFactory.newFrom(dataSource);

        Reference reference     = request.getReference();

        String idField      = dataSource.getDataFields().getId();
        String nameField    = dataSource.getDataFields().getName();


        // get reference hash
        Map<String,Object> hash;
        int offset = 0;
        int max = request.getSize();
        String refId = "";
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
            refId = reference.getDocument().getId();
            max++;
        }else{
            // from inference
            TextReference textReference = reference.getText();
            Map<Integer, List<String>> topicsMap = inferenceService.getTopicNamesByRelevance(textReference.getContent(), textReference.getModel());
            hash = HashTopicBuilder.from(topicsMap);
        }

        // query by hash
        // convert hash into query params

        String rankList = searcher.getRawBy(
                hash,
                dataSource.getFilter(),
                Optional.of(Arrays.asList(nameField)),
                max,
                true,
                request.getDataSource().getOffset());

        return rankList;

    }

    public List<Item> getItemsByLabels(ItemsRequest request) throws IOException, UnirestException {

        LOG.debug("Ready to create sets from: " + request);

        DataSource dataSource   = request.getDataSource();

        Searcher searcher       = searcherFactory.newFrom(dataSource);

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
