package es.upm.oeg.librairy.api.service;

import es.upm.oeg.librairy.api.facade.model.avro.SetRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

@Component
public class SetService {

    private static final Logger LOG = LoggerFactory.getLogger(SetService.class);

    @Autowired
    MailService mailService;

    @Autowired
    WorkspaceService workspaceService;



    public void create(SetRequest request){
        try{
            LOG.info("Ready to create sets from: " + request);


        }catch (Exception e){
            LOG.error("Error creating topics",e);
        }
    }

}
