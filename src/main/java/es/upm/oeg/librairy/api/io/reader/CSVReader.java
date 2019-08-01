package es.upm.oeg.librairy.api.io.reader;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.builders.DateBuilder;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.model.Document;
import es.upm.oeg.librairy.api.model.QueryDocument;
import es.upm.oeg.librairy.api.service.LanguageService;
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
    private final String source;
    private final String file;
    private final LanguageService languageService;
    private String separator;
    private Map<String, List<Integer>> map;
    private String labelSeparator;

    public CSVReader(DataSource dataSource, Boolean zip, LanguageService languageService) throws IOException {
        this.path           = "inputStream";
        this.reader         = new BufferedReader(getInputStream(dataSource.getUrl(), zip));
        this.file           = dataSource.getUrl();
        this.map            = new HashMap<>();
        getParameters(dataSource).entrySet().forEach(entry -> this.map.put(entry.getKey(), entry.getValue().stream().map(i -> Integer.valueOf(i)).collect(Collectors.toList())));
        this.separator      = Strings.isNullOrEmpty(dataSource.getFilter())? "," : dataSource.getFilter();
        this.labelSeparator = " ";
        this.languageService = languageService;
        source = dataSource.getName();
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

            document.setFile(file);
            document.setSource(source);
            if (map.containsKey("id")){
                StringBuilder id = new StringBuilder();
                for(Integer i : map.get("id")){
                    id.append(format(values[i]));
                }

                document.setId(id.toString());
            }

            if (map.containsKey("name")){
                StringBuilder name = new StringBuilder();
                for(Integer i : map.get("name")){
                    name.append(format(values[i]));
                }

                document.setName(name.toString());
            }

            if (map.containsKey("text")) {
                StringBuilder text = new StringBuilder();
                for(Integer i : map.get("text")){
                    text.append(format(values[i])).append(" ");
                }

                document.setText(text.toString());
                String lang = languageService.getLanguage(text.substring(0, Math.min(100, text.length())));
                document.setLang(lang);
            }
            if (map.containsKey("labels")){
                StringBuilder labels = new StringBuilder();
                for(Integer i : map.get("labels")){
                    if (values.length>i) {
                        labels.append(StringReader.softFormat(values[i])).append(" ");
                    }
                }

                document.setLabels(Arrays.asList(labels.toString().split(labelSeparator)));
            }
            if (map.containsKey("extra")){
                Map<String,String> extraData = new HashMap<>();
                for(Integer i : map.get("extra")){
                    extraData.put(String.valueOf(i), StringReader.softFormat(values[i]) );
                }
                document.setExtraData(extraData);
            }
            document.setDate(DateBuilder.now());
            document.setFormat("csv");

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
