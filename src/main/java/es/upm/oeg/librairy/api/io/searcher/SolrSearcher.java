package es.upm.oeg.librairy.api.io.searcher;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.facade.model.avro.Credentials;
import es.upm.oeg.librairy.api.facade.model.avro.DataFields;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.model.QueryDocument;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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


        HttpClientBuilder client = HttpClientBuilder.create();

        Credentials credentials = dataSource.getCredentials();

        if (credentials != null
                && !Strings.isNullOrEmpty(credentials.getUser())
                && !Strings.isNullOrEmpty(credentials.getPassword())){

            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials UserPwdCredentials = new UsernamePasswordCredentials(credentials.getUser(),credentials.getPassword());
            provider.setCredentials(AuthScope.ANY, UserPwdCredentials);

            client.setDefaultCredentialsProvider(provider);
        }

        this.solrClient     = new HttpSolrClient.Builder(dataSource.getUrl()).withHttpClient(client.build()).build();

    }

    @Override
    public List<QueryDocument> getBy(Map<String, Object> queryParams, String filterQuery, Optional<List<String>> fields, Integer max, Boolean combine) {

        SolrQuery refQuery = new SolrQuery();
        try {
            refQuery.setRows(max);
            refQuery.addField("id");
            refQuery.addField("score");
            if (fields.isPresent()){
                fields.get().forEach(f -> refQuery.addField(f));
            }

            String query = "*:*";

            // purge query params



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

            return toQueryDocuments(results, fields);
        } catch (SolrServerException e) {
            LOG.error("Error reading solr core",e);
            return Collections.emptyList();
        } catch (IOException e) {
            LOG.error("Error connecting to solr server",e);
            return Collections.emptyList();
        } catch (Exception e){
            LOG.error("Unexpected query error",e);
            return Collections.emptyList();
        }


    }

    private List<String> combine(Map<String, Object> params){
        List<String> combinedMap = new ArrayList<>();

        List<String> sortIds = params.keySet().stream().sorted((a, b) -> a.compareTo(b)).collect(Collectors.toList());

        int multiplier = 10;
        for(int i=0;i<sortIds.size();i++){
            String key = sortIds.get(i);
            int base = Double.valueOf(multiplier/(i+1)).intValue();
            int boost = Double.valueOf(Math.pow(base, params.size()-i)).intValue();
            Object param = params.get(sortIds.get(i));
            if (param == null) continue;
            for(String t : param.toString().split(" ")){
                combinedMap.add(key+":"+t+"^"+boost);
            }
            for(int j=i-1;j>=0;j--){
                int innerMultiplier = Double.valueOf(multiplier/(i+1)).intValue();
                int innerbase = Double.valueOf(innerMultiplier/(j+1)).intValue();
                boost= Double.valueOf(Math.pow(innerbase, params.size()-j)).intValue();
                for(String t : params.get(sortIds.get(j)).toString().split(" ")){
                    combinedMap.add(key+":"+t+"^"+boost);
                }
            }
        }

        return combinedMap;
    }

    @Override
    public List<QueryDocument> getMoreLikeThis(String id, String queryField, String filterQuery, Optional<List<String>> fields, Integer max) {

        try {
            SolrQuery refQuery = new SolrQuery();
            refQuery.setRows(max);
            refQuery.addField("id");
            refQuery.addField("score");
            if (fields.isPresent()){
                fields.get().forEach(f -> refQuery.addField(f));
            }
            String query = "id:"+id;
            refQuery.setQuery(query);

            if (!Strings.isNullOrEmpty(filterQuery)) refQuery.addFilterQuery(filterQuery);

            refQuery.set("mlt",true);
            refQuery.set("mlt.fl",queryField);
            refQuery.set("mlt.mindf",1);
            refQuery.set("mlt.mintf",1);
            refQuery.set("mlt.minwl",1);
            refQuery.set("mlt.boost",false);
            refQuery.set("mlt.count",max);


            QueryResponse rsp = solrClient.query(refQuery);

            if (rsp.getResults().isEmpty()){
                LOG.info("No found documents by query: " + query + " and filter: " + filterQuery);
                return Collections.emptyList();
            }

            NamedList<SolrDocumentList> results = rsp.getMoreLikeThis();

            SolrDocumentList resultList = results.get(id);
            return toQueryDocuments(resultList, fields);
        } catch (SolrServerException e) {
            LOG.error("Error reading solr core",e);
            return Collections.emptyList();
        } catch (IOException e) {
            LOG.error("Error connecting to solr server",e);
            return Collections.emptyList();
        }

    }

    private List<QueryDocument> toQueryDocuments(SolrDocumentList solrDocumentList, Optional<List<String>> fields){
        List<QueryDocument> queryDocuments = new ArrayList<>();

        for (SolrDocument result : solrDocumentList){
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
    }
}
