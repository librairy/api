package es.upm.oeg.librairy.api.io.reader;

import es.upm.oeg.librairy.api.builders.DateBuilder;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.model.QueryDocument;
import es.upm.oeg.librairy.api.service.LanguageService;
import org.json.JSONArray;
import org.json.JSONObject;
import es.upm.oeg.librairy.api.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class  JsonlReader extends FileReader{

    private static final Logger LOG = LoggerFactory.getLogger(JsonlReader.class);
    private final Map<String, List<String>> map;
    private final String path;
    private final LanguageService languageService;
    private final String file;
    private final String source;

    private BufferedReader reader;

    public JsonlReader(DataSource dataSource, Boolean zip, LanguageService languageService) throws IOException {
        this.path           = "inputStream";
        this.reader         = new BufferedReader(getInputStream(dataSource.getUrl(), zip));
        this.map            = getParameters(dataSource);
        this.languageService = languageService;
        this.file           = dataSource.getUrl();
        this.source         = dataSource.getName();
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

            document.setSource(source);
            document.setFile(file);
            JSONObject jsonObject = new JSONObject(line);

            if (map.containsKey("id")) {
                document.setId(retrieve(jsonObject, map.get("id"), false));
            }
            if (map.containsKey("name")) {
                document.setName(retrieve(jsonObject, map.get("name"), false));
            }
            if (map.containsKey("text"))    {
                document.setText(retrieve(jsonObject, map.get("text"), true));
                String lang = languageService.getLanguage(document.getText().substring(0, Math.min(100, document.getText().length())));
                document.setLang(lang);
            }
            if (map.containsKey("labels")){
                document.setLabels(Arrays.asList(retrieve(jsonObject, map.get("labels"), false).split(" ")));
            }

            if (map.containsKey("extra")){
                Map<String,String> extraData = new HashMap<>();
                for(String extraField : map.get("extra")){
                    String val = retrieve(jsonObject, Arrays.asList(extraField), false);
                    extraData.put(extraField,val);
                }
                document.setExtraData(extraData);
            }
            document.setFormat("json");
            document.setDate(DateBuilder.now());
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
