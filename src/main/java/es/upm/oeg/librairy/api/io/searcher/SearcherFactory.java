package es.upm.oeg.librairy.api.io.searcher;

import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.facade.model.avro.ReaderFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class SearcherFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SearcherFactory.class);

    public static Searcher newFrom(DataSource dataSource) throws IOException {

        ReaderFormat format = dataSource.getFormat();

        switch (format){
            case SOLR_CORE: return new SolrSearcher(dataSource);
            default: throw new RuntimeException("No Searcher found by format: " + format);
        }

    }

}
