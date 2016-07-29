package zemberek.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

//TODO (aaa): needs better design. add tests.
public class TextConsumer {

    List<String> content;
    int cursor = 0;

    public TextConsumer(List<String> content) {
        this.content = content;
    }

    public List<String> moveUntil(Predicate<String> predicate) {
        List<String> consumed = new ArrayList<>();

        while (!finished()) {
            String line = content.get(cursor);
            if (predicate.test(line)) {
                return consumed;
            }
            consumed.add(line);
            cursor++;
        }

        return consumed;
    }

    public boolean finished() {
        return cursor >= content.size();
    }

    public String current() {
        return content.get(cursor);
    }

    public void advance() {
        if (!finished())
            cursor++;
    }
}
