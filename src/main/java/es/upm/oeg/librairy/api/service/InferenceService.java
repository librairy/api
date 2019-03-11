package es.upm.oeg.librairy.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.librairy.service.modeler.facade.rest.model.ClassRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class InferenceService {

    private static final Logger LOG = LoggerFactory.getLogger(InferenceService.class);


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

    public Map<Integer,List<String>> getTopicsByRelevance(String text, String model) throws UnirestException {

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
}
