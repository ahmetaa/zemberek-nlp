Turkish Named Entity Recognition
============

## Introduction

Term `Named Entity` (NE) is used for defining unique entities. For example, `Ankara` 
`Cahit Arf` and `Gereksiz İşler Müdürlüğü` are named entities. Task of `Named Entity Recognition`
is to find NEs in natural language sentences.   

Zemberek-NLP provides a simple Named Entity Recognition module for Turkish. Currently module does not
have any model in it therefore users need to train their own model for now. In the upcoming version we will
provide simple generic models that can be used directly. However, usually NER modules are domain sensitive 
so some degree of customization will always be necessary. 

## Generating a NER model 

### Data Preparation

For creating a NER model, we first need a `training set`. A file that contains correct examples.
Training data can be prepared using three annotation methods. Lets look at the sentence below.

    Enerji Verimliliği Merkezi kurucu başkanı Bülent Yeşilata, Ankara'da bir toplantıya katıldı.
    
It should be tokenized first.     

    Enerji Verimliliği Merkezi kurucu başkanı Bülent Yeşilata , Ankara'da bir toplantıya katıldı .

Then named entity boundaries and types are annotated. There are three ways to annotate.
 
Bracket Style annotation

    [ORG Enerji Verimliliği Merkezi] kurucu başkanı [PER Bülent Yeşilata] , [LOC Ankara'da] bir toplantıya katıldı.
    
OpenNLP Style annotation

    <START:ORG> Enerji Verimliliği Merkezi <END> kurucu başkanı <START:PER> Bülent Yeşilata <END> , <START:LOC> Ankara'da <END> bir toplantıya katıldı.
    
Enamex Style annotation

    <b_enamex TYPE="ORG">Enerji Verimliliği Merkezi<e_enamex> kurucu başkanı <b_enamex TYPE="PER">Bülent Yeşilata<e_enamex> , <b_enamex TYPE="LOC">Ankara'da<e_enamex> bir toplantıya katıldı.

Here PER (Person), ORG (Organization) and LOC (Location) named entity types are used. But these names are arbitrary, user can define more types.
There should be a sentence per line. There may be sentences without NEs.

A Training set file needs to contain fairly large amounts of data.

NER annotation is not an easy task. Sometimes it is hard to decide the boundary and types of the named entities.

It is usually a good idea to separate 1/10 of the data for testing. We call this data `test set`.
Normally another set `development set` is used for parameter tuning but we will not use it in this example. 

### Training with CL application

If you use zemberek jar file, training is straight forward.

```
java -jar zemberek-full.jar TrainNerModel -s ENAMEX -t NE-enamex.train.txt -o test-model
```

This will create two model directories under `test-model` directory. One is for text model, other is for compressed binary model.   

### Training NER programatically

You can also train a NER model programatically:
   
    Path trainPath = Paths.get("ner-train");
    Path testPath = Paths.get("ner-test");
    Path modelRoot = Paths.get("my-model");

    NerDataSet trainingSet = NerDataSet.load(trainPath, AnnotationStyle.BRACKET);
    trainingSet.info(); // prints information

    NerDataSet testSet = NerDataSet.load(testPath, AnnotationStyle.BRACKET);
    testSet.info();

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    // Training occurs here. Result is a PerceptronNer instance.
    // There will be 7 iterations with 0.1 learning rate.
    PerceptronNer ner = new PerceptronNerTrainer(morphology)
        .train(trainingSet, testSet, 7, 0.1f);

    Files.createDirectories(modelRoot);
    ner.saveModelAsText(modelRoot);
        
If code ends with success, several files will be created in `modelRoot` directory. From now on 
these model directory will represent our NER model.

## Evaluating NER 

For evaluating a NER model, there is a command line application.

```
java -jar zemberek-full.jar EvaluateNer -s ENAMEX -r NE-enamex.test.txt -m test-model/model
```

## Using NER programatically

Once a model is generated, applying it to sentences is straight forward.

Here is an example:

    // assumes you generated a model in my-model directory.
    Path modelRoot = Paths.get("my-model");

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    PerceptronNer ner = PerceptronNer.loadModel(modelRoot, morphology);

    String sentence = "Ali Kaan yarın İstanbul'a gidecek.";
    NerSentence result = ner.findNamedEntities(sentence);
    List<NamedEntity> namedEntities = result.getNamedEntities();

    for (NamedEntity namedEntity : namedEntities) {
      System.out.println(namedEntity);
    }

Output will hopefully be like:

    [PER Ali Kaan]
    [LOC İstanbul'a]

### Performance and Speed

According to our tests, it gives
about %86-87 F-measure value (CONNL metric) for data set used in the papers
`Exploiting Morphology in Turkish Named Entity Recognition System` by Yeniterzi 2011 and 
 `Initial explorations on using CRFs for Turkish NER` by Seker and Eryigit 2012.
 This is probably not state of the art result but it is close. 

Each training iteration takes about 30 seconds on a 25.600 sentence 443.000 token training set.

Speed of Finding NEs is about 9000 tokens/second 

Test system: 2.3 Ghz AMD FX-8320, Ubuntu Linux 16.04 LTS.

## Algorithm

System uses Averaged Perceptron models for each class of NE and NE position.
It uses lexical features similar to Ratinov and Roth's `Design Challenges and Misconceptions in Named Entity Recognition` work.
Also, like Yeniterzi, we also add Turkish specific morphological features. 
We are aware that feature selection requires some improvements.  

In later versions we will add Word embeddings like Demir 2015 work `Improving Named Entity Recognition for Morphologically Rich Languages`
and try more advanced algorithms using CRF's, LSTMs or convolutional neural networks.

TODO: Add recent papers on NER 
