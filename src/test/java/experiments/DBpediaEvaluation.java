package experiments;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.io.reader.JsonlReader;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.util.Hash;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class DBpediaEvaluation {

    private static final Logger LOG = LoggerFactory.getLogger(DBpediaEvaluation.class);

    @Test
    public void test() throws IOException, UnirestException {

        HttpResponse<JsonNode> response = Unirest.get("http://librairy.linkeddata.es/dbpedia-model/topics").asJson();

        if (response.getStatus() != 200) throw new RuntimeException("Topic model not available: " + response.getStatus());

        Map<String,Integer> topics = new HashMap<>();

        JSONArray modelTopics = response.getBody().getArray();

        for(int i=0;i<modelTopics.length();i++){
            topics.put(modelTopics.getJSONObject(i).getString("name"),1);
        }

        String filePath = "/Users/cbadenes/Dropbox/Trabajo/Carlos/Academic/DoctoradoIA/experiments/dbpedia-categories/library_topics_2019_10_10.csv";
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String row = "";

        int total = 0;
        int tp = 0;
        int tp0 = 0;
        int tp1 = 0;
        int tp2 = 0;
        int tn = 0;
        int fn = 0;
        int fp = 0;
        int missing = 0;
        int shortText = 0;

        reader.readLine();
        Set missingTopics = new TreeSet<>();
        while(((row = reader.readLine()) != null)){

            total++;
            String[] values = row.split("\t");
            String entity       = values[0];
            String type         = values[1].replaceAll("<","").replaceAll(">","");
            String text         = values[2];
            String categories   = values[3];

            if (text.length() < 50) {
                shortText++;
                continue;
            }

            String jsonString = StringUtils.substringBefore(categories.replaceAll("u'", "'").replaceAll("'","\""),", \"vector")+"}";

            JSONObject jsonCategories = new JSONObject(jsonString);

            JSONArray classifiedTopics = jsonCategories.getJSONArray("topics");

            Map<String,Integer> result = new HashMap<>();

            for(int i=0;i<classifiedTopics.length();i++){
                try{
                    if (i>9) break;
                    JSONObject jsonObject = classifiedTopics.getJSONObject(i);
                    Integer level = jsonObject.getInt("id");
                    String category = jsonObject.getString("name");
                    result.put(category,level);
                }catch (Exception e){
                    LOG.warn("Parsing error: " + jsonString);
                    break;
                }
            }

            if (result.containsKey(type)){
                switch(result.get(type)){
                    case 0: tp0++;
                    break;
                    case 1: tp1++;
                    break;
                    case 2: tp2++;
                    break;
                    default:break;
                }
                tp++;
            }else{
                if (!topics.containsKey(type)){
                    missing++;
                    missingTopics.add(type);
                    continue;
                }
                fp++;
                //LOG.warn("Category not identified");
            }
        }

        LOG.info("Results: ");
        LOG.info("True-Positive: " + tp + "[0:"+tp0+", 1:"+tp1+", 2:"+tp2+"]");
        LOG.info("False-Positive: " + fp);
        LOG.info("Total: " + total);
        LOG.info("Invalid: " + missing);
        LOG.info("Short-Text: " + shortText);
        LOG.info("Accuracy: " + accuracy(tp,fp) + "[0:"+accuracy(tp0,fp) + ", 1:" + accuracy(tp0+tp1,fp) + ", 2:" + accuracy(tp0+tp1+tp2, fp)+ "]");
        LOG.info("Missing Topics: ["+ missingTopics.size()+"] " + missingTopics);

    }


    @Test
    public void readJsonL() throws IOException {

        String url = "https://dumps.wikimedia.org/other/wikidata/20191007.json.gz";

        BufferedReader reader = new BufferedReader(getInputStream(url, true));
        String row = null;
        while((row = reader.readLine()) != null){
            LOG.info(row);
        }
    }

    protected InputStreamReader getInputStream(String url, Boolean zip) throws IOException {

        InputStream inputStream = url.toLowerCase().startsWith("http")? new URL(url).openStream() : new FileInputStream(url);

        InputStreamReader inputStreamReader = zip? new InputStreamReader(new GZIPInputStream(inputStream)) : new InputStreamReader(inputStream);

        return inputStreamReader;

    }

    private Double accuracy(int tp, int fp){
        return (Double.valueOf(tp)) / ( Double.valueOf(tp) + Double.valueOf(fp)  );
    }
}
