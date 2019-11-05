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

        return  raw
                .replaceAll("\n", " ")
                .replaceAll("\r", " ")
                .replaceAll("\b", " ")
                .replaceAll("\"", "'")
                ;

    }

    public static String hardFormat(String raw){

        String t1 = StringEscapeUtils.unescapeHtml4(raw);
        String t2 = StringEscapeUtils.unescapeXml(t1);

        return StringUtils.
                stripAccents(t2)
                .replaceAll("[^a-zA-Z0-9 .,'_-]", "");
                //.replaceAll("[.]",". ");

    }

    public static String softFormat(String raw){

        String t1 = StringEscapeUtils.unescapeHtml4(raw);
        String t2 = StringEscapeUtils.unescapeXml(t1);

        return StringUtils.stripAccents(t2)
                .replaceAll("[^a-zA-Z0-9 .,'_:/-]", "");
                //.replaceAll("[.]",". ");

    }

    public static String softLabelFormat(String raw){

        String t1 = StringEscapeUtils.unescapeHtml4(raw);
        String t2 = StringEscapeUtils.unescapeXml(t1);
        String t3 = t2.trim();

        return StringUtils.stripAccents(t3)
                .replaceAll("[^a-zA-Z0-9 .,'_:/-]", "").
                replaceAll(" ","_");

    }


    public static void main(String[] args) {


        String text = "<http://dbpedia.org/resource/Abraham_Lincoln>";
        String t2 = "Policia y Medio Ambiente";
        String t1 = "Buenos días. Enfrente al portal de Cesareo Alierta, 15, justo en el puente que atraviesa de un lado a otro la avenida, lleva creciendo durante años unahierba/matorral hasta tener unas dimensiones considerables. El matorrral ya tiene casi \"tronco\" y la raíz está desplazando las piezas del puente con el consiguiente peligro de desprendimiento hacia la vía donde circulan los vehículos. Por si es de su consideración.\\r\\nSaludos";
        LOG.info("-> " + softFormat(t2));

    }

}
