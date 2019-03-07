package es.upm.oeg.librairy.api.controllers;

import es.upm.oeg.librairy.api.facade.AvroServer;
import es.upm.oeg.librairy.api.facade.model.avro.LibrairyApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

@Component
public class LearnerAvroController {

    @Autowired
    LibrairyApi service;

    @Value("#{environment['LEARNER_AVRO_PORT']?:${learner.avro.port}}")
    Integer port;

    String host = "0.0.0.0";

    private AvroServer server;

    @PostConstruct
    public void setup() throws IOException {
        server = new AvroServer(service);
        server.open(host,port);
    }

    @PreDestroy
    public void destroy(){
        if (server != null) server.close();
    }

}
