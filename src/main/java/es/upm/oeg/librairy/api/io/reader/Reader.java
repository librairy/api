package es.upm.oeg.librairy.api.io.reader;

import es.upm.oeg.librairy.api.model.Document;
import es.upm.oeg.librairy.api.model.QueryDocument;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public interface Reader {

    Optional<Document> next();

    void offset(Integer numLines);

}

