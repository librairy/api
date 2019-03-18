package es.upm.oeg.librairy.api.controllers;

import es.upm.oeg.librairy.api.builders.DateBuilder;
import es.upm.oeg.librairy.api.facade.model.avro.LibrairyApi;
import es.upm.oeg.librairy.api.facade.model.rest.TopicsRequest;
import es.upm.oeg.librairy.api.model.Task;
import es.upm.oeg.librairy.api.service.QueueService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;

@RestController
@RequestMapping("/topics")
@Api(tags="/topics", description = "create topic models")
public class TopicsServiceController {

    private static final Logger LOG = LoggerFactory.getLogger(TopicsServiceController.class);


    @Autowired
    LibrairyApi service;

    @PostConstruct
    public void setup(){

    }

    @PreDestroy
    public void destroy(){

    }


    @ApiOperation(value = "create topic models", nickname = "postTopics", response=String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Accepted", response = String.class),
    })
    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<es.upm.oeg.librairy.api.facade.model.rest.Task> create(@RequestBody TopicsRequest request)  {
        String date = DateBuilder.now();
        try {
            if (!request.isValid()) return new ResponseEntity(new es.upm.oeg.librairy.api.facade.model.rest.Task(date, "REJECTED", "Bad Request"),HttpStatus.BAD_REQUEST);

            if (request.get("parameters") == null) request.setParameters(new HashMap<>());

            service.createTopics(request);

            return new ResponseEntity(new es.upm.oeg.librairy.api.facade.model.rest.Task(date,"QUEUED","Task created"), HttpStatus.ACCEPTED);
        } catch (Exception e) {
            LOG.error("IO Error", e);
            return new ResponseEntity(new es.upm.oeg.librairy.api.facade.model.rest.Task(date, "REJECTED", "IO error"),HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
