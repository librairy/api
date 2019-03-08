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
                .replaceAll("[.]"," . ")
                .replaceAll("\\P{Print}", "")
                .replaceAll("[^a-zA-Z0-9 .,'_-]", "");

    }

    public static String softFormat(String raw){

        return StringUtils.stripAccents(raw)
                .replaceAll("[.]"," . ")
                .replaceAll("\\P{Print}", "")
                .replaceAll("[^a-zA-Z0-9 .,'_:/-]", "");

    }


    public static void main(String[] args) {


        String text = "rácismo xenofobia.strasbourg millón regular personalidad referir trabajo información programa jurídico podrán";
        LOG.info("-> " + hardFormat(text));

    }

}
