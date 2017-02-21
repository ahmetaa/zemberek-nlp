Language Modeling And Compression
=================================

## Language Model Compression: SmoothLm

This library provides a language model library compression algorithm implementation.
SmoothLm is a compressed, optionally quantized, randomized back-off n-gram language model.
It uses Minimal Perfect Hash functions for compression, This means actual n-gram values are not stored in the model.
Implementation is similar with the systems described in Gutthrie and Hepple's
'Storing the Web in Memory: Space Efficient Language Models with Constant Time Retrieval (2010)' paper.

This is a lossy model because for non existing n-grams it may return an existing n-gram probability value (false positive).
Probability of this happening depends on the fingerprint hash length. This value is determined during the model creation.
Regularly 8,16 or 24 bit fingerprints are used and false positive probability for an non existing n-gram is
(probability of an n-gram does not exist in LM)*1/(2^fingerprint bit size).
SmoothLm also provides quantization for even more compactness. So probability and back-off values can be quantized to
8, 16 or 24 bits.

There are many alternatives for language model compression such as KenLm, RandLm and BerkeleyLm. SmoothLm and BerkeleyLm are implemented in Java
so they are probably good choices for Java applications. Otherwise KenLm may be a better fit.

### Limitations
- SmoothLm can only compress language models where for an order, n-gram amount must be less than 2,147,483,648 (2^31-1)
- SmoothLm requires Java 8.
- SmoothLm loads all model data to memory. It does not work from disk. So it may not be convenient when there is limited amount of memory and language model is huge.

### Generating SmoothLm
SmoothLm can be generated from standard ARPA formatted language models. There is a command line application class available for
the conversion. Once the compressed binary file is generated it can be used in the applications.
Generally speaking for 1 billion N-Grams if 16-8-8 space parameter is used, generated file will be around 4.4GB.
Suppose we have an arpa file named lm.arpa:

From Command Line:

    java -Xmx4G -cp [jar file with dependencies] zemberek.lm.app.CompressLm -in lm.arpa -out lm.smooth

Generates the compressed model file lm.smooth. -Xmx4G parameter tells java virtual machine to use maximum 4 Gbytes
of memory. If model is very large, for better compression -spaceUsage 16-8-8 can be used. This will quantize probability
and backoff values to 256 values and usually application performance is not effected from this.

Here are the parameters for the application:

    Usage: java -cp "[CLASS-PATH]" zemberek.lm.apps.ConvertToSmoothLm -arpaFile FILE [-chunkBits N] [-logFile FILE] -smoothFile FILE [-spaceUsage VAL] [-tmpDir FILE] [-verbosity N]

     -in FILE         : Arpa input file.
     -chunkBits N     : Defines the size of chunks when compressing very large models. By default it is
                        21 bits meaning that chunks of 2^21 n-grams are used. Value must be between 16
                        to 31 (inclusive).
     -logFile FILE    : Log output file.
     -out FILE        : SmoothLm output file.
     -spaceUsage VAL  : How many bits of space to be used for fingerprint, probability and back-off
                        values in the compressed language model. Value must be in x-y-z format. By
                        default it is 16-16-16 which means all values will be 2 bytes (16 bits). Values
                        must be an order of 8, maximum 32 is allowed. Default is 24-8-8
     -tmpDir FILE     : Temporary folder for intermediate files. Operating System's temporary dir with
                        a random folder is used by default.
     -verbosity N     : Verbosity level. 0-WARN 1-INFO 2-DEBUG 3-TRACE. Default level is 1

### Using SmoothLm

Once SmoothLm file is generated, it can be accessed programmatically.
SmoothLm does not provide a constructor. It can be instantiated with Builder pattern:

    SmothLm lm = SmoothLm.builder(new File("lm.smooth")).build();

There are several parameters can be used during the instantiation.

    SmoothLm lm = SmoothLm.builder(new File("lm.smooth"))
        .logBase(Math.E)
        .unigramSmoothing(0.8)
        .build();

Here model probability and backoff values are converted to e and additional unigram smoothing is applied.

After Language model is instantiated several methods are available. Some Examples:

    // Model information.
    System.out.println(lm.info());

    // convert words to indexes.
    int[] wordIds = lm.getVocabulary().toIndexes("hello","world");

    // gets probability of an n-gram. applies back-off if necessary
    float probability = lm.getProbability(wordIds);

    // explains how probability is calculated.
    System.out.println(lm.explain(wordIds));


