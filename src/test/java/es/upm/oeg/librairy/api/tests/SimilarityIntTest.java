package es.upm.oeg.librairy.api.tests;

import com.google.common.collect.Sets;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class SimilarityIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(SimilarityIntTest.class);


    @Test
    public void similarity() throws UnirestException, IOException {

        List<String> x = Arrays.asList("t7_0","t7_1");

        List<String> y = Arrays.asList("t7_0");

        int result = Sets.intersection(Sets.newHashSet(x), Sets.newHashSet(y)).size();

        double score = Double.valueOf(result) / Double.valueOf(Math.max(x.size(), y.size()));

        LOG.info("Sim: " + score);



    }

}
