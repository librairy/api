package es.upm.oeg.librairy.api.io.writer;

import es.upm.oeg.librairy.api.model.Document;

import java.util.Map;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public interface Writer {

    Boolean save(Document document);

    Boolean save(String id, Map<String,Object> data);

    Boolean update(String id, Map<String,Object> data);

    Boolean close();

    Boolean reset();

    void offset(Integer numLines);

}

