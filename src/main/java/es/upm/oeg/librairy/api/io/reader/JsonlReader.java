package es.upm.oeg.librairy.api.io.reader;

import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import org.json.JSONArray;
import org.json.JSONObject;
import es.upm.oeg.librairy.api.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class  JsonlReader extends FileReader{

    private static final Logger LOG = LoggerFactory.getLogger(JsonlReader.class);
    private final Map<String, List<String>> map;
    private final String path;

    private BufferedReader reader;

    public JsonlReader(DataSource dataSource, Boolean zip) throws IOException {
        this.path           = "inputStream";
        this.reader         = new BufferedReader(getInputStream(dataSource.getUrl(), zip));
        this.map            = getParameters(dataSource);
    }


    @Override
    public Optional<Document> next()  {
        String line;
        try{
            if ((line = reader.readLine()) == null){
                reader.close();
                return Optional.empty();
            }
            Document document = new Document();

            JSONObject jsonObject = new JSONObject(line);

            if (map.containsKey("id")) {
                document.setId(retrieve(jsonObject, map.get("id"), false));
            }
            if (map.containsKey("name")) {
                document.setName(retrieve(jsonObject, map.get("name"), false));
            }
            if (map.containsKey("text"))    {
                document.setText(retrieve(jsonObject, map.get("text"), true));
            }
            if (map.containsKey("labels")){
                document.setLabels(Arrays.asList(retrieve(jsonObject, map.get("labels"), false).split(" ")));
            }
            document.setFormat("json");
            return Optional.of(document);

        }catch (Exception e){
            LOG.error("Unexpected error parsing file: " + path,e);
            return Optional.of(new Document());
        }
    }

    private String retrieve(JSONObject jsonObject, List<String> fields, Boolean hardFormat){
        StringBuilder txt = new StringBuilder();
        fields.stream().filter(i -> jsonObject.has(i)).forEach(i -> {

            Object innerObject = jsonObject.get(i);

            if (innerObject instanceof JSONArray){

                JSONArray jsonArray = jsonObject.getJSONArray(i);

                for(int j=0;j<jsonArray.length();j++){
                    String innerText = (String) jsonArray.get(j);
                    txt.append(hardFormat? format(innerText) : StringReader.softFormat(innerText)).append(" ");
                }

            }else{
                txt.append(hardFormat? format(jsonObject.getString(i)) : StringReader.softFormat(jsonObject.getString(i))).append(" ");
            }

        });
        return txt.toString();

    }

}
