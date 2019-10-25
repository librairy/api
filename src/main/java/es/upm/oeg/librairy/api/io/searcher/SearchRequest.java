package es.upm.oeg.librairy.api.io.searcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class SearchRequest {

    private static final Logger LOG = LoggerFactory.getLogger(SearchRequest.class);
    private final Map<String, Object> hash;
    private final String filter;
    private final Optional<List<String>> fields;
    private final Integer max;
    private final Boolean combine;


    public SearchRequest(Map<String,Object> hash, String filter, Optional<List<String>> fields,Integer max, Boolean combine) {
        this.hash = hash;
        this.filter = filter;
        this.fields = fields;
        this.max = max;
        this.combine = combine;
    }

    public Map<String, Object> getHash() {
        return hash;
    }

    public String getFilter() {
        return filter;
    }

    public Optional<List<String>> getFields() {
        return fields;
    }

    public Integer getMax() {
        return max;
    }

    public Boolean getCombine() {
        return combine;
    }
}

