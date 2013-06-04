package zemberek.morphology.parser;

import java.util.List;

public interface MorphParser {

    public List<MorphParse> parse(String input);

}
