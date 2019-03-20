package es.upm.oeg.librairy.api.service;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.builders.HashTopicBuilder;
import es.upm.oeg.librairy.api.executors.ParallelExecutor;
import es.upm.oeg.librairy.api.facade.model.avro.*;
import es.upm.oeg.librairy.api.io.reader.Reader;
import es.upm.oeg.librairy.api.io.reader.ReaderFactory;
import es.upm.oeg.librairy.api.io.searcher.Searcher;
import es.upm.oeg.librairy.api.io.searcher.SearcherFactory;
import es.upm.oeg.librairy.api.io.writer.Writer;
import es.upm.oeg.librairy.api.io.writer.WriterFactory;
import es.upm.oeg.librairy.api.model.Document;
import org.librairy.metrics.data.Evaluation;
import org.librairy.metrics.data.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class EvaluationService {

    private static final Logger LOG = LoggerFactory.getLogger(EvaluationService.class);

    @Autowired
    MailService mailService;

    @Autowired
    ItemService itemService;

    @Autowired
    AnnotationService annotationService;


    public void create(DataSource dataSource, DataSink dataSink,String model, Integer refSize, List<Integer> intervals){

        try{

            if (dataSource.getDataFields().getLabels() == null || dataSource.getDataFields().getLabels().isEmpty()) throw new RuntimeException("Labels field is emtpy");

            Writer writer       = WriterFactory.newFrom(dataSink);

            // Annotate
            writer.reset();
            AnnotationsRequest annotationRequest = AnnotationsRequest.newBuilder()
                    .setDataSource(dataSource)
                    .setDataSink(dataSink)
                    .setModelEndpoint(model)
                    .setContactEmail("internal@mail.com")
                    .build();
            annotationService.create(annotationRequest);

            // Evaluate
            LOG.info("ready to evaluate annotations...");
            DataSource outputSource = DataSource.newBuilder()
                    .setCache(false)
                    .setCredentials(dataSink.getCredentials())
                    .setFilter("*:*")
                    .setDataFields(
                            DataFields.newBuilder()
                                    .setId("id")
                                    .setName("name_s")
                                    .setLabels(Arrays.asList("labels_t"))
                                    .setExtra(Arrays.asList("topics0_t","topics1_t","topics2_t"))
                                    .build()
                    )
                    .setFormat(ReaderFormat.valueOf(dataSink.getFormat().name()))
                    .setOffset(0)
                    .setSize(-1)
                    .setUrl(dataSink.getUrl())
                    .build();

            Reader reader       = ReaderFactory.newFrom(outputSource);

            Map<String,Evaluation> simResults   = new ConcurrentHashMap<>();
            Map<String,Evaluation> t0Results    = new ConcurrentHashMap<>();
            Map<String,Evaluation> t1Results    = new ConcurrentHashMap<>();
            Map<String,Evaluation> t2Results    = new ConcurrentHashMap<>();

            // Create Evaluations
            Long maxSize = outputSource.getSize();
            AtomicInteger counter = new AtomicInteger();
            Integer interval = maxSize > 100? maxSize.intValue()/100 : 1 ;
            Optional<Document> doc;
            reader.offset(outputSource.getOffset().intValue());
            ParallelExecutor parallelExecutor = new ParallelExecutor();
            while(( maxSize<0 || counter.get()<maxSize) &&  (doc = reader.next()).isPresent()){
                final Document document = doc.get();
                if (counter.incrementAndGet() % interval == 0) LOG.info(counter.get() + " document/s reviewed");
                parallelExecutor.submit(() -> {
                    try {
                        String id           = document.getId();
                        Reference reference = Reference.newBuilder().setDocument(DocReference.newBuilder().setId(id).build()).build();

                        Map<String, String> extra = document.getExtraData();
                        t0Results.put(id, new Evaluation(document.getLabels(), extra.containsKey("topics0_t")? Arrays.asList(extra.get("topics0_t").split(" ")) : Collections.emptyList()));
                        t1Results.put(id, new Evaluation(document.getLabels(), extra.containsKey("topics1_t")? Arrays.asList(extra.get("topics1_t").split(" ")) : Collections.emptyList()));
                        t2Results.put(id, new Evaluation(document.getLabels(), extra.containsKey("topics2_t")? Arrays.asList(extra.get("topics2_t").split(" ")) : Collections.emptyList()));

                        ItemsRequest itemsRefRequest = ItemsRequest.newBuilder()
                                .setSize(refSize)
                                .setReference(reference)
                                .setDataSource(outputSource)
                                .build();
                        List<Item> itemsByLabels    = itemService.getItemsByLabels(itemsRefRequest);

                        int num = intervals.isEmpty()? refSize : intervals.stream().reduce((a, b) -> a > b ? a : b).get() + 1;
                        ItemsRequest itemsValRequest = ItemsRequest.newBuilder()
                                .setSize(num)
                                .setReference(reference)
                                .setDataSource(outputSource)
                                .build();
                        List<Item> itemsByHash      = itemService.getItemsByHash(itemsValRequest).stream().skip(1).collect(Collectors.toList());

                        List<String> refData = itemsByLabels.stream().map(i -> i.getId()).collect(Collectors.toList());
                        List<String> valData = itemsByHash.stream().map(i -> i.getId()).collect(Collectors.toList());

                        Map<String,Object> data = new HashMap<String, Object>();

                        data.put("byLabels_t",itemsByLabels.stream().map(i -> i.getId()+"("+i.getScore()+")").collect(Collectors.joining(" ")));
                        data.put("byHash_t",itemsByHash.stream().map(i -> i.getId()+"("+i.getScore()+")").collect(Collectors.joining(" ")));

                        writer.update(id,data);

                        simResults.put(id, new Evaluation(refData, valData));

                    } catch (Exception e) {
                        LOG.error("Unexpected error adding new document to corpus",e);
                    }
                });
            }
            parallelExecutor.awaitTermination(1, TimeUnit.HOURS);
            writer.close();

            LOG.info(simResults.size() + " evaluations completed");

            StringBuilder report = new StringBuilder();

            if (intervals.isEmpty()){
                report.append(printResults(simResults, "Evaluation", Optional.empty()));
            }else{
                for(Integer n : intervals){
                    report.append(printResults(simResults, "Evaluation@"+n, Optional.of(n)));
                }
            }

            report.append(printResults(t0Results, "Topic-based Hash Evaluation - Level 0", Optional.empty()));
            report.append(printResults(t1Results, "Topic-based Hash Evaluation - Level 1", Optional.empty()));
            report.append(printResults(t2Results, "Topic-based Hash Evaluation - Level 2", Optional.empty()));

            //mailService.notifyAnnotation(annotationRequest,"Annotation completed");
            LOG.info("Result: " + report.toString());
            LOG.info("Evaluation Completed!");
        }catch (Exception e){
            LOG.error("Unexpected error",e);
            //mailService.notifyAnnotationError(annotationRequest, e.getMessage());
        }
    }

    private String printResults(Map<String,Evaluation> table, String id, Optional<Integer> range){
        StringBuilder report = new StringBuilder();
        report.append("# ").append(id).append(" :").append("\n");
        Stats precisionStats   = new Stats(table.entrySet().parallelStream().map(e -> range.isPresent()? e.getValue().getPrecisionAt(range.get()) : e.getValue().getPrecision()).collect(Collectors.toList()));
        report.append(" - Precision:").append(precisionStats).append("\n");
        Stats recallStats       = new Stats(table.entrySet().parallelStream().map(e -> range.isPresent()? e.getValue().getRecallAt(range.get()) : e.getValue().getRecall()).collect(Collectors.toList()));
        report.append(" - Recall:").append(recallStats).append("\n");
        Stats fMeasureStats     = new Stats(table.entrySet().parallelStream().map(e -> range.isPresent()? e.getValue().getFMeasureAtN(range.get()) : e.getValue().getFMeasure()).collect(Collectors.toList()));
        report.append(" - FMeasure:").append(fMeasureStats).append("\n");
        report.append("\n");
        return report.toString();

    }

}
