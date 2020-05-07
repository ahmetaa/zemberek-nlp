Applications
============

## Introduction

This module contains console applications that can be run from the terminal. 

## Running applications

Easiest way to run the applications is to use jar file with dependencies.

    java -jar zemberek-full.jar
    
This will print available applications. Output may look like this:     

```
List of available applications:
===============================
MorphologyConsole
-----------------
Applies morphological analysis and disambiguation to user entries.

PreprocessTurkishCorpus
-----------------------
Applies Turkish Sentence boundary detection and tokenization to a corpus file or
a directory of corpus files. Lines start with `<` character are ignored. It applies
white space normalization and  removes soft hyphens. Sentences that contain `combining
diacritic` symbols are ignored.

TrainClassifier
---------------
Generates a text classification model from a training set. Classification algorithm
is based on Java port of fastText library. It is usually suggested to apply tokenization,
lower-casing and other specific text operations to the training set before training
the model. Algorithm may be more suitable for sentence and short paragraph level
texts rather than long documents.
In the training set, each line should contain a single document. Document class
label needs to have __label__ prefix attached to it. Such as [__label__sports Match
ended in a draw.]
Each line (document) may contain more than one label.
If there are a lot of labels, LossType can be chosen `HIERARCHICAL_SOFTMAX`. This
way training and runtime speed will be faster with a small accuracy loss.
For generating compact models, use -applyQuantization and -cutOff [dictionary-cut-off]
parameters.

GenerateWordVectors
-------------------
Generates word vectors using a text corpus. Uses java port of fastText project.

ClassificationConsole
---------------------
Generates a FasttextTextClassifier from the given model and makes predictions for
the input sentences provided by the user. By default application applies tokenization
and lowercasing to the input. If model is generated with Lemmatization use [--preprocess
LEMMA] parameters.

EvaluateClassifier
------------------
Evaluates classifier with a test set.

CompressLm
----------
This application generates a compressed binary language model (Smooth-Lm).

GenerateVocabulary
------------------
Generates vocabulary from a given corpus.

StartGrpcServer
---------------
Starts Zemberek gRPC Server. By default it uses port 6789

EvaluateNer
-----------
Evaluates an annotated NER data set (reference) by either actually running NER with
a given model or against an already generated hypothesis data.

TrainNerModel
-------------
Generates Turkish Named Entity Recognition model. There will be two model sets in
the output directory, one is text models (in [model] directory), other is compressed
lossy model (in [model-compressed] directory). Usually compressed model is four
times smaller than the text model.

FindNamedEntities
-----------------
Finds named entities from a Turkish text file.
```

For running any application, add the name to the previous command:

    java -jar zemberek-full.jar MorphologyConsole

