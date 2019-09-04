package es.upm.oeg.librairy.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.librairy.api.model.Annotation;
import es.upm.oeg.librairy.api.model.AnnotationRequest;
import es.upm.oeg.librairy.api.model.Token;
import org.json.JSONArray;
import org.json.JSONObject;
import es.upm.oeg.librairy.service.modeler.facade.rest.model.ClassRequest;
import es.upm.oeg.librairy.service.modeler.facade.rest.model.InferenceRequest;
import es.upm.oeg.librairy.service.modeler.facade.rest.model.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class InferenceService {

    private static final Logger LOG = LoggerFactory.getLogger(InferenceService.class);

    @Value("#{environment['NLP_ENDPOINT']?:'${nlp.endpoint}'}")
    String nlpEndpoint;

    static{
        Unirest.setObjectMapper(new ObjectMapper() {
            private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper
                    = new com.fasterxml.jackson.databind.ObjectMapper();

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return jacksonObjectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object value) {
                try {
                    return jacksonObjectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Unirest.setTimeouts(20000, 120000);
        Unirest.setDefaultHeader("accept", "application/json");
        Unirest.setDefaultHeader("Content-Type", "application/json");

    }

    public Map<Integer,List<String>> getTopicNamesByRelevance(String text, String model) throws UnirestException {

        Map<Integer, List<String>> topicsMap = new HashMap<>();

        ClassRequest request = new ClassRequest();
        request.setText(text);
        HttpResponse<JsonNode> response = Unirest
                .post(model + "/classes")
                .body(request).asJson();
        JSONArray topics = response.getBody().getArray();

        for(int i=0;i<topics.length();i++){
            JSONObject topic = topics.getJSONObject(i);
            Integer level   = topic.getInt("id");
            String tId      = topic.getString("name");

            if (!topicsMap.containsKey(level)) topicsMap.put(level,new ArrayList<>());

            topicsMap.get(level).add(tId);
        }

        return topicsMap;

    }

    public Map<String,String> getTopics(String model){
        try {

            HttpResponse<JsonNode> result = Unirest.get(model+"/topics")
                    .asJson();

            Map<String,String> topics = new HashMap<>();

            JSONArray li = result.getBody().getArray();

            for(int i=0;i<li.length();i++){
                JSONObject json = li.getJSONObject(i);
                int id          = json.getInt("id");
                String name     = json.getString("name");
                String description = json.getString("description");
//                topics.put(name, Arrays.asList(description.split(",")));


                AnnotationRequest req = new AnnotationRequest();
                req.setFilter(Arrays.asList("NOUN","VERB","ADJECTIVE"));
//                req.setLang(lang);
                req.setMultigrams(false);
                req.setReferences(false);
                req.setSynset(true);
                req.setText(description.replace(",",", ").toLowerCase());
                List<Annotation> annotations = getAnnotations(req);
                String synset = annotations.stream().flatMap(a -> a.getSynset().stream().map(s -> StringUtils.substringBefore(s, "."))).distinct().limit(10).collect(Collectors.joining(" "));
                topics.put(name, synset);

            }
            return topics;

        } catch (Exception e) {
            LOG.error("Unexpected API error: "+ e.getMessage());
            return new HashMap<>();
        }
    }

    public List<Double> getTopicsDistribution(String text, String model) throws UnirestException {


        InferenceRequest request = new InferenceRequest();
        request.setText(text);
        request.setTopics(false);
        HttpResponse<JsonNode> response = Unirest
                .post(model + "/inferences")
                .body(request).asJson();
        JSONArray topics = response.getBody().getObject().getJSONArray("vector");

        List<Double> topicDist = new ArrayList<>();
        for(int i=0;i<topics.length();i++){
            Double dist  = topics.getDouble(i);
            topicDist.add(dist);
        }

        return topicDist;
    }

    public Topic getTopic(String model, Integer index) throws UnirestException {

        HttpResponse<JsonNode> response = Unirest
                .get(model + "/topics/" + index)
                .asJson();

        Topic topic = new Topic();

        if (response.getStatus() != 200) {
            LOG.warn("Error on request: " + response);
            return topic;
        }

        JSONObject json = response.getBody().getObject();

        topic.setId(index);
        topic.setName(json.getString("name"));
        topic.setEntropy(json.getDouble("entropy"));
        topic.setDescription(json.getString("description"));

        return topic;

    }

    public List<Annotation> getAnnotations (AnnotationRequest request){
        try {

            if (Strings.isNullOrEmpty(request.getText())) return Collections.emptyList();

            List<Annotation> annotations = new ArrayList<>();
            String endpoint = nlpEndpoint+"/annotations";
            if (!endpoint.toLowerCase().startsWith("http")){
                endpoint = "http://"+endpoint;
            }
            HttpResponse<JsonNode> result = Unirest.post(endpoint)
                    .body(request)
                    .asJson();

            JSONArray annotationList = result.getBody().getObject().getJSONArray("annotatedText");
            for(int i=0;i<annotationList.length();i++){
                JSONObject json = annotationList.getJSONObject(i);
                Annotation annotation = new Annotation();

                JSONObject tokenJson = json.getJSONObject("token");
                Token token = new Token();
                token.setLemma(tokenJson.getString("lemma"));
                token.setTarget(tokenJson.getString("target"));
                token.setPos(tokenJson.getString("pos"));
                annotation.setToken(token);

                annotation.setOffset(json.getInt("offset"));

                List<String> synset = new ArrayList<>();
                JSONArray synsetList = json.getJSONArray("synset");
                for(int j=0;j<synsetList.length();j++){
                    String val = synsetList.getString(j);
                    synset.add(val);
                }

                annotation.setSynset(synset);

                annotations.add(annotation);

            }

            return annotations;

        } catch (Exception e) {
            LOG.error("Unexpected API error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

}
