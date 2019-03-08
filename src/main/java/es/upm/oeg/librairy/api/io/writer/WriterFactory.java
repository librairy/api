package es.upm.oeg.librairy.api.io.writer;

import es.upm.oeg.librairy.api.facade.model.avro.DataSink;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.facade.model.avro.ReaderFormat;
import es.upm.oeg.librairy.api.facade.model.avro.WriterFormat;
import es.upm.oeg.librairy.api.io.reader.CSVReader;
import es.upm.oeg.librairy.api.io.reader.JsonlReader;
import es.upm.oeg.librairy.api.io.reader.Reader;
import es.upm.oeg.librairy.api.io.reader.SolrReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class WriterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(WriterFactory.class);

    public static Writer newFrom(DataSink dataSink) throws IOException {

        WriterFormat format = dataSink.getFormat();

        switch (format){
            case SOLR_CORE: return new SolrWriter(dataSink);
            default: throw new RuntimeException("No reader found by hardFormat: " + format);
        }

    }

}
