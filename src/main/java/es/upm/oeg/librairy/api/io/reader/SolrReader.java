package es.upm.oeg.librairy.api.io.reader;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.facade.model.avro.Credentials;
import es.upm.oeg.librairy.api.facade.model.avro.DataFields;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.model.Document;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class SolrReader implements Reader {

    private static final Logger LOG = LoggerFactory.getLogger(SolrReader.class);

    private static final Integer window = 500;

    private final SolrClient solrClient;
    private final String endpoint;
    private final String collection;
    private final String filter;
    private final String idField;
    private final String nameField;
    private final List<String> txtFields;
    private final List<String> labelsFields;
    private final Long maxSize;
    private final List<String> extraFields;

    private String nextCursorMark;
    private SolrQuery solrQuery;
    private String cursorMark;
    private SolrDocumentList solrDocList;
    private AtomicInteger index;
    private Integer counter;
    private Integer offset = 0;


    public SolrReader(DataSource dataSource) throws IOException {


        DataFields fields = dataSource.getDataFields();
        this.idField        = fields.getId();
        this.nameField      = Strings.isNullOrEmpty(fields.getName())? null : fields.getName();
        this.txtFields      = fields.getText();
        this.labelsFields   = (fields.getLabels() != null)? fields.getLabels() : Collections.emptyList();
        this.extraFields    = (fields.getExtra() != null)? fields.getExtra() : Collections.emptyList();
        this.maxSize        = dataSource.getSize();

        this.filter         = Strings.isNullOrEmpty(dataSource.getFilter())? "*:*" : dataSource.getFilter();
        this.endpoint       = StringUtils.substringBeforeLast(dataSource.getUrl(),"/");
        this.collection     = StringUtils.substringAfterLast(dataSource.getUrl(),"/");


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

        this.solrClient     = new HttpSolrClient.Builder(endpoint).withHttpClient(client.build()).build();
        this.counter        = 0;

        this.solrQuery = new SolrQuery();
        solrQuery.setRows(window);
        solrQuery.addField(idField);
        if (!Strings.isNullOrEmpty(nameField)) solrQuery.addField(nameField);
        txtFields.forEach(f -> solrQuery.addField(f));
        labelsFields.forEach(f -> solrQuery.addField(f));
        extraFields.forEach(f -> solrQuery.addField(f));
        solrQuery.setQuery(filter);
        solrQuery.addSort(idField, SolrQuery.ORDER.asc);
        this.nextCursorMark = CursorMarkParams.CURSOR_MARK_START;
        query();

    }

    private void query() throws IOException {
        try{
            this.cursorMark = nextCursorMark;
            solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
            QueryResponse rsp = solrClient.query(collection, solrQuery);
            this.nextCursorMark = rsp.getNextCursorMark();
            this.solrDocList = rsp.getResults();
            this.index = new AtomicInteger();
            this.counter += solrDocList.size();
        }catch (Exception e){
            throw new IOException(e);
        }
    }

    @Override
    public Optional<Document> next() {
        try{
            if (this.offset > this.counter){
                int times = this.offset / this.counter;
                LOG.info("passing " + times + " pages...");
                for(int i=0;i<times;i++){
                    query();
                }
            }

            if (index.get() >= solrDocList.size()) {
                if (index.get() < window){
                    return Optional.empty();
                }
                query();
            }

            if (cursorMark.equals(nextCursorMark)) {
                return Optional.empty();
            }

            if ((maxSize > 0) && (index.get() > maxSize)){
                return Optional.empty();
            }

            SolrDocument solrDoc = solrDocList.get(index.getAndIncrement());

            Document document = new Document();

            String id = (String) solrDoc.get(idField);
            document.setId(id);

            if (!Strings.isNullOrEmpty(nameField)){
                document.setName((String) solrDoc.get(nameField));
            }

            StringBuilder txt = new StringBuilder();
            txtFields.stream().filter(tf -> solrDoc.containsKey(tf)).forEach(tf -> txt.append(StringReader.basicFormat(solrDoc.getFieldValue(tf).toString())).append(" "));
            document.setText(txt.toString());
            document.setFormat("solr_document");

            if (!labelsFields.isEmpty()){
                StringBuilder labels = new StringBuilder();
                labelsFields.stream().filter(tf -> solrDoc.containsKey(tf)).forEach(tf -> labels.append(StringReader.softLabelFormat(solrDoc.getFieldValue(tf).toString())).append(" "));
                document.setLabels(Arrays.asList(labels.toString().split(" ")));
            }

            if (!extraFields.isEmpty()){
                Map<String,String> extraData = new HashMap<>();
                extraFields.stream().filter(tf -> solrDoc.containsKey(tf)).forEach(tf -> extraData.put(tf, StringReader.basicFormat(solrDoc.getFieldValue(tf).toString())));
                document.setExtraData(extraData);
            }

            return Optional.of(document);
        }catch (Exception e){
            LOG.error("Unexpected error on iterated list of solr docs",e);
            if (e instanceof java.lang.IndexOutOfBoundsException) return Optional.empty();
            return Optional.of(new Document());
        }

    }

    @Override
    public void offset(Integer numLines) {
        this.offset = numLines;
    }

}
