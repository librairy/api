package es.upm.oeg.librairy.api.tests;

import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.librairy.api.service.InferenceService;
import es.upm.oeg.librairy.api.utils.WriterUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.librairy.service.modeler.facade.rest.model.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class TopicDistAlignmentIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(TopicDistAlignmentIntTest.class);

    @Test
    public void execute() throws UnirestException, IOException {

        Map<String,String> docs = new HashMap<>();

        docs.put("http://librairy.linkeddata.es/jrc-en-model","Reglamento (CE) no 1087/2005 de la Comisión\\nde 8 de julio de 2005\\npor el que se modifica el Reglamento (CE) no 1210/2003 del Consejo relativo a determinadas restricciones específicas aplicables a las relaciones económicas y financieras con Iraq\\nLA COMISIÓN DE LAS COMUNIDADES EUROPEAS,\\nVisto el Tratado constitutivo de la Comunidad Europea,\\nVisto el Reglamento (CE) no 1210/2003 del Consejo, de 7 de julio de 2003, relativo a determinadas restricciones específicas aplicables a las relaciones económicas y financieras con Iraq y por el que se deroga el Reglamento (CE) no 2465/96 del Consejo [1] y, en particular, su artículo 11, letra b),\\nConsiderando lo siguiente:\\n(1) El anexo IV del Reglamento (CE) no 1210/2003 contiene una lista de personas físicas y jurídicas, entidades y organismos asociados con el régimen del antiguo Presidente Sadam Hussein a los que afecta el bloqueo de fondos y recursos económicos establecido en ese Reglamento.\\n(2) El 22 de junio de 2005, el Comité de Sanciones del Consejo de Seguridad de la ONU decidió modificar la citada lista, en la que figuran Sadam Hussein y otros oficiales de alto rango del régimen iraquí, sus parientes más inmediatos y las entidades poseídas o controladas por estas personas o por otras que actúan en su nombre o bajo su dirección, a quienes se aplica el bloqueo de fondos y recursos económicos. Procede, por tanto, modificar el Reglamento (CE) no 1210/2003 en consecuencia.\\n(3) A fin de velar por la eficacia de las medidas previstas en el presente Reglamento, éste debe entrar en vigor el día de su publicación.\\nHA ADOPTADO EL PRESENTE REGLAMENTO:\\nArtículo 1\\nEl anexo IV del Reglamento (CE) no 1210/2003 se modifica conforme a lo especificado en el anexo del presente Reglamento.\\nArtículo 2\\nEl presente Reglamento entrará en vigor el día de su publicación en el Diario Oficial de la Unión Europea.\\nEl presente Reglamento será obligatorio en todos sus elementos y directamente aplicable en cada Estado miembro.\\nHecho en Bruselas, el 8 de julio de 2005.\\nPor la Comisión\\nEneko Landáburu\\nDirector General de Relaciones Externas\\n[1] DO L 169 de 8.7.2003, p. 6. Reglamento modificado en último lugar por el Reglamento (CE) no 1566/2004 de la Comisión (DO L 285 de 4.9.2004, p. 6).\\n--------------------------------------------------\\nANEXO\\nEl anexo IV del Reglamento (CE) no 1210/2003 se modifica como sigue:\\nSe añadirán las siguientes personas físicas:\\n\\\"Muhammad Yunis Ahmad [alias a) Muhammad Yunis Al-Ahmed, b) Muhammad Yunis Ahmed, c) Muhammad Yunis Ahmad Al-Badrani, d) Muhammad Yunis Ahmed Al-Moali]. Direcciones: a) Al-Dawar Street, Bludan, Siria, b) Damasco, Siria, c) Mosul, Iraq, d) Wadi Al-Hawi, Iraq, e) Dubai, Emiratos Árabes Unidos, f) Al-Hasaka, Siria. Fecha de nacimiento: 1949. Lugar de nacimiento: Al-Mowall, Mosul, Iraq. Nacionalidad: iraquí.\\\"\\n--------------------------------------------------\\n");
        docs.put("http://librairy.linkeddata.es/jrc-es-model","Commission Regulation (EC) No 1087/2005\\nof 8 July 2005\\namending Council Regulation (EC) No 1210/2003 concerning certain specific restrictions on economic and financial relations with Iraq\\nTHE COMMISSION OF THE EUROPEAN COMMUNITIES,\\nHaving regard to the Treaty establishing the European Community,\\nHaving regard to Council Regulation (EC) No 1210/2003 of 7 July 2003 concerning certain specific restrictions on economic and financial relations with Iraq and repealing Regulation (EC) No 2465/96 [1], and in particular Article 11(b) thereof,\\nWhereas:\\n(1) Annex IV to Regulation (EC) No 1210/2003 lists the natural and legal persons, bodies or entities associated with the regime of former President Saddam Hussein covered by the freezing of funds and economic resources under that Regulation.\\n(2) On 22 June 2005, the Sanctions Committee of the UN Security Council decided to amend the list comprising Saddam Hussein and other senior officials of the former Iraqi regime, their immediate family members and the entities owned or controlled by them or by persons acting on their behalf or at their direction, to whom the freezing of funds and economic resources should apply. Therefore, Annex IV should be amended accordingly.\\n(3) In order to ensure that the measures provided for in this Regulation are effective, this Regulation must enter into force immediately,\\nHAS ADOPTED THIS REGULATION:\\nArticle 1\\nAnnex IV to Regulation (EC) No 1210/2003 is hereby amended as set out in the Annex to this Regulation.\\nArticle 2\\nThis Regulation shall enter into force on the day of its publication in the Official Journal of the European Union.\\nThis Regulation shall be binding in its entirety and directly applicable in all Member States.\\nDone at Brussels, 8 July 2005.\\nFor the Commission\\nEneko Landáburu\\nDirector-General for External Relations\\n[1] OJ L 169, 8.7.2003, p. 6. Regulation as last amended by Commission Regulation (EC) No 1566/2004 (OJ L 285, 4.9.2004, p. 6).\\n--------------------------------------------------\\nANNEX\\nAnnex IV to Regulation (EC) No 1210/2003 is amended as follows:\\nThe following natural person shall be added:\\n\\\"Muhammad Yunis Ahmad (alias (a) Muhammad Yunis Al-Ahmed, (b) Muhammad Yunis Ahmed, (c) Muhammad Yunis Ahmad Al-Badrani, (d) Muhammad Yunis Ahmed Al-Moali). Addresses: (a) Al-Dawar Street, Bludan, Syria, (b) Damascus, Syria, (c) Mosul, Iraq, (d) Wadi Al-Hawi, Iraq, (e) Dubai, United Arab Emirates, (f) Al-Hasaka, Syria. Date of birth: 1949. Place of birth: Al-Mowall, Mosul, Iraq. Nationality: Iraqi.\\\"\\n--------------------------------------------------\\n");


        InferenceService inferenceService = new InferenceService();

        Map<String, Map<String,Double>> topicDocList = new HashMap<>();

        for(Map.Entry<String,String> doc : docs.entrySet()){

            Map<Integer,Topic> topicMap     = new HashMap<>();
            Map<String,Double> distMap      = new HashMap<>();

            String model        = doc.getKey();
            String modelName    = StringUtils.substringAfterLast(model,"/");
            String text         = doc.getValue();

            List<Double> topicDist = inferenceService.getTopicsDistribution(text, model);
            for(int i=0;i<topicDist.size();i++){
                if (!topicMap.containsKey(i)){
                    LOG.info("Getting topic info from:  " + model + " by id: " + i);
                    topicMap.put(i,inferenceService.getTopic(model,i));
                }
                distMap.put(topicMap.get(i).getName(),topicDist.get(i));
            }
            topicDocList.put(modelName, distMap);
        }

        BufferedWriter ti = WriterUtils.to("target/topics_index.txt", false);

        List<String> topicIndex = topicDocList.entrySet().stream().map(e -> e.getValue()).map(m -> m.keySet()).reduce((a, b) -> (a.size() > b.size()) ? a : b).get().stream().sorted((a,b) -> a.compareTo(b)).collect(Collectors.toList());
        boolean indexed = false;
        for(Map.Entry<String,Map<String,Double>> td : topicDocList.entrySet()){

            LOG.info("Saving topic distribution from model: " + td.getKey());
            BufferedWriter tw = WriterUtils.to("target/"+td.getKey()+".txt", false);

            Map<String, Double> tdMap = td.getValue();

            for(int i=0;i<topicIndex.size();i++){
                String label = topicIndex.get(i);
                if (!indexed){
                    ti.write(label+"\n");
                }
                Double value = tdMap.containsKey(label)? tdMap.get(label) : 0.0;
                tw.write(value+"\n");
            }
            tw.close();

            indexed = true;


        }

        ti.close();

        LOG.info("Task completed");


    }

}
