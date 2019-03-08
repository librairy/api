package es.upm.oeg.librairy.api.io.reader;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class CSVReader extends FileReader{

    private static final Logger LOG = LoggerFactory.getLogger(CSVReader.class);
    private String separator;
    private Map<String, List<Integer>> map;
    private String labelSeparator;

    public CSVReader(DataSource dataSource, Boolean zip) throws IOException {
        this.path           = "inputStream";
        this.reader         = new BufferedReader(getInputStream(dataSource.getUrl(), zip));
        this.map            = new HashMap<>();
        getParameters(dataSource).entrySet().forEach(entry -> this.map.put(entry.getKey(), entry.getValue().stream().map(i -> Integer.valueOf(i)).collect(Collectors.toList())));
        this.separator      = Strings.isNullOrEmpty(dataSource.getFilter())? "," : dataSource.getFilter();
        this.labelSeparator = " ";
    }

    @Override
    public Optional<Document> next() {
        String line = null;
        try {
            if ((line = reader.readLine()) == null) {
                reader.close();
                return Optional.empty();
            }

            String[] values = line.split(separator);

            Document document = new Document();

            if (map.containsKey("id")){
                StringBuilder id = new StringBuilder();
                for(Integer i : map.get("id")){
                    id.append(format(values[i]));
                }

                document.setId(id.toString());
            }
            if (map.containsKey("text")) {
                StringBuilder text = new StringBuilder();
                for(Integer i : map.get("text")){
                    text.append(format(values[i])).append(" ");
                }

                document.setText(text.toString());
            }
            if (map.containsKey("labels")){
                StringBuilder labels = new StringBuilder();
                for(Integer i : map.get("labels")){
                    labels.append(StringReader.softFormat(values[i])).append(" ");
                }

                document.setLabels(Arrays.asList(labels.toString().split(labelSeparator)));
            }

            return Optional.of(document);
        } catch (ArrayIndexOutOfBoundsException e){
            LOG.warn("Invalid row("+e.getMessage()+") - [" + line + "]");
            return Optional.of(new Document());
        }catch (Exception e){
            LOG.error("Unexpected error parsing file: " + path,e);
            return Optional.of(new Document());
        }
    }



}
