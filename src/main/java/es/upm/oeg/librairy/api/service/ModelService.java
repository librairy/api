package es.upm.oeg.librairy.api.service;

import cc.mallet.topics.ModelFactory;
import cc.mallet.topics.ModelParams;
import es.upm.oeg.librairy.api.facade.model.avro.TopicsRequest;
import es.upm.oeg.librairy.api.builders.CorpusBuilder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class ModelService {

    private static final Logger LOG = LoggerFactory.getLogger(ModelService.class);

    @Value("#{environment['RESOURCE_FOLDER']?:'${resource.folder}'}")
    String resourceFolder;

    @Autowired
    ModelFactory modelFactory;

    @Autowired
    MailService mailService;

    public Boolean train(CorpusBuilder corpus, TopicsRequest request) {
        Map<String,String> parameters = request.getParameters() != null? request.getParameters() : new HashMap<>();

        try {
            if (corpus.getNumDocs() <= 0 ){
                LOG.info("Corpus is empty.");
                mailService.notifyModelError(request, "Model not created. Corpus is empty.");
                return false;
            }

            // iterate on retries
            ModelParams bestParameters = null;
            Double bestResult = -Double.MIN_VALUE;
            Boolean regenerate = false;

            Boolean retry = false;

            Integer numTopics = null;
            Integer incrTopics = null;
            Integer maxTopics = null;
            do{
                LOG.info("ready to create a new topic model with parameters: " + parameters);
                ModelParams ldaParameters = new ModelParams(corpus.getFilePath().toFile().getAbsolutePath(), resourceFolder);

                if (parameters.containsKey("topics")){
                    // topics = 10
                    // topics = 10_100:5

                    String topics = parameters.get("topics");
                    if (!topics.contains("_")){
                        numTopics = Integer.valueOf(topics);
                    }else{
                        if (numTopics == null){
                            StringTokenizer tokenizer = new StringTokenizer(topics,":");

                            String topicsExpression = tokenizer.nextToken();
                            Integer firstTopics = Integer.valueOf(StringUtils.substringBefore(topicsExpression, "_"));
                            maxTopics  = Integer.valueOf(StringUtils.substringAfter(topicsExpression, "_"));

                            String incrementExpression = tokenizer.nextToken();
                            incrTopics   = Integer.valueOf(incrementExpression);

                            numTopics = firstTopics-incrTopics;
                        }

                        numTopics += incrTopics;
                        retry = (numTopics < maxTopics);
                    }

                }

                if (parameters.containsKey("alpha"))        ldaParameters.setAlpha(Double.valueOf(parameters.get("alpha")));
                if (parameters.containsKey("beta"))         ldaParameters.setBeta(Double.valueOf(parameters.get("beta")));
                if (numTopics != null)                      ldaParameters.setNumTopics(numTopics);
                if (parameters.containsKey("iterations"))   ldaParameters.setNumIterations(Integer.valueOf(parameters.get("iterations")));
                if (parameters.containsKey("language"))     ldaParameters.setLanguage(parameters.get("language"));
                if (parameters.containsKey("pos"))          ldaParameters.setPos(parameters.get("pos"));
                if (parameters.containsKey("retries"))      ldaParameters.setNumRetries(Integer.valueOf(parameters.get("retries")));
                if (parameters.containsKey("topwords"))     ldaParameters.setNumTopWords(Integer.valueOf(parameters.get("topwords")));
                if (parameters.containsKey("stopwords"))    ldaParameters.setStopwords(Arrays.asList(parameters.get("stopwords").split(" ")));
                if (parameters.containsKey("minfreq"))      ldaParameters.setMinFreq(Integer.valueOf(parameters.get("minfreq")));
                if (parameters.containsKey("maxdocratio"))  ldaParameters.setMaxDocRatio(Double.valueOf(parameters.get("maxdocratio")));
                if (parameters.containsKey("raw"))          ldaParameters.setRaw(Boolean.valueOf(parameters.get("raw")));
                if (parameters.containsKey("inference"))    ldaParameters.setInference(Boolean.valueOf(parameters.get("inference")));
                if (parameters.containsKey("multigrams"))   ldaParameters.setEntities(Boolean.valueOf(parameters.get("multigrams")));
                if (parameters.containsKey("entities"))     ldaParameters.setEntities(Boolean.valueOf(parameters.get("entities")));
                if (parameters.containsKey("seed"))         ldaParameters.setSeed(Integer.valueOf(parameters.get("seed")));
                if (parameters.containsKey("stoplabels"))   ldaParameters.setStoplabels(Arrays.asList(parameters.get("stoplabels").split(" ")));
                if (parameters.containsKey("lowercase"))    ldaParameters.setLowercase(Boolean.valueOf(parameters.get("lowercase").toUpperCase()));
                if (parameters.containsKey("autolabels"))   ldaParameters.setAutolabels(Boolean.valueOf(parameters.get("autolabels").toUpperCase()));
                if (parameters.containsKey("autowords"))    ldaParameters.setAutowords(Boolean.valueOf(parameters.get("autowords").toUpperCase()));


                ldaParameters.setSize(corpus.getNumDocs());

                if (!parameters.containsKey("algorithm")){
                    List<String> labels = request.getDataSource().getDataFields().getLabels();
                    if (labels != null && !labels.isEmpty()) parameters.put("algorithm","llda");
                }

                Double result = modelFactory.train(parameters, ldaParameters);
                if (result >= bestResult || bestParameters == null){
                    bestParameters = ldaParameters;
                    bestResult = result;
                }else{
                    regenerate = true;
                }

            }while(retry);

            if (regenerate){
                LOG.info("Recreating model with the best configuration");
                modelFactory.train(parameters, bestParameters);
            }

            return true;
        } catch (IOException e) {
            LOG.error("Error building a topic model from: " + parameters, e);
            mailService.notifyModelError(request, "Model not created. For details consult your administrator. ");
            return false;
        } catch(ClassCastException e) {
            LOG.error("Error reading parameters from: " + parameters, e);
            mailService.notifyModelError(request, "Model not created. For details consult your administrator. ");
            return false;
        } catch(Exception e){
            LOG.error("Unexpected error during training phase", e);
            mailService.notifyModelError(request, "Model not created. For details consult your administrator. ");
            return false;
        }
    }
}
