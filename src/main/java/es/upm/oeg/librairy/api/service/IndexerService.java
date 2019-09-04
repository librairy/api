package es.upm.oeg.librairy.api.service;

import com.google.common.base.Strings;
import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.librairy.api.builders.HashTopicBuilder;
import es.upm.oeg.librairy.api.executors.ParallelExecutor;
import es.upm.oeg.librairy.api.facade.model.avro.Credentials;
import es.upm.oeg.librairy.api.facade.model.avro.DataSink;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.facade.model.avro.WriterFormat;
import es.upm.oeg.librairy.api.io.reader.Reader;
import es.upm.oeg.librairy.api.io.reader.ReaderFactory;
import es.upm.oeg.librairy.api.io.writer.SolrWriter;
import es.upm.oeg.librairy.api.model.Document;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class IndexerService {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerService.class);

    @Value("#{environment['STORAGE_URL']?:'${storage.url}'}")
    String endpoint;

    @Value("#{environment['STORAGE_USER']?:'${storage.user}'}")
    String user;

    @Value("#{environment['STORAGE_PWD']?:'${storage.pwd}'}")
    String password;

    @Value("#{environment['MODEL_URLS']?:'${model.urls}'}")
    String models;

    @Value("#{environment['MODEL_BASE']?:'${model.base}'}")
    String modelBase;

    List<String> modelList = new ArrayList<>();

    @Autowired
    InferenceService inferenceService;

    @Autowired
    ReaderFactory readerFactory;

    @Autowired
    LanguageService languageService;

    private Map<String,Map<String,String>> modelSynsets = new HashMap<>();

    private SolrWriter solrWriter;
    private ParallelExecutor executor;
    private Timer timer;
    private AtomicInteger counter;


    @PostConstruct
    public void setup(){
        String url = !endpoint.startsWith("http")? "http://"+endpoint : endpoint;

        this.counter = new AtomicInteger(0);
        Credentials credentials = new Credentials();
        if (!Strings.isNullOrEmpty(user) && !Strings.isNullOrEmpty(password)){
            credentials.setUser(user);
            credentials.setPassword(password);
        }

        DataSink dataSink = new DataSink(url, WriterFormat.SOLR_CORE, credentials);
        try {
            solrWriter = new SolrWriter(dataSink);
        } catch (IOException e) {
            LOG.error("Data Storage is not available: " + e.getMessage());
        }

        if (!Strings.isNullOrEmpty(models)){
            for(String model: models.split(",")){
                StringBuilder endpoint = new StringBuilder();
                if (model.startsWith("/")){
                    endpoint.append(modelBase);
                }
                endpoint.append(model);
                modelList.add(endpoint.toString());
            }
        }

        executor = new ParallelExecutor();

        for(String model: modelList){
            for(String lang: languageService.getAvailableLangs()){
                executor.submit(() -> {
                    try{
                        getTopicsFromModel(model.replace("%%",lang));
                    }catch(Exception e){
                        LOG.error("Error getting topics",e);
                    }
                });
            }
        }
        this.timer = new Timer("Timer");

    }

    private void getTopicsFromModel(String model){
        LOG.info("Loading inference model: " + model + " ...");
        Map<String, String> topicConcepts = inferenceService.getTopics(model);
        modelSynsets.put(model,topicConcepts);
        if (!topicConcepts.isEmpty()) LOG.info("Model '" + model + "' ready!");
    }

    public void index(DataSource dataSource){

        this.executor.submit(() -> {
            try{
                LOG.info("Parsing" + dataSource);
                Reader reader = readerFactory.newFrom(dataSource);

                Optional<Document> doc;
                while((doc = reader.next()).isPresent()){
                    counter.incrementAndGet();
                    final Document document = doc.get();

                    executor.submit(() -> {
                        try{
                            Map<String,String> extraData = new HashMap<>();

                            for(String model: modelList){
                                try {
                                    String modelName = StringUtils.substringBefore(StringUtils.substringBeforeLast(model,"/"),"-");
                                    String modelEndpoint = model.replace("%%", document.getLang());
                                    Map<Integer, List<String>> topicsMap = inferenceService.getTopicNamesByRelevance(document.getText(), modelEndpoint);
                                    Map<String,Object> data = HashTopicBuilder.from(topicsMap);

                                    if (!modelSynsets.containsKey(modelEndpoint)) getTopicsFromModel(modelEndpoint);

                                    data.entrySet().forEach(e -> extraData.put(e.getKey(), modelSynsets.get(modelEndpoint).get(e.getValue().toString())));

                                } catch (UnirestException e) {
                                    LOG.warn("Error getting topic hierarchies from: " + model +" - " + e.getMessage());
                                }
                            }
                            if (!extraData.isEmpty()) document.setExtraData(extraData);

                            solrWriter.save(document);
                            LOG.info(document + " indexed");
                        }catch (Exception e){
                            LOG.error("Error on inner thread indexing doc",e);
                        }finally {
                            counter.decrementAndGet();
                            timer.schedule(new TimerTask() {
                                public void run() {
                                    if (counter.get() <= 0) {
                                        LOG.info("["+counter.get()+"] Task performed on: " + new Date());
                                        solrWriter.commit();
                                    }
                                }
                            }, 1000l);

                        }
                    });


                }

            } catch (IOException e) {
                LOG.warn("Error parsing source: " + dataSource + " - " + e.getMessage());
                counter.decrementAndGet();
            } catch(Exception e){
                LOG.error("Error on indexing thread",e);
                counter.decrementAndGet();
            }
        });

    }
}
