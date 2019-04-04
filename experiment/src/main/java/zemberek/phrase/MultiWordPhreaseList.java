package zemberek.phrase;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.Turkish;

public class MultiWordPhreaseList {

  static void processRaw(Path input, Path output) throws IOException {
    List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
    Set<String> rawPhrases = new HashSet<>();
    int totalRaw = 0;
    for (String line : lines) {
      String relevant = line.substring(line.indexOf('=') + 1).trim();
      List<String> items = Splitter.on("|").trimResults().omitEmptyStrings().splitToList(relevant);
      totalRaw += items.size();
      rawPhrases.addAll(items);
    }
    Log.info("Total Raw    = " + totalRaw);
    Log.info("No Duplicate = " + rawPhrases.size());

    List<String> sorted = new ArrayList<>(rawPhrases);
    sorted.sort(Turkish.COLLATOR);

    Files.write(output, sorted, StandardCharsets.UTF_8);
  }

  static void processRaw2(Path input, Path output) throws IOException {
    List<String> lines = TextIO.loadLines(input);
    List<String> result = new ArrayList<>();
    for (String line : lines) {
      if (line.startsWith("...")) {
        continue;
      }
      if (line.contains("(")) {
        result.add(line);
      }
    }
    Files.write(output, result, StandardCharsets.UTF_8);
  }

  static void processParenthesis(Path input) throws IOException {
    List<String> lines = TextIO.loadLines(input);
    for (String line : lines) {
      List<Foo> foos = new ArrayList<>();
      List<String> words = new ArrayList<>();


    }
  }

  static String baz(String line) {
    Pattern pattern =Pattern.compile("[(](.+?)[)]");
    Matcher m = pattern.matcher(line);

    StringBuffer b = new StringBuffer();
    while(m.find()) {

      String g = m.group();
      g = g.replaceAll("[()]","");
      m.appendReplacement(b, "_"+g.replaceAll(" ","_"));
    }
    m.appendTail(b);

    return b.toString();
  }

  class Foo {
    String word;
    List<String> alternatives;
  }


  public static void main(String[] args) throws IOException {
    Path r1 = Paths.get("data/phrase/raw-phrases.txt");
    Path r2 = Paths.get("data/phrase/raw-phrases2.txt");
    Path paranthesis = Paths.get("data/phrase/raw-paranthesis.txt");
    //processRaw(r1, r2);
    //processRaw2(r2, paranthesis);
    String s = baz("boo foo (haha aha) gfgf (foo) bar ");
    System.out.println(s);
  }

}
