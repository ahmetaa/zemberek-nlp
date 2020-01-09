Text Classification
============

## Introduction

Text classification can be used in several NLP tasks such as document classification, sentiment analysis or spam detection.   

Zemberek-NLP provides a simple text classification module based on our Java port of [fastText](https://fasttext.cc/) project.

## Generating a Classification Model 

### Data Preparation

For creating a classification model, we first need a `training set`. A file that contains documents and their labels.
Training data should be prepared `fastText` style. For example, a training set that contains news
titles and their categories (You can [download](https://drive.google.com/drive/folders/1JBPExAeRctAXL2oGW2U6CbqfwIJ84BG7) the set):

    __label__magazin Jackie Chan'a yapmadıklarını bırakmadılar!
    __label__spor Fenerbahçe Akhisar'da çok rahat kazandı    
    __label__teknoloji Google Nexus telefonları Huawei de üretebilir!    

Each line must contain a `document` and its label(s). A document can be a sentence, or a paragraph. Algorithm should work
with a page long document but performance may be lower than expected. A label must have a `__label__` prefix.  

However, it is usually suggested to preprocess the training set. Otherwise, for example `üretebilir!` and `üretebilir`
 will be handled as different words. How the input should be processed depends on the problem. Some options: 
 * Tokenization
 * Removal of some punctuations
 * Removal or normalization of digits
 * Using stems, lemmas or morphemes instead of words
 * Lower casing 
 
 Most of those operations reduce sparsity of the vocabulary without damaging the information carried 
 in the document and probably improves the performance but experimentation is necessary.
 
 For example after tokenization and lowercasing training set may become:
 
    __label__magazin jackie chan'a yapmadıklarını bırakmadılar
    __label__spor fenerbahçe akhisar'da çok rahat kazandı
    __label__teknoloji google nexus telefonları huawei de üretebilir
 
 Lets assume this file is called `news-title-category-set`

### Training

Training can be done with a console application or with the API. Using console application is easy.
Use zemberek with dependencies jar: 

    java -jar zemberek-full.jar TrainClassifier \ 
     -i news-title-category-set \
     -o news-title-category-set.model \
     --learningRate 0.1 \
     --epochCount 50 
   
If training ends with success `news-title-category-set.model` file will be generated. Model file is quite large
but there are ways to reduce it.  

## Using the Classifier

Once a model is generated, applying it to documents is straight forward. One important thing is that
 before the prediction operation, input text should be processed same as the training data.  

Here is an example:

```java
    FastTextClassifier classifier = FastTextClassifier.load(modelPath);

    String s = "Beşiktaş berabere kaldı."
    
    // process the input exactly the way trainin set is processed
    String processed = String.join(" ", TurkishTokenizer.DEFAULT.tokenizeToStrings(s));
    processed = processed.toLowerCase(Turkish.LOCALE);
    
    // results, only top three.
    List<ScoredItem<String>> res = classifier.predict(processed, 3);
    
    for (ScoredItem<String> re : res) {
       System.out.println(re);
    }
```

Output may look like this:

    __label__spor : 0.000010
    __label__türkiye : -11.483298
    __label__yaşam : -11.512561
    
## Reducing the model size
    
Like original fastText, Java port supports quantization for reducing the model size. Generally model
sizes can be hundreds of megabytes. Using quantization and l2-norm cut-off, model size can be reduced
dramatically with small performance loss.

For generating quantized models, `--applyQuantization` and `--cutOff` can be used. For example: 

    java -jar zemberek-full.jar TrainClassifier \ 
     -i news-title-set \
     -o news-title.model \
     --learningRate 0.1 \
     --epochCount 50 \
     --applyQuantization \
     --cutOff 15000

Now there will be two models, `news-title-category-set.model` and `news-title-category-set.model.q` 
Both models can be used for instantiating FastTextClassifier.

For the set mentioned above, model size is reduced from 400 MB to 1MB.

### Performance and Speed

According to the [1] fastText classification algorithm gives comparable results to alternative
more complex systems of 2016. However, more recent state of the art systems may give better results. 

Despite not using GPUs, original fastText library is very fast. Our Java port's speed is close to the C++ version. Training is multi-threaded. 
For example, using 4 threads, news title set with 68365 samples and 442.000 tokens, training takes 
about 20 seconds. Testing 1000 examples takes around 4.5 seconds with a single thread.

Test system: 2.3 Ghz AMD FX-8320, Ubuntu Linux 16.04 LTS.

[1] A. Joulin, E. Grave, P. Bojanowski, T. Mikolov,
 [Bag of Tricks for Efficient Text Classification](https://arxiv.org/abs/1607.01759)

## Algorithm

As mentioned before, classification algorithm is based on a port of fastText project.
Please refer to the [project](https://fasttext.cc/) documentation and related scientific papers for more information. 

## Examples

There are two examples in [examples](https://github.com/ahmetaa/zemberek-nlp/tree/master/examples/src/main/java/zemberek/examples/classification) module.
**NewsTitleCategoryFinder** generates different classification models from Turkish news title category
 data set and evaluates.
 
 **SimpleClassification** shown how to make category prediction in runtime.
 
 There is also a Turkish [wiki page](https://github.com/ahmetaa/zemberek-nlp/wiki/Zemberek-NLP-ile-Metin-S%C4%B1n%C4%B1fland%C4%B1rma)
  on generating and evaluating classifier for Turkish news headline categories.  
