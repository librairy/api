package es.upm.oeg.librairy.api.model;

import es.upm.oeg.librairy.api.facade.model.avro.AnnotationsRequest;
import es.upm.oeg.librairy.api.facade.model.avro.DocumentsRequest;
import es.upm.oeg.librairy.api.facade.model.avro.TopicsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class Task {

    private static final Logger LOG = LoggerFactory.getLogger(Task.class);
    private TopicsRequest topicsRequest;
    private AnnotationsRequest annotationsRequest;
    private DocumentsRequest documentsRequest;

    public enum Type {
        TOPICS, ANNOTATIONS, DOCUMENTS, CLEAN
    }

    private final Type type;

    public Task(DocumentsRequest request) {
        this.type = Type.DOCUMENTS;
        this.documentsRequest = request;
    }

    public Task() {
        this.type = Type.CLEAN;
    }

    public Task(TopicsRequest topicsRequest) {
        this.type = Type.TOPICS;
        this.topicsRequest = topicsRequest;
    }

    public Task(AnnotationsRequest annotationsRequest) {
        this.type = Type.ANNOTATIONS;
        this.annotationsRequest = annotationsRequest;
    }

    public Type getType() {
        return type;
    }

    public TopicsRequest getTopicsRequest() {
        return topicsRequest;
    }

    public AnnotationsRequest getAnnotationsRequest() {
        return annotationsRequest;
    }

    public DocumentsRequest getDocumentsRequest() {
        return documentsRequest;
    }

    @Override
    public String toString() {
        return "Task{" +
                "topicsRequest=" + topicsRequest +
                ", annotationsRequest=" + annotationsRequest +
                ", documentsRequest=" + documentsRequest +
                ", type=" + type +
                '}';
    }
}
