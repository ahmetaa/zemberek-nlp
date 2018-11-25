package zemberek.core.embeddings;

import java.io.PrintStream;
import java.util.List;
import zemberek.core.collections.IntMap;
import zemberek.core.collections.IntVector;

public class Meter {

  long nexamples_;

  IntMap<Metrics> labelMetrics_ = new IntMap<>();
  Metrics metrics_ = new Metrics();

  double precision(int i) {
    Metrics metrics = labelMetrics_.get(i);
    return metrics.precision();
  }

  double recall(int i) {
    return labelMetrics_.get(i).recall();
  }

  double f1Score(int i) {
    return labelMetrics_.get(i).f1Score();
  }

  double precision() {
    return metrics_.precision();
  }

  double recall() {
    return metrics_.recall();
  }

  void log(
      IntVector labels,
      List<Model.FloatIntPair> predictions) {
    nexamples_++;
    metrics_.gold += labels.size();
    metrics_.predicted += predictions.size();

    for (Model.FloatIntPair prediction : predictions) {
      labelMetrics_.get(prediction.second).predicted++;

      if (labels.contains(prediction.second)) {
        labelMetrics_.get(prediction.second).predictedGold++;
        metrics_.predictedGold++;
      }
    }

    for (int label : labels.copyOf()) {
      labelMetrics_.get(label).gold++;
    }
  }

  void writeGeneralMetrics(PrintStream out, int k) {
    out.println( "N\t" +  nexamples_ );
    out.println( String.format("P@%d\t%.3f", k, metrics_.precision()));
    out.println( String.format("R@%d\t%.3f", k, metrics_.recall()));
  }

  public static class Metrics {

    long gold;
    long predicted;
    long predictedGold;

    double precision() {
      return predictedGold / (double) predicted;
    }

    double recall() {
      return predictedGold / (double) gold;
    }

    double f1Score() {
      return 2 * predictedGold / (double) (predicted + gold);
    }
  }

}
