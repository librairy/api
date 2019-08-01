package es.upm.oeg.librairy.api.io.writer;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.facade.model.avro.Credentials;
import es.upm.oeg.librairy.api.facade.model.avro.DataSink;
import es.upm.oeg.librairy.api.model.Document;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class SolrWriter implements Writer {

    private static final Logger LOG = LoggerFactory.getLogger(SolrWriter.class);

    private static final Integer window = 500;

    private final SolrClient solrClient;
    private AtomicInteger counter;


    public SolrWriter(DataSink dataSink) throws IOException {


        HttpClientBuilder client = HttpClientBuilder.create();

        Credentials credentials = dataSink.getCredentials();

        if (credentials != null
                && !Strings.isNullOrEmpty(credentials.getUser())
                && !Strings.isNullOrEmpty(credentials.getPassword())){

            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials UserPwdCredentials = new UsernamePasswordCredentials(credentials.getUser(),credentials.getPassword());
            provider.setCredentials(AuthScope.ANY, UserPwdCredentials);

            client.setDefaultCredentialsProvider(provider);
        }

        this.solrClient     = new HttpSolrClient.Builder(dataSink.getUrl()).withHttpClient(client.build()).build();
        this.counter        = new AtomicInteger();
    }


    @Override
    public Boolean update(String id, Map<String, Object> data) {
        Boolean saved = false;
        try{
            SolrInputDocument sd = new SolrInputDocument();
            sd.addField("id",id.replaceAll(" ",""));

            for(Map.Entry<String,Object> entry : data.entrySet()){
                String fieldName = entry.getKey();
                Object td = entry.getValue();
                Map<String,Object> updatedField = new HashMap<>();
                updatedField.put("set", td);
                sd.addField(fieldName, updatedField);
            }

            solrClient.add(sd);

            LOG.debug("[" + counter.incrementAndGet() + "] Document '" + id + "' updated");

            if (counter.get() % 100 == 0){
                LOG.debug("Committing partial annotations["+ this.counter.get() +"]");
                solrClient.commit();
            }

            saved = true;
        }catch (Exception e){
            LOG.error("Unexpected error annotating doc: " + id, e);
        }
        return saved;

    }

    @Override
    public Boolean save(Document document) {
        String id = document.getId();
        Map<String,Object> fields = new HashMap<>();
        if (!document.getLabels().isEmpty()) fields.put("labels_t", document.getLabels().stream().collect(Collectors.joining(" ")));
        fields.put("name_t", document.getName());
        fields.put("file_s", document.getFile());
        fields.put("txt_t",document.getText());
        fields.put("size_i",document.getText().length());
        fields.put("lang_s",document.getLang());
        fields.put("date_dt", document.getDate());
        fields.put("source_s",document.getSource());
        fields.put("format_s",document.getFormat());

        if (!document.getExtraData().isEmpty()){
            document.getExtraData().entrySet().forEach(entry -> fields.put(entry.getKey(),Strings.isNullOrEmpty(entry.getValue())? "" : entry.getValue()));
        }
        save(id, fields);
        return true;
    }

    @Override
    public Boolean save(String id, Map<String, Object> data) {
        Boolean saved = false;
        try{
            SolrInputDocument sd = new SolrInputDocument();
            sd.addField("id",id.replaceAll(" ",""));

            for(String fieldName : data.keySet()){
                sd.addField(fieldName, data.get(fieldName));
            }

            solrClient.add(sd);

            LOG.info("[" + counter.incrementAndGet() + "] Document '" + id + "' saved");

            if (counter.get() % 100 == 0){
                commit();
            }

            saved = true;
        }catch (Exception e){
            LOG.error("Unexpected error annotating doc: " + id, e);
        }
        return saved;

    }

    public void commit(){
        LOG.debug("Committing partial annotations["+ this.counter.get() +"]");
        try {
            solrClient.commit();
        }catch (Exception e){
            LOG.error("Unexpected error ", e);
        }
    }

    @Override
    public Boolean close() {
        Boolean commited = false;
        try{
            LOG.info("Committing partial annotations["+ this.counter.get() +"]");
            solrClient.commit();
            commited = true;
        }catch (Exception e){
            LOG.error("Unexpected error closing collection", e);
        }
        return commited;
    }

    @Override
    public Boolean reset() {
        try {
            solrClient.deleteByQuery("*:*");
            return true;
        } catch (Exception e) {
            LOG.error("Unexpected error deleting index",e);
            return false;
        }
    }

    @Override
    public void offset(Integer numLines) {
        this.counter = new AtomicInteger(numLines);
    }
}
