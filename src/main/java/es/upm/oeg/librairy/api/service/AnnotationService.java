package es.upm.oeg.librairy.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import es.upm.oeg.librairy.api.executors.ParallelExecutor;
import es.upm.oeg.librairy.api.facade.model.avro.AnnotationsRequest;
import es.upm.oeg.librairy.api.facade.model.avro.DataSink;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.io.reader.Reader;
import es.upm.oeg.librairy.api.io.reader.ReaderFactory;
import es.upm.oeg.librairy.api.io.writer.Writer;
import es.upm.oeg.librairy.api.io.writer.WriterFactory;
import es.upm.oeg.librairy.api.model.Document;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.librairy.service.modeler.facade.rest.model.ClassRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class AnnotationService {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationService.class);

    @Autowired
    MailService mailService;

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


    public void create(AnnotationsRequest annotationRequest){

        try{

            DataSource dataSource = annotationRequest.getDataSource();
            Reader reader = ReaderFactory.newFrom(dataSource);

            DataSink dataSink = annotationRequest.getDataSink();
            Writer writer = WriterFactory.newFrom(dataSink);

            Long maxSize = dataSource.getSize();
            AtomicInteger counter = new AtomicInteger();
            Integer interval = maxSize > 0? maxSize > 100? maxSize.intValue()/100 : 1 : 100;
            Optional<Document> doc;
            reader.offset(dataSource.getOffset().intValue());
            ParallelExecutor parallelExecutor = new ParallelExecutor();
            while(( maxSize<0 || counter.get()<maxSize) &&  (doc = reader.next()).isPresent()){
                final Document document = doc.get();
                if (Strings.isNullOrEmpty(document.getText())) continue;
                if (counter.incrementAndGet() % interval == 0) LOG.info(counter.get() + " document/s annotated");
                parallelExecutor.submit(() -> {
                    try {
                        String id   = document.getId();
                        String txt  = document.getText();

                        Map<Integer, List<String>> topicsMap = new HashMap<>();

                        ClassRequest request = new ClassRequest();
                        request.setText(txt);
                        HttpResponse<JsonNode> response = Unirest
                                .post(annotationRequest.getModelEndpoint() + "/classes")
                                .body(request).asJson();
                        JSONArray topics = response.getBody().getArray();

                        for(int i=0;i<topics.length();i++){
                            JSONObject topic = topics.getJSONObject(i);
                            Integer level   = topic.getInt("id");
                            String tId      = topic.getString("name");

                            if (!topicsMap.containsKey(level)) topicsMap.put(level,new ArrayList<>());

                            topicsMap.get(level).add(tId);
                        }

                        Map<String,Object> data = new HashMap<String, Object>();
                        for(Map.Entry<Integer,List<String>> hashLevel : topicsMap.entrySet()){

                            String fieldName = "topics"+hashLevel.getKey()+"_t";
                            String td        = hashLevel.getValue().stream().map(i -> "t" + i).collect(Collectors.joining(" "));
                            data.put(fieldName, td);
                        }

                        writer.save(id, data);
                    } catch (Exception e) {
                        LOG.error("Unexpected error adding new document to corpus",e);
                    }
                });
            }
            parallelExecutor.awaitTermination(1, TimeUnit.HOURS);
            writer.close();

            mailService.notifyAnnotation(annotationRequest,"Annotation completed");
            LOG.info("Annotation Completed!");
        }catch (Exception e){
            LOG.error("Unexpected error",e);
            mailService.notifyAnnotationError(annotationRequest, e.getMessage());
        }




    }

}
