package es.upm.oeg.librairy.api.io.searcher;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.facade.model.avro.DataFields;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.model.QueryDocument;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class SolrSearcher implements Searcher {

    private static final Logger LOG = LoggerFactory.getLogger(SolrSearcher.class);

    private final SolrClient solrClient;
    private final String filter;
    private final String idField;
    private final String nameField;
    private final List<String> txtFields;
    private final List<String> labelsFields;
    private final Long maxSize;


    public SolrSearcher(DataSource dataSource) throws IOException {


        DataFields fields = dataSource.getDataFields();
        this.idField        = fields.getId();
        this.nameField      = Strings.isNullOrEmpty(fields.getName())? null : fields.getName();
        this.txtFields      = (fields.getText() != null)? fields.getText() : Collections.emptyList();
        this.labelsFields   = (fields.getLabels() != null)? fields.getLabels() : Collections.emptyList();
        this.maxSize        = dataSource.getSize();

        this.filter         = Strings.isNullOrEmpty(dataSource.getFilter())? "*:*" : dataSource.getFilter();

        this.solrClient     = new HttpSolrClient.Builder(dataSource.getUrl()).build();

    }

    @Override
    public List<QueryDocument> getBy(Map<String, Object> queryParams, String filterQuery, Optional<List<String>> fields, Integer max, Boolean combine) {

        try {
            SolrQuery refQuery = new SolrQuery();
            refQuery.setRows(max);
            refQuery.addField("id");
            refQuery.addField("score");
            if (fields.isPresent()){
                fields.get().forEach(f -> refQuery.addField(f));
            }

            String query = "*:*";


            if (!queryParams.isEmpty()){
                List<String> params = combine? combine(queryParams) : queryParams.entrySet().stream().map(e -> e.getKey()+":"+e.getValue()).collect(Collectors.toList());
                query = params.stream().collect(Collectors.joining(" OR "));
            }

            refQuery.setQuery(query);

            if (!Strings.isNullOrEmpty(filterQuery)) refQuery.addFilterQuery(filterQuery);

            QueryResponse rsp = solrClient.query(refQuery);

            if (rsp.getResults().isEmpty()){
                LOG.info("No found documents by query: " + query + " and filter: " + filterQuery);
                return Collections.emptyList();
            }

            SolrDocumentList results = rsp.getResults();

            List<QueryDocument> queryDocuments = new ArrayList<>();

            for (SolrDocument result : results){
                QueryDocument queryDocument = new QueryDocument();
                queryDocument.setId((String)result.getFieldValue("id"));
                queryDocument.setScore(((Float)result.getFieldValue("score")).doubleValue());
                Map<String,Object> data = new HashMap<>();
                if (fields.isPresent()){
                    for(String field : fields.get()){
                        data.put(field, result.getFieldValue(field));
                    }
                }
                queryDocument.setData(data);
                queryDocuments.add(queryDocument);
            }

            return queryDocuments;
        } catch (SolrServerException e) {
            LOG.error("Error reading solr core",e);
            return Collections.emptyList();
        } catch (IOException e) {
            LOG.error("Error connecting to solr server",e);
            return Collections.emptyList();
        }


    }

    private List<String> combine(Map<String, Object> params){
        List<String> combinedMap = new ArrayList<>();

        List<String> sortIds = params.keySet().stream().sorted((a, b) -> a.compareTo(b)).collect(Collectors.toList());

        for(int i=0;i<sortIds.size();i++){
            String key = sortIds.get(i);
            int boost = params.size();
            for(String t : params.get(sortIds.get(i)).toString().split(" ")){
                combinedMap.add(key+":"+t+"^"+boost);
            }
            for(int j=i-1;j>=0;j--){
                boost--;
                for(String t : params.get(sortIds.get(j)).toString().split(" ")){
                    combinedMap.add(key+":"+t+"^"+boost);
                }
            }
        }

        return combinedMap;
    }
}
