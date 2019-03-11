package es.upm.oeg.librairy.api.builders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class HashTopicBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(HashTopicBuilder.class);


    public static List<String> fields(){
        return Arrays.asList("topics0_t","topics1_t","topics2_t");
    }

    public static Map<String, Object> from(Map<Integer,List<String>> topicsMap){
        Map<String,Object> data = new HashMap<String, Object>();
        for(Map.Entry<Integer,List<String>> hashLevel : topicsMap.entrySet()){
            String fieldName = "topics"+hashLevel.getKey()+"_t";
            String td        = hashLevel.getValue().stream().collect(Collectors.joining(" "));
            data.put(fieldName, td);
        }
        return data;
    }


}
