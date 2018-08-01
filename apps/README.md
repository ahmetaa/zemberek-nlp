Zemberek Applications
============

## Introduction

This module contains console applications that can be run from the terminal. 

## Running applications

Easiest way to run the applications is to use zemberek jar file with dependencies.

    java -jar zemberek-with-dependencies.jar
    
This will print available applications. Output may look like this:     

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

For running any application, add the name to the previous command:

    java -jar zemberek-with-dependencies.jar MorphologyConsole

