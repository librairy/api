package es.upm.oeg.librairy.api.io.reader;

import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.facade.model.avro.ReaderFormat;
import es.upm.oeg.librairy.api.service.LanguageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class ReaderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ReaderFactory.class);

    @Autowired
    LanguageService languageService;

    public Reader newFrom(DataSource dataSource) throws IOException {

        ReaderFormat format = dataSource.getFormat();

        switch (format){
            case SOLR_CORE: return new SolrReader(dataSource);
            case CSV: return new CSVReader(dataSource, false, languageService);
            case CSV_TAR_GZ: return new CSVReader(dataSource, true, languageService);
            case JSONL: return new JsonlReader(dataSource, false, languageService);
            case JSONL_TAR_GZ: return new JsonlReader(dataSource, true, languageService);
            case PDF:return new DocumentReader(dataSource, languageService,"pdf");
            case DOC:return new DocumentReader(dataSource, languageService,"doc");
            case TXT:return new DocumentReader(dataSource, languageService,"txt");
            default: throw new RuntimeException("No reader found by format: " + format);
        }

    }

}
