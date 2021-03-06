package es.upm.oeg.librairy.api.service;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.builders.CorpusBuilder;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.facade.model.avro.TopicsRequest;
import org.apache.commons.io.FileUtils;
import es.upm.oeg.librairy.service.modeler.clients.LibrairyNlpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class WorkspaceService {

    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceService.class);

    @Autowired
    LibrairyNlpClient librairyNlpClient;

    @Autowired
    LanguageService languageService;

    @Value("#{environment['OUTPUT_DIR']?:'${output.dir}'}")
    String outputDir;


    public CorpusBuilder create(TopicsRequest request) throws IOException {
        Path workspacePath = getPath(request);
        if (!workspacePath.toFile().getParentFile().exists()) workspacePath.toFile().getParentFile().mkdirs();
        if (!request.getDataSource().getCache() && workspacePath.toFile().exists()) workspacePath.toFile().delete();
        CorpusBuilder corpusBuilder = new CorpusBuilder(workspacePath, librairyNlpClient, languageService);
        if (request.getDataSource().getCache()) corpusBuilder.load();
        return corpusBuilder;
    }

    public void delete(TopicsRequest request){
        try {
            LOG.info("Deleting workspace (non-cached) from: " + request);
            Files.deleteIfExists(getPath(request));
        } catch (IOException e) {
            LOG.warn("Error deleting workspace from: " + request, e);
        }
    }

    public boolean clean(){

        try {
            Path outputPath = Paths.get(outputDir);
            FileUtils.cleanDirectory(outputPath.toFile());
            return true;
        } catch (Exception e) {
            LOG.warn("Error deleting workspaces", e);
            return false;
        }

    }

    private Path getPath(TopicsRequest request){

        // name
        StringBuilder fileNameBuilder = new StringBuilder();
        fileNameBuilder.append(request.getName().replaceAll("\\W+","-"));

        DataSource datasource = request.getDataSource();

        // url
        fileNameBuilder.append("-").append(datasource.getUrl());

        // filter
        if (!Strings.isNullOrEmpty(datasource.getFilter())){
            fileNameBuilder.append("-").append(datasource.getFilter().hashCode());
        }

        // size
        String size = datasource.getSize()<0? "full" : String.valueOf(datasource.getSize());
        fileNameBuilder.append("-").append(size);

        // extension
        String fileName = fileNameBuilder.toString().hashCode() + ".csv.gz";

        return Paths.get(outputDir, fileName);
    }

}
