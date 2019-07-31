package es.upm.oeg.librairy.api.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class XlsFileParser extends OfficeFileReader implements FileParser {

    private static final Logger LOG = LoggerFactory.getLogger(XlsFileParser.class);

    @Override
    public String suffix() {
        return "xls";
    }
}
