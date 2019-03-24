package es.upm.oeg.librairy.api.io.writer;

import es.upm.oeg.librairy.api.facade.model.avro.DataSink;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class SolrWriter implements Writer {

    private static final Logger LOG = LoggerFactory.getLogger(SolrWriter.class);

    private static final Integer window = 500;

    private final SolrClient solrClient;
    private AtomicInteger counter;


    public SolrWriter(DataSink dataSink) throws IOException {
        this.solrClient     = new HttpSolrClient.Builder(dataSink.getUrl()).build();
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
