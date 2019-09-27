package cc.mallet.topics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class ModelFactory {

    @Autowired
    LDALauncher ldaLauncher;

    @Autowired
    LabeledLDALauncher lldaLauncher;


    public Double train(Map<String,String> parameters, ModelParams ldaParameters) throws IOException {

        String email = parameters.get("email");

        Double result = -100.0;

        if (!parameters.containsKey("algorithm")){
            result = ldaLauncher.train(ldaParameters, email);
        }else{

            switch (parameters.get("algorithm").toLowerCase()){
                case "lda":
                    result = ldaLauncher.train(ldaParameters,email);
                    break;
                case "llda":
                    result = lldaLauncher.train(ldaParameters,email);
                    break;
                default:
                    new RuntimeException("No algorithm found by parameter 'algorithm' = " + parameters.get("algorithm"));
            }
        }

        return result;
    }
}
