package es.upm.oeg.librairy.api.io.searcher;

import es.upm.oeg.librairy.api.model.QueryDocument;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public interface Searcher {

    List<QueryDocument> getBy(Map<String,Object> query, String filter, Optional<List<String>> fields, Integer max, Boolean combine);

    List<QueryDocument> getMoreLikeThis(String id, String queryField, String filter,Optional<List<String>> fields, Integer max);

}
