package zemberek.core.quantization;


public interface Quantizer {

    int getQuantizationIndex(double value);

    double getQuantizedValue(double value);

    DoubleLookup getDequantizer();
}
