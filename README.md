Zemberek-NLP
============

Zemberek-NLP provides basic Natural Language Processing tools for Turkish.
Please note that **all code and APIs are subject to change until version 1.0.0**

Latest version is 0.14.0 (June 30th 2018). [Change Log](CHANGELOG.md)

## FAQ 

Please read the [FAQ](https://github.com/ahmetaa/zemberek-nlp/wiki/FAQ) for common questions.

## Citing

If you use this project in an academic publication, please refer to this site.

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

And dependencies (For example morphology):

    <dependencies>
        <dependency>
            <groupId>zemberek-nlp</groupId>
            <artifactId>zemberek-morphology</artifactId>
            <version>0.14.0</version>
        </dependency>
    </dependencies>

### Jar distributions

[Google docs page](https://drive.google.com/#folders/0B9TrB39LQKZWSjNKdVcwWUxxUm8) page 
contains jar files for different versions. 

[**module-jars**] folder contain all zemberek modules as separate jar files. 
[**zemberek-all-VERSION.jar**] contains all zemberek modules. [**dependencies**] folder contains other dependencies suchas Google Guava.

### Examples

There is an [examples](examples) module in the code for usage examples.

Also, there is a separate project with same examples that uses Zemberek-NLP as maven modules: 
[Turkish-nlp-examples](https://github.com/ahmetaa/turkish-nlp-examples)

## Modules

### Core

Core classes such as special Collection classes, Hash functions and helpers.

Maven artifact id : **zemberek-core**

### Morphology

Turkish morphological analysis, disambiguation and word generation. [Documentation](morphology)

Maven artifact id : **zemberek-morphology**

### Tokenization

Turkish Tokenization and sentence boundary detection. [Documentation](tokenization)

Maven artifact id : **zemberek-tokenization**

### Normalization

Provides basic spell checker and suggestion functions. [Documentation](normalization)

Maven artifact id : **zemberek-normalization**

### Named Entity Recognition

Basic Named Entity Recognition mechanism. [Documentation](ner)

Maven artifact id : **zemberek-ner**

### Language Identification.

Allows fast identification of text language. [Documentation](lang-id)

Maven artifact id : **zemberek-lang-id**

### Language modeling

Provides a language compression algorithm. [Documentation](lm)

Maven artifact id : **zemberek-lm**

### Examples

Provides basic usage examples. [Source](examples)

Maven artifact id : **zemberek-examples**

## Known Issues and Limitations
- Project requires Java 8 or higher.
- NER module does not provide a model yet.
- Library is not well-tested for multi-threaded usage.

Please see issues section for further issues and feel free to create new ones.

## License
Code is licensed under Apache License, Version 2.0

## Acknowledgements
Please refer to contributors.txt file.
