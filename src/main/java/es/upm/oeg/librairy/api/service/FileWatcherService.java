package es.upm.oeg.librairy.api.service;

import es.upm.oeg.librairy.api.executors.ParallelExecutor;
import es.upm.oeg.librairy.api.facade.model.avro.DataFields;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.facade.model.avro.ReaderFormat;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class FileWatcherService {

    private static final Logger LOG = LoggerFactory.getLogger(FileWatcherService.class);

    @Value("#{environment['INPUT_FOLDER']?:'${input.folder}'}")
    String resourceFolder;

    @Value("#{environment['INPUT_SOURCE']?:'${input.source}'}")
    String source;

    @Value("#{environment['INPUT_CSV_SEPARATOR']?:'${input.csv.separator}'}")
    String csvSeparator;

    @Value("#{environment['INPUT_CSV_OFFSET']?:'${input.csv.offset}'}")
    String csvOffset;

    @Value("#{environment['INPUT_CSV_ID']?:'${input.csv.id}'}")
    String csvId;

    @Value("#{environment['INPUT_CSV_NAME']?:'${input.csv.name}'}")
    String csvName;

    @Value("#{environment['INPUT_CSV_TEXT']?:'${input.csv.text}'}")
    String csvText;

    @Value("#{environment['INPUT_CSV_LABELS']?:'${input.csv.labels}'}")
    String csvLabels;

    @Value("#{environment['INPUT_JSONL_ID']?:'${input.jsonl.id}'}")
    String jsonlId;

    @Value("#{environment['INPUT_JSONL_NAME']?:'${input.jsonl.name}'}")
    String jsonlName;

    @Value("#{environment['INPUT_JSONL_TEXT']?:'${input.jsonl.text}'}")
    String jsonlText;

    @Value("#{environment['INPUT_JSONL_LABELS']?:'${input.jsonl.labels}'}")
    String jsonlLabels;


    @Autowired
    IndexerService indexerService;


    @PostConstruct
    public void setup() throws IOException {

        WatchService watcher = FileSystems.getDefault().newWatchService();

        Path dir = Paths.get(resourceFolder);

        File dirFile = dir.toFile();

        if (!dirFile.exists()) dirFile.mkdirs();

        try {
            WatchKey key = dir.register(watcher,
                    ENTRY_CREATE
//                    ENTRY_DELETE,
//                    ENTRY_MODIFY
                    );
        } catch (IOException x) {
            System.err.println(x);
        }

        new Thread(() -> {
            LOG.info("Listening at: " + dir.toFile().getAbsolutePath());
            for (;;) {

                // wait for key to be signaled
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException x) {
                    return;
                }

                for (WatchEvent<?> event: key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    // This key is registered only
                    // for ENTRY_CREATE events,
                    // but an OVERFLOW event can
                    // occur regardless if events
                    // are lost or discarded.
                    if (kind == OVERFLOW) {
                        LOG.warn("overflow");
                        continue;
                    }

                    // The filename is the
                    // context of the event.
                    WatchEvent<Path> ev = (WatchEvent<Path>)event;
                    final Path filename = ev.context();

                    try{

                        DataSource dataSource = new DataSource();
                        String path = dir.resolve(filename).toString();
                        dataSource.setUrl(path);
                        dataSource.setName(source);
                        if (path.toLowerCase().endsWith(".pdf")) {
                            dataSource.setFormat(ReaderFormat.PDF);
                        }else if(path.toLowerCase().endsWith(".doc")){
                                dataSource.setFormat(ReaderFormat.DOC);
                        }else if(path.toLowerCase().endsWith(".txt")){
                            dataSource.setFormat(ReaderFormat.TXT);
                        }else if(path.toLowerCase().endsWith(".csv.gz") || path.toLowerCase().endsWith(".tsv.gz")){
                            dataSource.setFormat(ReaderFormat.CSV_TAR_GZ);
                            dataSource.setOffset(Long.valueOf(csvOffset));
                            dataSource.setFilter(csvSeparator);
                            dataSource.setDataFields(DataFields.newBuilder().setId(csvId).setName(csvName).setText(Arrays.asList(csvText)).setLabels(Arrays.asList(csvLabels)).build());
                        }else if(path.toLowerCase().endsWith(".csv") || path.toLowerCase().endsWith(".tsv")){
                            dataSource.setFormat(ReaderFormat.CSV);
                            dataSource.setOffset(Long.valueOf(csvOffset));
                            dataSource.setFilter(csvSeparator);
                            dataSource.setDataFields(DataFields.newBuilder().setId(csvId).setName(csvName).setText(Arrays.asList(csvText)).setLabels(Arrays.asList(csvLabels)).build());
                        }else if(path.toLowerCase().endsWith(".jsonl.gz")){
                            dataSource.setFormat(ReaderFormat.JSONL_TAR_GZ);
                            dataSource.setDataFields(DataFields.newBuilder().setId(jsonlId).setName(jsonlName).setText(Arrays.asList(jsonlText)).setLabels(Arrays.asList(jsonlLabels)).build());
                        }else if(path.toLowerCase().endsWith(".jsonl")){
                            dataSource.setFormat(ReaderFormat.JSONL);
                            dataSource.setDataFields(DataFields.newBuilder().setId(jsonlId).setName(jsonlName).setText(Arrays.asList(jsonlText)).setLabels(Arrays.asList(jsonlLabels)).build());
                        }

                        indexerService.index(dataSource);
                    }catch (Exception e){
                        LOG.error("Unexpected error parsing file: " + filename,e);
                    }


//                        // Verify that the new
//                        //  file is a text file.
//                        try {
//                            // Resolve the filename against the directory.
//                            // If the filename is "test" and the directory is "foo",
//                            // the resolved name is "test/foo".
//                            Path child = dir.resolve(filename);
//                            String contentType = Files.probeContentType(child);
//                            LOG.info("new " + contentType + " detected: " + filename);
//                        } catch (IOException x) {
//                            System.err.println(x);
//                            continue;
//                        }

                }

                // Reset the key -- this step is critical if you want to
                // receive further watch events.  If the key is no longer valid,
                // the directory is inaccessible so exit the loop.
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        }).start();

    }


}
