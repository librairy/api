package cc.mallet.topics;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import es.upm.oeg.librairy.api.builders.InstanceBuilder;
import es.upm.oeg.librairy.api.builders.MailBuilder;
import es.upm.oeg.librairy.api.model.TopicReport;
import es.upm.oeg.librairy.api.service.StoplabelService;
import es.upm.oeg.librairy.api.service.StopwordService;
import es.upm.oeg.librairy.service.modeler.facade.model.TopicWord;
import es.upm.oeg.librairy.service.modeler.service.TopicsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class LDALauncher {

    private static final Logger LOG = LoggerFactory.getLogger(LDALauncher.class);

    @Value("#{environment['OUTPUT_DIR']?:'${output.dir}'}")
    String outputDir;

    @Autowired
    InstanceBuilder instanceBuilder;

    @Autowired
    ModelLauncher modelLauncher;

    @Autowired
    MailBuilder mailBuilder;

    @Autowired
    TopicsService topicsService;

    @Autowired
    StoplabelService stoplabelService;

    @Autowired
    StopwordService stopwordService;

    public Double train(ModelParams parameters, String email) throws IOException {


        Double result = -100.0; //log-likelihood

        File outputDirFile = Paths.get(parameters.getOutputDir()).toFile();
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs();
        }


        ParallelTopicModel model = null;
        Pipe pipe = null;
        Boolean isReady = false;

        List<String> stoplabels = new ArrayList<>(parameters.getStoplabels());
        List<String> stopwords  = new ArrayList<>(parameters.getStopwords());

        Boolean autolabels      = parameters.getAutolabels();
        Boolean autowords       = parameters.getAutowords();

        try{
            int swRetries = 0;
            do{

                    TopicReport report = build(parameters);

                    if (report.isEmpty()) return result;

                    model = report.getModel();

                    pipe = report.getPipe();

                    if ((!autolabels) && (!autowords)) break;

                    Map<Integer, List<TopicWord>> topWords = topicsService.getTopWords(model, parameters.getNumTopWords());

                    Map<String,List<String>> topics = new HashMap<>();

                    for (Map.Entry<Integer,List<TopicWord>> entry : topWords.entrySet()){
                        Label name = model.topicAlphabet.lookupLabel(entry.getKey().intValue());
                        List<String> words = entry.getValue().stream().map(tw -> tw.getValue()).collect(Collectors.toList());
                        String nameL = name.toString();
                        topics.put(nameL, words);

                    }

                    List<String> stoplabelCandidateList = Collections.emptyList();

                    if (autolabels) {
                        stoplabelCandidateList = stoplabelService.detect(topics, parameters.getNumTopWords());
                    }


                    if (stoplabelCandidateList.isEmpty() && autowords){

                        List<String> stopWordCandidateList = stopwordService.detect(topics);

                        if (stopWordCandidateList.isEmpty()) break;

                        swRetries = (stopWordCandidateList.size() == 1)? swRetries+1 : 0;

                        if (swRetries >= 20) break; // break-point

                        LOG.warn("Stop-words detected: " + stopWordCandidateList.size());
                        LOG.info("Total stop-word list size: " + stopwords.size());

                        stopwords.addAll(stopWordCandidateList);
                        parameters.setStopwords(stopwords);
                        continue;

                    }


                    LOG.warn("Invalid Topics detected: " + stoplabelCandidateList.size());
                    LOG.info("Total stop-label list size: " + stoplabels.size());
                    swRetries = 0;

                    stoplabels.addAll(stoplabelCandidateList);
                    parameters.setStoplabels(stoplabels);



            }while(!isReady);

            LOG.info("saving model to disk .. ");
            result = modelLauncher.saveModel(parameters.getOutputDir(), "lda", parameters, model, parameters.getNumTopWords(), pipe);

            LOG.info(" Model created and saved successfully");
        }catch (Exception e){
            LOG.error("Error creating topic model", e);
        } finally {
            return result;
        }
    }


    private TopicReport build(ModelParams parameters) throws IOException {
        int numTopics       = parameters.getNumTopics();
        Double alpha        = parameters.getAlpha();
        Double beta         = parameters.getBeta();
        Integer numTopWords = parameters.getNumTopWords();
        String pos          = parameters.getPos();
        Integer maxRetries  = parameters.getNumRetries();
        Boolean raw         = parameters.getRaw();
        Integer seed        = parameters.getSeed();
        Integer corpusSize  = parameters.getSize();



        //labeledLDA.setRandomSeed(100);
        LOG.info("processing corpus: " + parameters.getCorpusFile());

        Instant startProcess = Instant.now();

        InstanceList instances = instanceBuilder.getInstances(parameters.getCorpusFile(), corpusSize, parameters.getRegEx(), parameters.getTextIndex(), parameters.getLabelIndex(), parameters.getIdIndex(), false, pos, parameters.getMinFreq(), parameters.getMaxDocRatio(),raw, parameters.getStopwords(), parameters.getStoplabels());

        int numWords = instances.getDataAlphabet().size();
        if ( numWords <= 10 || instances.size() <=10){
            LOG.warn("Not enough words ("+numWords+") to train a model. Task aborted");
            return new TopicReport();
        }

        double alphaSum = numTopics * alpha;

        ParallelTopicModel model = new ParallelTopicModel(numTopics, alphaSum, beta);

        model.setRandomSeed(seed);
        //model.setSymmetricAlpha(true);
        model.setSymmetricAlpha(false);

        int size = instances.size();
        model.addInstances(instances);
        Instant endProcess = Instant.now();
        String durationProcess = ChronoUnit.HOURS.between(startProcess, endProcess) + "hours "
                + ChronoUnit.MINUTES.between(startProcess, endProcess) % 60 + "min "
                + (ChronoUnit.SECONDS.between(startProcess, endProcess) % 60) + "secs";
        LOG.info("Corpus ("+size+") processed in: " + durationProcess);

        // Use two parallel samplers, which each look at one half the corpus and combine
        //  statistics after every iteration.
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int parallelThreads = (availableProcessors > 1) && (size/availableProcessors >= 100)? availableProcessors -1: 1;
        LOG.info("Parallel model to: " + parallelThreads + " threads");
        model.setNumThreads(parallelThreads);

        Integer numIterations = Double.valueOf(Math.min(Double.valueOf(instances.size())/2.0, Double.valueOf(parameters.getNumIterations()))).intValue();
        model.setNumIterations(numIterations);
        LOG.info("Num Iterations: " + numIterations);

        // Disable print loglikelihood. (use for testing purposes)
        model.printLogLikelihood = false;

        Integer optimizeInterval = numIterations/2;
        model.setOptimizeInterval(optimizeInterval);
        LOG.info("Optimize Interval: " + optimizeInterval);

        Integer intervalTopicDisplay = numIterations/2;
        model.setTopicDisplay(intervalTopicDisplay,5);
        LOG.info("Interval Topic Display: " + intervalTopicDisplay);

        Integer intervalTopicValidation = numIterations/2;
        model.setTopicValidation(intervalTopicValidation,10);
        model.maxRetries = maxRetries;
        LOG.info("Interval Topic Validation: " + intervalTopicValidation);

        LOG.info("building topic model " + parameters);
        Instant startModel = Instant.now();
        model.estimate();

        long invTopics = Arrays.stream(model.alpha).filter(e -> !(e > 0.0)).count();
        if (invTopics>0){
            LOG.warn("Too much Iterations!");
        }

        Instant endModel = Instant.now();
        String durationModel = ChronoUnit.HOURS.between(startModel, endModel) + "hours "
                + ChronoUnit.MINUTES.between(startModel, endModel) % 60 + "min "
                + (ChronoUnit.SECONDS.between(startModel, endModel) % 60) + "secs";
        LOG.info("Topic Model created in: " + durationModel);

        return new TopicReport(model, instances.getPipe());
    }
}
