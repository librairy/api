package es.upm.oeg.librairy.api.services;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.librairy.api.facade.model.avro.DataFields;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.facade.model.avro.ReaderFormat;
import es.upm.oeg.librairy.api.io.reader.Reader;
import es.upm.oeg.librairy.api.io.reader.ReaderFactory;
import es.upm.oeg.librairy.api.model.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class AlignTopicIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(AlignTopicIntTest.class);

    @Test
    public void getCommon() throws UnirestException, IOException {

        DataSource datasource = DataSource.newBuilder()
                .setDataFields(DataFields.newBuilder().setId("id").setLabels(Arrays.asList("root-labels_t")).build())
                .setFilter("*:*")
                .setFormat(ReaderFormat.SOLR_CORE)
                .setUrl("http://librairy.linkeddata.es/solr/jrc")
                .build()
                ;
        Reader reader = new ReaderFactory().newFrom(datasource);

        Optional<Document> res = Optional.empty();
        Set<String> labels = new TreeSet<>();
        while((res = reader.next()).isPresent()){
            Document doc = res.get();
            labels.addAll(doc.getLabels());
            LOG.info("added " + doc.getId());
        }


        Map<String, String> m1 = getTopicsAsMap("http://librairy.linkeddata.es/jrc-en-model");
        Map<String, String> m2 = getTopicsAsMap("http://librairy.linkeddata.es/jrc-es-model");

        List<String> commonMap = m1.entrySet().stream().map(e -> e.getKey()).filter(t -> m2.containsKey(t)).collect(Collectors.toList());
        List<String> e1 = m1.entrySet().stream().map(e -> e.getKey()).filter(t -> !m2.containsKey(t)).collect(Collectors.toList());
        List<String> e2 = m2.entrySet().stream().map(e -> e.getKey()).filter(t -> !m1.containsKey(t)).collect(Collectors.toList());

        LOG.info("Total m1 topics: " + m1.size());
        LOG.info("Total m2 topics: " + m2.size());
        LOG.info("Total common topics: " + commonMap.size());
        LOG.info("Total e1 topics: " + e1.size());
        LOG.info("Total e2 topics: " + e2.size());


        LOG.info("e1: " + e1);
        LOG.info("e2: " + e2);


        List<String> ex = labels.stream().filter(l -> !commonMap.contains(l)).collect(Collectors.toList());
        LOG.info("total.size: " + labels.size());
        LOG.info("exclude.size: " + ex.size());
        LOG.info("exclude: " + ex);

    }

    private Map<String,String> getTopicsAsMap(String modelEndpoint) throws UnirestException {
        HttpResponse<JsonNode> r1 = Unirest.get(modelEndpoint+ "/topics").asJson();
        if (r1.getStatus() != 200) return new HashMap<>();

        Map<String,String> topicMap = new HashMap<>();

        JSONArray tl = r1.getBody().getArray();
        for(int i=0;i<tl.length();i++){
            JSONObject t = tl.getJSONObject(i);
            String name         = t.getString("name");
            String description  = t.getString("description");
            topicMap.put(name,description);
            LOG.info("Topic '" + name + "' mapped");
        }

        return topicMap;
    }


}
