package es.upm.oeg.librairy.api.io.writer;

import java.util.Map;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public interface Writer {

    Boolean save(String id, Map<String,Object> data);

    Boolean update(String id, Map<String,Object> data);

    Boolean close();

    void offset(Integer numLines);

}

