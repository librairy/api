package es.upm.oeg.librairy.api.service;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.facade.model.avro.Credentials;
import es.upm.oeg.librairy.api.facade.model.avro.DataSink;
import es.upm.oeg.librairy.api.facade.model.avro.WriterFormat;
import es.upm.oeg.librairy.api.io.writer.SolrWriter;
import es.upm.oeg.librairy.api.model.Document;
import es.upm.oeg.librairy.api.parser.FileParser;
import es.upm.oeg.librairy.api.parser.ParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class IndexerService {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerService.class);

    @Autowired
    ParserFactory parserFactory;

    @Value("#{environment['STORAGE_URL']?:'${storage.url}'}")
    String endpoint;

    @Value("#{environment['STORAGE_USER']?:'${storage.user}'}")
    String user;

    @Value("#{environment['STORAGE_PWD']?:'${storage.pwd}'}")
    String password;


    private SolrWriter solrWriter;


    @PostConstruct
    public void setup(){
        String url = !endpoint.startsWith("http")? "http://"+endpoint : endpoint;


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
    }

    public void index(Path file){

        //TODO get topics from text
        Optional<FileParser> parser = parserFactory.getParser(file);

        if (!parser.isPresent()){
            LOG.warn("No parser found for file: " + file);
            return;
        }

        Document document = parser.get().parse(file);
        LOG.info("indexing document: " + document);
//        solrWriter.save(document);

    }
}
