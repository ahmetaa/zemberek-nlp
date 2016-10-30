package zemberek.morphology.apps;

import org.junit.Test;
import zemberek.morphology.lexicon.NullSuffixForm;
import zemberek.morphology.lexicon.SuffixForm;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ZemberekNlpScripts {

    @Test
    public void generateSuffixNames() throws IOException {
        TurkishSuffixes suffixes = new TurkishSuffixes();
        List<SuffixForm> forms = new ArrayList<>();
        for (SuffixForm form : suffixes.getAllForms()) {
            if (form instanceof NullSuffixForm) {
                continue;
            }
            forms.add(form);
        }
        forms.sort((a, b) -> a.getId().compareTo(b.getId()));
        List<String> result = forms.stream().map(s -> s.id).collect(Collectors.toList());
        Files.write(Paths.get("suffix-list"), result);
    }



}
