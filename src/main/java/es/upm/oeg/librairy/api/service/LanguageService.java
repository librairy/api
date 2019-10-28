package es.upm.oeg.librairy.api.service;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.BuiltInLanguages;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class LanguageService {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageService.class);

    private static final String DEFAULT_LANG = "en";

    private LanguageDetector languageDetector;
    private TextObjectFactory textObjectFactory;

    private List<String> availableLangs = Arrays.asList(new String[]{"en","es","fr","de","it"});

    @PostConstruct
    public void setup() throws IOException {
        //load all languages:
        LanguageProfileReader langReader = new LanguageProfileReader();

        List<LanguageProfile> languageProfiles = new ArrayList<>();

        Iterator it = BuiltInLanguages.getLanguages().iterator();

        while(it.hasNext()) {
            LdLocale locale = (LdLocale)it.next();
            languageProfiles.add(langReader.readBuiltIn(locale));
        }


        //build language detector:
        this.languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                .withProfiles(languageProfiles)
                .build();

        //create a text object factory
        this.textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
    }


    public String getLanguage(String text){
        if (Strings.isNullOrEmpty(text)) return DEFAULT_LANG;

        String textVal = text.substring(0, Math.min(1000, text.length()));
        TextObject textObject   = textObjectFactory.forText(textVal);
        Optional<LdLocale> lang = languageDetector.detect(textObject);
        return (!lang.isPresent())? "unknown" : lang.get().getLanguage();
    }

    public List<String> getAvailableLangs() {
        return availableLangs;
    }

    public static void main(String[] args) throws IOException {
        LanguageService service = new LanguageService();
        service.setup();
        String text = "Judgment of the Court.  First Chamber.  of 27 October 2005.  in Joined Cases C-26604 to C-27004 C-27604 and C-32104 to C-32504 Reference for a preliminary ruling from the Tribunal des affaires de securite sociale de Saint-Etienne Nazairdis SAS and Others v Caisse nationale de l'organisation autonome d'assurance vieillesse des travailleurs non salaries des professions industrielles et commerciales Organic 1.  In Joined Cases C-26604 to C-27004 C-27604 and C-32104 to C-32504 references for a preliminary ruling under Article 234 EC from the Tribunal des affaires de securite sociale de Saint-Etienne Cases C-26604 to C-27004 and Case C-27604 and the Cour d'appel de Lyon Cases C-32104 to C-32504 France made by Decisions of 5 April and 24 February 2004 received at the Court on 24 25 and 29 June and 27 July 2004 in the proceedings pending before those courts between Distribution Casino France SAS formerly Nazairdis SAS Case C-26604 Jaceli SA Case C-26704 Komogo SA Case C-26804 and Case C-32404 Tout pour la maison SARL Case C-26904 and Case C-32504 Distribution Casino France SAS Case C-27004 Bricorama France SAS Case C-27604 Distribution Casino France 3 SAS Case C-32104 Societe Casino France successor to IMQEF SA successor to JUDIS SA Case C-32204 Dechrist Holding SA Case C-32304 and Caisse nationale de l'organisation autonome d'assurance vieillesse des travailleurs non salaries des professions industrielles et commerciales Organic  the Court First Chamber composed of K.  Schiemann President of the Fourth Chamber acting for the President of the First Chamber J. N.  Cunha Rodrigues K.  Lenaerts Rapporteur E.  Juhasz and M.  Ilesic Judges.  C.  Stix-Hackl Advocate General L.  Hewlett Principal Administrator for the Registrar gave a judgment on 27 October 2005 in which it ruled 1 OJ C 228 of 11. 09. 2004. OJ C 201 of 07. 08. 2004. OJ C 239 of 25. 09. 2004.  --------------------------------------------------.   ";
        System.out.println(service.getLanguage(text));

    }
}
