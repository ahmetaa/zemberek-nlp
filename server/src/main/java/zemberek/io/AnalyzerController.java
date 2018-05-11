package zemberek.io;

import java.util.List;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

@Controller
@EnableAutoConfiguration
public class AnalyzerController {
    static TurkishMorphology morphology;
    static {
        try {
            morphology = TurkishMorphology.createWithDefaults();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/analyze")
    @ResponseBody
    String home(@RequestParam(name="word", required=false, defaultValue="") String sentence) {

        List<WordAnalysis> analysisList = morphology.analyzeSentence(sentence);
        StringBuilder sb = new StringBuilder("Input: "  + sentence);
        for(WordAnalysis wa : analysisList) {
            sb.append("<div>" + wa.getInput() + "</div>");
            for (SingleAnalysis sa : wa) {
                sb.append("<div>" +  sa.formatLong() + "</div>");
            }
        }
        sb.append("Disambiguation result:");
        SentenceAnalysis disambiguated = morphology.disambiguate(sentence, analysisList);
        for (SingleAnalysis sa : disambiguated.bestAnalysis()) {
            sb.append("<div>" +  sa.formatLong() + "</div>");
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(AnalyzerController.class, args);
    }
}