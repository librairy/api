package es.upm.oeg.librairy.api.controllers;

import es.upm.oeg.librairy.api.facade.model.avro.LibrairyApi;
import es.upm.oeg.librairy.api.facade.model.rest.Item;
import es.upm.oeg.librairy.api.facade.model.rest.ItemsRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@ConditionalOnProperty(value="api.items.enabled")
@RequestMapping("/ranks")
@Api(tags="/ranks", description = "sorted list of documents")
public class RanksServiceController {

    private static final Logger LOG = LoggerFactory.getLogger(RanksServiceController.class);

    @Autowired
    LibrairyApi service;

    @PostConstruct
    public void setup(){

    }

    @PreDestroy
    public void destroy(){

    }


    @ApiOperation(value = "document suggestions", nickname = "postItems", response=Item.class, responseContainer = "list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok", response = Item.class, responseContainer = "list"),
    })
    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> create(@RequestBody ItemsRequest request,
                                         @RequestHeader HttpHeaders headers)  {
        try {
            if (!request.isValid()) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

            String rank = service.createRanks(request);

            return new ResponseEntity(rank, HttpStatus.OK);
        }catch (RuntimeException e){
            LOG.warn("Process error",e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            LOG.error("Unexpected Error", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
