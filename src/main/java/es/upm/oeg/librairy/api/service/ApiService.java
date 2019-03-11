package es.upm.oeg.librairy.api.service;

import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.librairy.api.builders.DateBuilder;
import es.upm.oeg.librairy.api.facade.model.avro.*;
import org.apache.avro.AvroRemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

@Component
public class ApiService implements LibrairyApi{

    private static final Logger LOG = LoggerFactory.getLogger(ApiService.class);

    @Autowired
    QueueService queueService;

    @Autowired
    ItemService setService;


    @Override
    public Task createTopics(TopicsRequest request) throws AvroRemoteException {
        return enqueue(new es.upm.oeg.librairy.api.model.Task(request) );
    }

    @Override
    public Task createAnnotations(AnnotationsRequest request) throws AvroRemoteException {
        return enqueue(new es.upm.oeg.librairy.api.model.Task(request));
    }

    @Override
    public Task createDocuments(DocumentsRequest request) throws AvroRemoteException {
        return enqueue(new es.upm.oeg.librairy.api.model.Task(request));
    }

    private Task enqueue(es.upm.oeg.librairy.api.model.Task queueTask){
        Task task;
        String date     = DateBuilder.now();
        String status   = "QUEUED";
        String message  = "Task Created";
        try {

            queueService.add(queueTask);
        } catch (Exception e) {
            LOG.error("Error creating topics",e);
            status  = "ERROR";
            message = e.getMessage();
        } finally{
            task = Task.newBuilder().setDate(date).setStatus(status).setMessage(message).build();
        }
        return task;
    }

    @Override
    public List<Item> createItems(ItemsRequest setRequest) throws AvroRemoteException {
        try {
            return setService.create(setRequest);
        } catch (IOException e) {
            throw new AvroRemoteException(e);
        } catch (UnirestException e) {
            throw new AvroRemoteException(e);
        }
    }

    @Override
    public Task cleanCache() throws AvroRemoteException {
        return enqueue(new es.upm.oeg.librairy.api.model.Task() );
    }
}
