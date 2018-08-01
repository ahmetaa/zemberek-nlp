Zemberek-NLP
============

Zemberek-NLP provides basic Natural Language Processing tools for Turkish.
Please note that **all code and APIs are subject to change until version 1.0.0**

Latest version is 0.14.0 (June 30th 2018). [Change Log](CHANGELOG.md)

Please read the [FAQ](https://github.com/ahmetaa/zemberek-nlp/wiki/FAQ) for common questions.

## Modules

|  Module    | Maven Id |         |
|------------|----------|---------|
| [Core](core)                    | zemberek-core           | Special Collections, Hash functions and helpers. |
| [Morphology](morphology)        | zemberek-morphology     | Turkish morphological analysis, disambiguation and word generation. |
| [Tokenization](tokenization)    | zemberek-tokenization   | Turkish Tokenization and sentence boundary detection. |
| [Normalization](normalization)  | zemberek-normalization  | Basic spell checker and word suggestion. |
| [NER](ner)                      | zemberek-ner            | Turkish Named Entity Recognition. |
| [Classification](classification)| zemberek-classification | Text classification based on Java port of fastText project. |
| [Language Identification](lang-id)| zemberek-lang-id      | Fast identification of text language. |
| [Language Modeling](lm)         | zemberek-lm             | A language compression algorithm. |
| [Applications](apps)            | zemberek-apps           | Console applications |
| [Examples](examples)            | zemberek-examples       | Usage examples. |

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

[Google drive page](https://drive.google.com/#folders/0B9TrB39LQKZWSjNKdVcwWUxxUm8) page 
contains jar files for different versions. 

[**module-jars**] folder contain all zemberek modules as separate jar files. 

[**zemberek-all-VERSION.jar**] contains all zemberek modules. [**dependencies**] folder contains other dependencies such as Google Guava.

[**zemberek-with-dependencies-VERSION.jar**] is a single jar that contains all modules and dependencies.

### Examples

There is an [examples](examples) module in the code for usage examples.

Also, there is a separate project with same examples that uses Zemberek-NLP as maven modules: 
[Turkish-nlp-examples](https://github.com/ahmetaa/turkish-nlp-examples)

## Known Issues and Limitations
- Project requires Java 8 or higher.
- NER module does not provide a model yet.
- Library is not well-tested for multi-threaded usage.

Please see issues section for further issues and feel free to create new ones.

## License
Code is licensed under Apache License, Version 2.0

## Citing

If you use this project in an academic publication, please refer to this site.

## Acknowledgements
Please refer to contributors.txt file.
