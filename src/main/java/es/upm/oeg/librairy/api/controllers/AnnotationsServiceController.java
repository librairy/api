package es.upm.oeg.librairy.api.controllers;

import es.upm.oeg.librairy.api.facade.model.rest.AnnotationsRequest;
import es.upm.oeg.librairy.api.model.Task;
import es.upm.oeg.librairy.api.service.QueueService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import es.upm.oeg.librairy.api.builders.DateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@RestController
@ConditionalOnProperty(value="api.annotations.enabled")
@RequestMapping("/annotations")
@Api(tags="/annotations", description = "save topic distributions")
public class AnnotationsServiceController {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationsServiceController.class);

    @Autowired
    QueueService queueService;

    @PostConstruct
    public void setup(){

    }

    @PreDestroy
    public void destroy(){

    }


    @ApiOperation(value = "annotate documents", nickname = "postAnnotations", response=String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Accepted", response = String.class),
    })
    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<es.upm.oeg.librairy.api.facade.model.rest.Task> create(@RequestBody AnnotationsRequest request)  {
        String date = DateBuilder.now();
        try {
            if (!request.isValid()) return new ResponseEntity(new es.upm.oeg.librairy.api.facade.model.rest.Task(date, "REJECTED", "Bad Request"),HttpStatus.BAD_REQUEST);
            queueService.add(new Task(request));
            return new ResponseEntity(new es.upm.oeg.librairy.api.facade.model.rest.Task(date,"QUEUED","Task created"), HttpStatus.ACCEPTED);
        }catch (RuntimeException e){
            LOG.warn("Process error",e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            LOG.error("IO Error", e);
            return new ResponseEntity(new es.upm.oeg.librairy.api.facade.model.rest.Task(date, "REJECTED", "IO error"),HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
