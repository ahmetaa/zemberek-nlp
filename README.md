Zemberek-NLP
============

**!!!! Important Notice: I have stopped working with the project indefinitely. Feel free to fork the project !!!!**

Here is the the new home of the Zemberek project.  
Zemberek-NLP is a Natural Language Processing library. Some modules are specifically developed for Turkish language.

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
            <version>0.9.1</version>
        </dependency>
    </dependencies>

### Jar distributions

[Zemberek-NLP-Distributions] (https://github.com/ahmetaa/zemberek-nlp-distributions) page has versions and
separate module and dependent jars.

Alternatively there is a public [Google docs page] (https://drive.google.com/#folders/0B9TrB39LQKZWSjNKdVcwWUxxUm8)
for distributions downloads.

### Examples

[Turkish-nlp-examples] (https://github.com/ahmetaa/turkish-nlp-examples)
contains a maven java project with small usage examples.

## Known Issues and Limitations
- Project requires Java 7.
- Currently word and sentence parse module operations generates parse graph with each initialization.
So each run in the system takes some seconds. We will fix it in the next version with fast serialization of the parse graph.
- Morphological parsing does not work for some obvious and frequent words.
- Morphological disambiguation is working less accurate then expected (Not very usable).
- Morphological generation may not work for some obvious Stem-Suffix combinations.
- Please see issues section for further issues and feel free to create new ones.

## Modules

### Core

Core classes such as special Collection classes, Hash functions and helpers.

### Morphology

Turkish morphological parsing, disambiguation and generation.
[Morphology Documentation] (https://github.com/ahmetaa/zemberek-nlp/tree/master/morphology)

### Tokenization

Turkish Tokenization and sentence boundary detection. So far only rule based algorithms.

### Hyphenation

Turkish syllabification and hyphenation.

### Language modelling

[Language model compression] (https://github.com/ahmetaa/zemberek-nlp/tree/master/lm)

## Acknowledgements
Please refer to contributors.txt file.

Portions of this code has been developed in Tübitak BİLGEM's Speech and Language Technologies Laboratory.

![Bilgem Logo](https://raw.github.com/ahmetaa/zemberek-nlp/master/docs/images/bilgem-logo.png)
