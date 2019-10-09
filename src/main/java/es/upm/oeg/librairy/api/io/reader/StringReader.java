package es.upm.oeg.librairy.api.io.reader;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class StringReader {

    private static final Logger LOG = LoggerFactory.getLogger(StringReader.class);

    public static String hardFormat(String raw){

        return StringUtils.stripAccents(raw)
                .replaceAll("[^a-zA-Z0-9 .,'_-]", "")
                .replaceAll("[.]"," . ")
                .replaceAll("\\P{Print}", "");

    }

    public static String softFormat(String raw){

        return StringUtils.stripAccents(raw)
                .replaceAll("[^a-zA-Z0-9 .,'_:/-]", "")
                .replaceAll("[.]"," . ")
                .replaceAll("\\P{Print}", "");

    }

    public static String softLabelFormat(String raw){

        return StringUtils.stripAccents(raw)
                .replaceAll("[^a-zA-Z0-9 .,'_:/-]", "")
                .replaceAll("\\P{Print}", "");

    }


    public static void main(String[] args) {


        String text = "<http://dbpedia.org/resource/Abraham_Lincoln>";
        LOG.info("-> " + softLabelFormat(text));

    }

}
