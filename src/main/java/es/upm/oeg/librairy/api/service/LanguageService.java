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
            LOG.info("language added: " + locale);
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

        TextObject textObject   = textObjectFactory.forText(text);
        Optional<LdLocale> lang = languageDetector.detect(textObject);
        return (!lang.isPresent())? "unknown" : lang.get().getLanguage();
    }


    public List<String> getAvailableLangs() {
        return availableLangs;
    }
}
