package es.upm.oeg.librairy.api.io;

import es.upm.oeg.librairy.api.model.Document;

import java.util.Optional;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public interface Reader {

    Optional<Document> next();

    void offset(Integer numLines);

}

