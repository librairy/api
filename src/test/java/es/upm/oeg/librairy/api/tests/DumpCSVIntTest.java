package es.upm.oeg.librairy.api.tests;

import com.mashape.unirest.http.exceptions.UnirestException;
import es.upm.oeg.librairy.api.executors.ParallelExecutor;
import es.upm.oeg.librairy.api.facade.model.avro.*;
import es.upm.oeg.librairy.api.io.reader.Reader;
import es.upm.oeg.librairy.api.io.reader.ReaderFactory;
import es.upm.oeg.librairy.api.io.writer.Writer;
import es.upm.oeg.librairy.api.io.writer.WriterFactory;
import es.upm.oeg.librairy.api.model.Document;
import es.upm.oeg.librairy.api.service.InferenceService;
import es.upm.oeg.librairy.api.service.LanguageService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class DumpCSVIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(DumpCSVIntTest.class);

    @Test
    public void dump() throws UnirestException, IOException {

        String language = "pt";
        int maxDocs = 200;
        int maxDocsPerCategory = 48;

        LanguageService languageService = new LanguageService();
        languageService.setup();

        DataSource fromSource = DataSource.newBuilder()
                .setName("sample")
                .setCache(false)
                .setFilter("lang_s:"+language+" AND size_i:[10000 TO 50000]")
                .setDataFields(
                        DataFields.newBuilder()
                                .setId("id")
                                .setName("name_t")
                                .setText(Arrays.asList("txt_t"))
                                .setLabels(Arrays.asList("root-labels_t"))
                                .build()
                )
                .setFormat(ReaderFormat.SOLR_CORE)
                .setOffset(0)
                .setSize(-1)
                .setUrl("http://librairy.linkeddata.es/solr/jrc")
                .build();


        Reader reader       = new ReaderFactory().newFrom(fromSource);
        Optional<Document> doc = Optional.empty();


        //Get the file reference
        Path path = Paths.get("/Users/cbadenes/Projects/librairy/public/demo/batch/jrc-"+language+".csv");

        //Use try-with-resource to get auto-closeable writer instance
        try (BufferedWriter writer = Files.newBufferedWriter(path))
        {

            writer.write("id,name,labels,txt\n");
            AtomicInteger counter = new AtomicInteger();

            ConcurrentHashMap<String,Integer> validLabels = new ConcurrentHashMap<>();
            validLabels.put("4361",1);  // communication systems
            validLabels.put("2817",1);   // intellectual property
            validLabels.put("4415",1);  // technology
            validLabels.put("4488",1);  // data processing
            validLabels.put("2524",1);  // pollution

//            validLabels.put("1810",1);  // public contract
//            validLabels.put("3641",1);  // technical regulations
//            validLabels.put("2472",1);  // information policy
//            validLabels.put("5268",1);  // commercial transaction

//            validLabels.put("2914",1);  // research
//            validLabels.put("494",1);  // documentation
//            validLabels.put("668",1);   // education
            //validLabels.put("1405",1);  // information technology industry
//            validLabels.put("4486",1);  // information processing
//            validLabels.put("1426",1);  // computer systems


//            validLabels.put("1700",1); // leisure
            //validLabels.put("1415",1); // pharmacy
//            validLabels.put("2468",1);  // employment policy
//            validLabels.put("2460",1);  // aid policy
//            validLabels.put("2442",1);  // agricultural policy


            //validLabels.put("1321",1);  // tax on comsuption
            //validLabels.put("1268",1);  // nutrition
//            validLabels.put("5130",1);
//            validLabels.put("3160",1);
            //validLabels.put("1432",1);  // offence
//            validLabels.put("539",1);
//            validLabels.put("3160",1);
//            validLabels.put("2166",1);
//            validLabels.put("69",1);
//            validLabels.put("2487",1);

            while( (doc = reader.next()).isPresent()){
                final Document document = doc.get();

                List<String> labels = document.getLabels().stream().filter(l -> validLabels.containsKey(l) && validLabels.get(l) < maxDocsPerCategory).collect(Collectors.toList());
                if (labels.isEmpty()) continue;

                if (document.getText().length() < 10000) continue;

                if (!languageService.getLanguage(document.getText()).equalsIgnoreCase(language)) continue;

                labels.forEach(l -> validLabels.put(l, validLabels.get(l)+1));

                try{
                    StringBuilder row = new StringBuilder();
                    row.append(document.getId()).append(",");
                    row.append("\"").append(document.getName().replaceAll("\"","")).append("\"").append(",");
                    row.append(labels.stream().collect(Collectors.joining(" "))).append(",");
                    row.append("\"").append(document.getText()).append("\"").append("\n");
                    writer.write(row.toString());
                    if (counter.incrementAndGet() >= maxDocs) break;
                }catch (Exception e){
                    LOG.error("Unexpected error",e);
                }


            }
            LOG.info(counter.get() + " documents moved");

            validLabels.entrySet().forEach(e -> LOG.info(e.getKey() + ": " + e.getValue()));
        }




    }
}
