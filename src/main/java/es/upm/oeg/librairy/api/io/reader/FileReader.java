package es.upm.oeg.librairy.api.io.reader;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.facade.model.avro.DataFields;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public abstract class FileReader implements Reader{

    private static final Logger LOG = LoggerFactory.getLogger(FileReader.class);

    protected String path;

    protected BufferedReader reader;


    protected InputStreamReader getInputStream(String url, Boolean zip) throws IOException {

        InputStream inputStream = url.toLowerCase().startsWith("http")? new URL(url).openStream() : new FileInputStream(url);

        InputStreamReader inputStreamReader = zip? new InputStreamReader(new GZIPInputStream(inputStream)) : new InputStreamReader(inputStream);

        return inputStreamReader;

    }

    protected Map<String,List<String>> getParameters(DataSource dataSource){
        DataFields dataFields = dataSource.getDataFields();

        Map<String,List<String>> parsingMap = new HashMap<>();
        parsingMap.put("id", Arrays.asList(dataFields.getId()));
        parsingMap.put("text", dataFields.getText());
        String nameValue = Strings.isNullOrEmpty(dataFields.getName()) ? dataFields.getId() : dataFields.getName();
        parsingMap.put("name", Arrays.asList(nameValue));
        if ((dataFields.getLabels() != null) && !dataFields.getLabels().isEmpty()) parsingMap.put("labels", dataFields.getLabels());
        return parsingMap;
    }

    protected String format(String text){
        return StringReader.hardFormat(text);
    }

    @Override
    public void offset(Integer numLines) {
        if (numLines>0){
            AtomicInteger counter = new AtomicInteger();
            String line;
            try{
                while (((line = reader.readLine()) != null) && (counter.incrementAndGet() < numLines)){
                }

            }catch (Exception e){
                LOG.error("Unexpected error parsing file: " + path,e);
            }


        }
    }

}
