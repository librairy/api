package es.upm.oeg.librairy.api.service;

import es.upm.oeg.librairy.api.builders.DateBuilder;
import es.upm.oeg.librairy.api.facade.model.avro.*;
import org.apache.avro.AvroRemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

@Component
public class ApiService implements LibrairyApi{

    private static final Logger LOG = LoggerFactory.getLogger(ApiService.class);

    @Autowired
    QueueService queueService;


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
    public Set getSet(SetRequest setRequest) throws AvroRemoteException {
        //TODO
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public Task cleanCache() throws AvroRemoteException {
        return enqueue(new es.upm.oeg.librairy.api.model.Task() );
    }
}
