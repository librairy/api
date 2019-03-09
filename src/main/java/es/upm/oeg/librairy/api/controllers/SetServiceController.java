package es.upm.oeg.librairy.api.controllers;

import es.upm.oeg.librairy.api.facade.model.avro.LibrairyApi;
import es.upm.oeg.librairy.api.facade.model.rest.Set;
import es.upm.oeg.librairy.api.facade.model.rest.SetRequest;
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

@RestController
@RequestMapping("/sets")
@Api(tags="/sets", description = "group of documents")
public class SetServiceController {

    private static final Logger LOG = LoggerFactory.getLogger(SetServiceController.class);

    @Autowired
    LibrairyApi service;

    @PostConstruct
    public void setup(){

    }

    @PreDestroy
    public void destroy(){

    }


    @ApiOperation(value = "group documents", nickname = "postSet", response=String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accepted", response = String.class),
    })
    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<Set> create(@RequestBody SetRequest request)  {
        try {
            if (!request.isValid()) return new ResponseEntity<Set>(HttpStatus.BAD_REQUEST);

            es.upm.oeg.librairy.api.facade.model.avro.Set set = service.getSet(request);

            return new ResponseEntity(new Set(set), HttpStatus.ACCEPTED);
        } catch (Exception e) {
            LOG.error("IO Error", e);
            return new ResponseEntity<Set>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
