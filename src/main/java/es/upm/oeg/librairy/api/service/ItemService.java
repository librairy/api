package es.upm.oeg.librairy.api.service;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.librairy.api.builders.HashTopicBuilder;
import es.upm.oeg.librairy.api.facade.model.avro.*;
import es.upm.oeg.librairy.api.io.reader.ReaderFactory;
import es.upm.oeg.librairy.api.io.searcher.Searcher;
import es.upm.oeg.librairy.api.io.searcher.SearcherFactory;
import es.upm.oeg.librairy.api.model.QueryDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

@Component
public class ItemService {

    private static final Logger LOG = LoggerFactory.getLogger(ItemService.class);

    @Autowired
    InferenceService inferenceService;

    @Autowired
    SearcherFactory searcherFactory;


    public List<Item> getItemsByHash(ItemsRequest request) throws IOException, UnirestException {

        LOG.debug("Ready to create sets from: " + request);

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

            // Partial Solution to reduce inconsistency
            Map<Integer, List<String>> results = new HashMap<>();
            results.put(0,new ArrayList<>());
            results.put(1,new ArrayList<>());
            results.put(2,new ArrayList<>());
            for (int i=0;i<10;i++){
                Map<Integer, List<String>> topicsMap = inferenceService.getTopicNamesByRelevance(textReference.getContent(), textReference.getModel());
                LOG.debug("TopicsMap: " + topicsMap);
                results.get(0).addAll(topicsMap.get(0));
                results.get(1).addAll(topicsMap.get(1));
                results.get(2).addAll(topicsMap.get(2));
            }
            Map<Integer, List<String>> finalMap = new HashMap<>();
            finalMap.put(0,getMostFrequent(results.get(0)));
            finalMap.put(1,getMostFrequent(results.get(1)));
            finalMap.put(2,getMostFrequent(results.get(2)));
            hash = HashTopicBuilder.from(finalMap);
        }

        // query by hash
        // convert hash into query params
        LOG.debug("Final Hash: " + hash);
        List<QueryDocument> simDocs = searcher.getBy(
                hash,
                dataSource.getFilter(),
                Optional.of(Arrays.asList(nameField)),
                max,
                true);


        final String refIdValue = refId;
        List<Item> items = simDocs.stream().map(qd -> Item.newBuilder().setId(qd.getId()).setName(String.valueOf(qd.getData().get(nameField)).equalsIgnoreCase("null")?null:String.valueOf(qd.getData().get(nameField))).setScore(qd.getScore()).build()).filter(i -> !refIdValue.equalsIgnoreCase(i.getId())).limit(request.getSize()).collect(Collectors.toList());

        return items;

    }

    private List<String> getMostFrequent(List<String> values){
        HashMap<String, Long> freqMap = values.stream().collect(Collectors.groupingBy(Function.identity(), HashMap::new, Collectors.counting()));
        Long maxFreq = 0l;
        for (String k:freqMap.keySet()){
            if (freqMap.get(k) > maxFreq){
                maxFreq = freqMap.get(k);
            }
        }
        final Long threshold = maxFreq/2l;
        return freqMap.entrySet().stream().filter( entry -> entry.getValue() >= threshold).map(entry -> entry.getKey()).collect(Collectors.toList());
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
