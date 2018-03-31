package zemberek.morphology;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.ambiguity.AbstractDisambiguator.DataSet;
import zemberek.morphology.ambiguity.AbstractDisambiguator.DataSetLoader;

public class ConvertTrainingSet {

  static class AnalysisData {
    String input;
    String root;
    PrimaryPos primaryPos;
    SecondaryPos secondaryPos;
    List<String> morphemeList = new ArrayList<>();
  }

  static DataSet load(Path path) throws IOException {
    return com.google.common.io.Files
        .readLines(path.toFile(), Charsets.UTF_8, new DataSetLoader());
  }

  public static void main(String[] args) throws IOException {
    Path path= Paths.get("/home/ahmetaa/apps/MD-Release/data.test.txt");
    DataSet test = load(path);
    

  }

}
