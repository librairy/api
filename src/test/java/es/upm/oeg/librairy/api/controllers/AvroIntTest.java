package es.upm.oeg.librairy.api.controllers;


import es.upm.oeg.librairy.api.Application;
import es.upm.oeg.librairy.api.facade.AvroClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
@WebAppConfiguration
public class AvroIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(AvroIntTest.class);

    @Test
    public void trainTest() throws InterruptedException, IOException {

        AvroClient client = new AvroClient();


        String host     = "localhost";
        Integer port    = 65112;

        client.open(host,port);


        client.close();
    }

}