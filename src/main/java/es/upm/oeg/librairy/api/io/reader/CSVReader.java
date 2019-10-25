package es.upm.oeg.librairy.api.io.reader;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.api.builders.DateBuilder;
import es.upm.oeg.librairy.api.facade.model.avro.DataFields;
import es.upm.oeg.librairy.api.facade.model.avro.DataSource;
import es.upm.oeg.librairy.api.facade.model.avro.ReaderFormat;
import es.upm.oeg.librairy.api.model.Document;
import es.upm.oeg.librairy.api.model.QueryDocument;
import es.upm.oeg.librairy.api.service.LanguageService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class CSVReader extends FileReader{

    private static final Logger LOG = LoggerFactory.getLogger(CSVReader.class);
    private final String source;
    private final String file;
    private final LanguageService languageService;
    private String separator;
    private Map<String, List<Integer>> map;
    private String labelSeparator;

    public CSVReader(DataSource dataSource, Boolean zip, LanguageService languageService) throws IOException {
        this.path           = "inputStream";
        this.reader         = new BufferedReader(getInputStream(dataSource.getUrl(), zip));
        this.file           = dataSource.getUrl();
        this.map            = new HashMap<>();
        getParameters(dataSource).entrySet().forEach(entry -> this.map.put(entry.getKey(), entry.getValue().stream().map(i -> Integer.valueOf(i)).collect(Collectors.toList())));
        this.separator      = Strings.isNullOrEmpty(dataSource.getFilter())? "," : dataSource.getFilter();
        this.labelSeparator = " ";
        this.languageService = languageService;
        source = dataSource.getName();
    }

    @Override
    public Optional<Document> next() {
        String line = null;
        try {
            if ((line = reader.readLine()) == null) {
                reader.close();
                return Optional.empty();
            }

            String[] values = parseByTokenizer(separator,line);

            Document document = new Document();

            document.setFile(file);
            document.setSource(source);
            if (map.containsKey("id")){
                StringBuilder id = new StringBuilder();
                for(Integer i : map.get("id")){
                    id.append(StringReader.softLabelFormat(values[i]));
                }

                document.setId(id.toString());
            }

            if (map.containsKey("name")){
                StringBuilder name = new StringBuilder();
                for(Integer i : map.get("name")){
                    name.append(StringReader.softLabelFormat(values[i]));
                }

                document.setName(name.toString());
            }

            if (map.containsKey("text")) {
                StringBuilder text = new StringBuilder();
                for(Integer i : map.get("text")){
                    text.append(format(values[i])).append(" ");
                }

                document.setText(text.toString());
                String lang = languageService.getLanguage(text.toString());
                document.setLang(lang);
            }
            if (map.containsKey("labels")){
                StringBuilder labels = new StringBuilder();
                for(Integer i : map.get("labels")){
                    if (values.length>i) {
                        labels.append(StringReader.softLabelFormat(values[i])).append(" ");
                    }
                }

                document.setLabels(Arrays.asList(labels.toString().split(labelSeparator)));
            }
            if (map.containsKey("extra")){
                Map<String,String> extraData = new HashMap<>();
                for(Integer i : map.get("extra")){
                    extraData.put(String.valueOf(i), StringReader.softFormat(values[i]) );
                }
                document.setExtraData(extraData);
            }
            document.setDate(DateBuilder.now());
            document.setFormat("csv");

            return Optional.of(document);
        } catch (ArrayIndexOutOfBoundsException e){
            LOG.warn("Invalid row("+e.getMessage()+") - [" + line + "]");
            return Optional.of(new Document());
        }catch (Exception e){
            LOG.error("Unexpected error parsing file: " + path,e);
            return Optional.of(new Document());
        }
    }

    public static String[] parseByRegex(String separator, String csvLine) {
        String text = csvLine.replaceAll("","");
        String pattern = "\"([^\"]*)\"|(?<="+separator+"|^)([^"+separator+"]*)(?:"+separator+"|$)";
        Pattern csvPattern = Pattern.compile(pattern);
        Matcher matcher = csvPattern.matcher(text);
        List<String> allMatches = new ArrayList<>();
        String match;
        while (matcher.find()) {
            match = matcher.group(1);
            if (match!=null) {
                allMatches.add(match);
            }
            else {
                allMatches.add(matcher.group(2));
            }
        }
        if (!allMatches.isEmpty()) return allMatches.toArray(new String[allMatches.size()]);
        return new String[]{};
    }

    public static String[] parseByTokenizer(String separator, String csvLine) {

        if (Strings.isNullOrEmpty(csvLine)) return new String[]{};

        StringTokenizer tokenizer = new StringTokenizer(csvLine,separator);

        List<String> fields = new ArrayList<>();
        String field = "";
        Boolean innerField = false;
        while(tokenizer.hasMoreTokens()){
            String token = tokenizer.nextToken();
            field += token.replaceAll("\"","");
            if (token.startsWith("\"") && !token.endsWith("\"")){
                innerField = true;
            }else if (token.endsWith("\"") && !token.endsWith("\\\"") && !token.endsWith("\"\"")) {
                innerField = false;
            }
            if (innerField){
                continue;
            }
            fields.add(field);
            innerField = false;
            field = "";
        }
        return fields.toArray(new String[0]);
    }


    public static void main(String[] args) throws IOException {
        DataSource ds = DataSource.newBuilder()
                .setDataFields(DataFields.newBuilder().setId("0").setText(Arrays.asList(new String[]{"1"})).build())
                .setName("tender")
                .setFilter(",")
                .setFormat(ReaderFormat.CSV)
                .setOffset(1)
                .setSize(-1)
                .setUrl("https://www.dropbox.com/s/wcysf99s6cb2c3p/jrc-en.csv?raw=1")
                .build();
        LanguageService lService = new LanguageService();
        lService.setup();
        CSVReader reader = new CSVReader(ds,false,lService);
        reader.offset(1);
        Integer count = 0;
        while(reader.next().isPresent()){
            count++;
        }
        System.out.println(count);
    }


//    public static void main(String[] args) {
//        String text = "jrc32000R2039-es,\"Reglamento (CE) nº 2039/2000 del Parlamento Europeo y del Consejo, de 28 de septiembre de 2000, que modifica el Reglamento (CE) nº 2037/2000 sobre las sustancias que agotan la capa de ozono, en cuanto al año de referencia para la asignación de cuotas de hidroclorofluorocarburos\",\"Reglamento (CE) no 2039/2000 del Parlamento Europeo y del Consejo. de 28 de septiembre de 2000. que modifica el Reglamento (CE) n° 2037/2000 sobre las sustancias que agotan la capa de ozono, en cuanto al año de referencia para la asignación de cuotas de hidroclorofluorocarburos. EL PARLAMENTO EUROPEO Y EL CONSEJO DE LA UNIÓN EUROPEA, Visto el Tratado constitutivo de la Comunidad Europea y, en particular, el apartado 1 de su artículo 275, Visto el dictamen del Comité Económico y Social(1), Previa consulta al Comité de las Regiones, De conformidad con el procedimiento establecido en el artículo 251 del Tratado(2), Considerando lo siguiente: (1) En el Reglamento (CE) n° 2037/2000 del Parlamento Europeo y del Consejo, de 29 de junio de 2000, sobre las sustancias que agotan la capa de ozono(3), se establece 1996 como año de referencia para la asignación de cuotas de hidroclorofluorocarburos (HCFC). Desde 1996, el mercado de los HCFC ha evolucionado considerablemente por lo que respecta a los importadores, por lo que el mantenimiento de esa fecha de referencia daría lugar a que numerosos importadores se vieran privados de su cuota de importación. Por norma general, las cuotas deben estar fundadas en las cifras más recientes y más representativas disponibles. En este caso, son las relativas a 1999. de ahí que mantener la fecha de 1996 pudiera considerarse arbitrario e incluso constituir una infracción a los principios de no discriminación y de confianza legítima. (2) Es preciso, por tanto, modificar el Reglamento (CE) n° 2037/2000 en consecuencia. HAN ADOPTADO EL PRESENTE REGLAMENTO: Artículo 1. En la letra h) del inciso i) del apartado 3 del artículo 4 del Reglamento (CE) n° 2037/2000, la mención \"\"su porcentaje de mercado en 1996\"\" se sustituirá por la mención \"\"la cuota porcentual que se le asignó en 1999\"\". Artículo 2. El presente Reglamento entrará en vigor el día siguiente al de su publicación en el Diario Oficial de las Comunidades Europeas. El presente Reglamento será obligatorio en todos sus elementos y directamente aplicable en cada Estado miembro. \",2084";
//        String text2 = "1,\"TY KU\",\" TY KU /taɪkuː/ is an American alcoholic beverage company that specializes in sake and other spirits. The privately-held company was founded in 2004 and is headquartered in New York City New York. While based in New York TY KU's beverages are made in Japan through a joint venture with two sake breweries. Since 2011 TY KU's growth has extended its products into all 50 states.\"";
//        Arrays.asList(parseByTokenizer(",",text)).forEach(r -> System.out.println("[row]" + r));
//    }

}
