package es.upm.oeg.librairy.api.service;

import es.upm.oeg.librairy.api.executors.ParallelExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

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

        final ParallelExecutor executor = new ParallelExecutor();

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

                    executor.submit(() -> {
                        try{
                            indexerService.index(dir.resolve(filename));
                        }catch (Exception e){
                            LOG.error("Unexpected error parsing file: " + filename,e);
                        }
                    });

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
