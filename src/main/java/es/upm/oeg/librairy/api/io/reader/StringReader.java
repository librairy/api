package es.upm.oeg.librairy.api.io.reader;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class StringReader {

    private static final Logger LOG = LoggerFactory.getLogger(StringReader.class);

    public static String basicFormat(String raw){

        return StringUtils.stripAccents(raw)
                .replaceAll("\\P{Print}", "");

    }

    public static String hardFormat(String raw){

        String t1 = StringEscapeUtils.unescapeHtml4(raw);
        String t2 = StringEscapeUtils.unescapeXml(t1);

        return StringUtils.
                stripAccents(t2)
                .replaceAll("[^a-zA-Z0-9 .,'_-]", "")
                .replaceAll("[.]",". ");

    }

    public static String softFormat(String raw){

        String t1 = StringEscapeUtils.unescapeHtml4(raw);
        String t2 = StringEscapeUtils.unescapeXml(t1);

        return StringUtils.stripAccents(t2)
                .replaceAll("[^a-zA-Z0-9 .,'_:/-]", "")
                .replaceAll("[.]",". ");

    }

    public static String softLabelFormat(String raw){

        String t1 = StringEscapeUtils.unescapeHtml4(raw);
        String t2 = StringEscapeUtils.unescapeXml(t1);

        return StringUtils.stripAccents(t2)
                .replaceAll("[^a-zA-Z0-9 .,'_:/-]", "");

    }


    public static void main(String[] args) {


        String text = "<http://dbpedia.org/resource/Abraham_Lincoln>";
        LOG.info("-> " + softLabelFormat(text));

    }

}
