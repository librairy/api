package es.upm.oeg.librairy.api.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class QueryDocument {

    private static final Logger LOG = LoggerFactory.getLogger(QueryDocument.class);

    private String id;

    private Double score;

    private Map<String,Object> data;

    public QueryDocument() {
        this.data = new HashMap<>();
    }

    public QueryDocument(String id, Double score) {
        this.id = id;
        this.score = score;
        this.data = new HashMap<>();
    }

    public void addData(String key, Object value){
        this.data.put(key, value);
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Map<String, Object> getData() {
        return data;
    }

}
