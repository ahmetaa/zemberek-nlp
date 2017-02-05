Zemberek-NLP
============

Here is the the new home of the Zemberek project. 
Zemberek-NLP provides basic Natural Language Processing tools for Turkish text.
Code and API is **not** compatible with the old [Zemberek2] (https://github.com/ahmetaa/zemberek) project. 
Please note that **all code and APIs are subject to change drastically until version 1.0.0**

Latest version is 0.10.0 (January 20th 2017)

## FAQ 

Please read the [FAQ] (https://github.com/ahmetaa/zemberek-nlp/wiki/FAQ) for common questions.

## Usage

### Maven

Add this to pom.xml file

    <repositories>
        <repository>
            <id>ahmetaa-repo</id>
            <name>ahmetaa Maven Repo on Github</name>
            <url>https://raw.github.com/ahmetaa/maven-repo/master</url>
        </repository>
    </repositories>

And dependecies (For example morphology):

    <dependencies>
        <dependency>
            <groupId>zemberek-nlp</groupId>
            <artifactId>morphology</artifactId>
            <version>0.10.0</version>
        </dependency>
    </dependencies>

### Jar distributions

[Google docs page] (https://drive.google.com/#folders/0B9TrB39LQKZWSjNKdVcwWUxxUm8) page has versions and
separate module and dependent jars.

There is also a single jar file containing all zemberek modules available. Other dependencies (Like Guava) can be found there as well.

### Examples

[Turkish-nlp-examples] (https://github.com/ahmetaa/turkish-nlp-examples)
contains a maven java project with small usage examples.

## Modules

### Core (core)

Core classes such as special Collection classes, Hash functions and helpers.

Maven artifact id : **core**

### Morphology (morphology)

Turkish morphological parsing, disambiguation and generation. [Documentation] (https://github.com/ahmetaa/zemberek-nlp/tree/master/morphology)

Maven artifact id : **morphology**

### Tokenization

Turkish Tokenization and sentence boundary detection. So far only rule based algorithms. [Documentation] (https://github.com/ahmetaa/zemberek-nlp/tree/master/tokenization)

Maven artifact id : **tokenization**

### Hyphenation

Turkish syllabification and hyphenation.

Maven artifact id : **hyphenation**

### Language Identification.

Allows fast identification of text language. [Documentation] (https://github.com/ahmetaa/zemberek-nlp/tree/master/lang-id)

Maven artifact id : **lang-id**

### Language modelling

Only provides a language compression implementation. [Documentation] (https://github.com/ahmetaa/zemberek-nlp/tree/master/lm)

Maven artifact id : **lm**

### Normalization

**Not yet finished**
So far only provides fast edit-distance dictionary matching. [Documentation.] (https://github.com/ahmetaa/zemberek-nlp/tree/master/normalization)

Maven artifact id : **normalization**

## Known Issues and Limitations
- Project requires Java 8.
- Currently word and sentence parse module operations generates parse graph with each initialization.
So each run in the system takes some seconds. 
- Morphological parsing does not work for some obvious and frequent words.
- Morphological disambiguation is working less accurate then expected.
- Morphological generation may not work for some obvious Stem-Suffix combinations.
- Please see issues section for further issues and feel free to create new ones.

## License
Code is licensed under Apache License, Version 2.0

## Acknowledgements
Please refer to contributors.txt file.
