package es.upm.oeg.librairy.api.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class ParserFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ParserFactory.class);

    @Autowired
    List<FileParser> parserList;

    private List<FileParser> sortedParserList;

    @PostConstruct
    public void setup(){
        sortedParserList = parserList.stream().sorted((a,b) -> -Integer.valueOf(a.suffix().length()).compareTo(Integer.valueOf(b.suffix().length()))).collect(Collectors.toList());
    }

    public Optional<FileParser> getParser(Path filePath){
        Optional<FileParser> fileParser = Optional.empty();

        if (parserList.isEmpty()) {
            LOG.warn("No file parser registered");
            return Optional.empty();
        }

        String fileNameString = filePath.getFileName().toString();
        for(FileParser parser: sortedParserList){
            if (fileNameString.endsWith(parser.suffix())){
                fileParser = Optional.of(parser);
                break;
            }
        }

        return fileParser;
    }


}
