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

    private static final Integer MAX_HASH_LEVELS = 3;

    private static final Integer MAX_TOPICS_PER_LEVEL   = 10;

    public static List<String> fields(){
        return Arrays.asList("topics0_t","topics1_t","topics2_t");
    }

    public static Map<String, Object> from(Map<Integer,List<String>> topicsMap){
        Map<String,Object> data = new HashMap<String, Object>();
        //for(Map.Entry<Integer,List<String>> hashLevel : topicsMap.entrySet()){
        for (int i = 0; i< MAX_HASH_LEVELS; i++){
            Integer size = topicsMap.get(i) != null? topicsMap.get(i).size() : 0;
            if (!isValid(i,size) || size == 0) continue;
            String fieldName = "topics"+i+"_t";
            String td        = topicsMap.get(i).stream().collect(Collectors.joining(" "));
            data.put(fieldName, td);
        }
        return data;
    }

    private static boolean isValid(Integer hierarchyLevel, Integer numTopics){
        return hierarchyLevel< MAX_HASH_LEVELS && numTopics<=MAX_TOPICS_PER_LEVEL;
    }


}
