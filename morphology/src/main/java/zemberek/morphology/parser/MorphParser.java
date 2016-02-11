package zemberek.morphology.parser;

import java.util.List;

public interface MorphParser {

    List<MorphParse> parse(String input);

}
