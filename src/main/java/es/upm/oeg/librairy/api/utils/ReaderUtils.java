package es.upm.oeg.librairy.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class ReaderUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ReaderUtils.class);


    public static BufferedReader from(String path) throws IOException {

        InputStreamReader inputStreamReader;
        if (path.startsWith("http")){
            inputStreamReader = new InputStreamReader(new GZIPInputStream(new URL(path).openStream()));
        }else{
            inputStreamReader = new InputStreamReader(new GZIPInputStream(new FileInputStream(path)));
        }

        return new BufferedReader(inputStreamReader);
    }

    public static BufferedReader from(String path, Boolean gzip) throws IOException {

        InputStream inputStream = path.startsWith("http")? new URL(path).openStream() : new FileInputStream(path);

        return gzip? new BufferedReader(new InputStreamReader(new GZIPInputStream(inputStream))) : new BufferedReader(new InputStreamReader(inputStream));
    }

}
