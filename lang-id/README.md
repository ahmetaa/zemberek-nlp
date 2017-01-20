Text Language Identification.
============

## Introduction

This library provides a text based language identification algorithm implementation.
Implementation is based on simple character n-gram models. It can identify 62 languages.

## Usage

For general usage, library can be initialized as:

    LanguageIdentifier lid = LanguageIdentifier.fromInternalModels();

This will load all 62 models to memory. After initialization, several identification methods can be called.
 
    lid.identify("Merhaba dünya ve tüm gezegenler.")

Will return the identified language code. In this case, "tr" should return. This method is the most accurate but for large documents slowest one.
 If document size is larger than 100 characters, using a method with sampling is preferable.
  
    lid.identify(inputString, 50);

In this case only 50 samples from the document is collected and scored. There is even a faster method. But using method below
 only makes sense if there are more than 10 models. 
  
    lid.identifyFast(inputString, 50);  

There is also a method for checking if only a part of the text contains a specified language.

    String input = "merhaba dünya ve tüm gezegenler Hola mundo y todos los planetas";
    lid.containsLanguage(input, "tr", 20);  // returns true
    lid.containsLanguage(input, "es", 20);  // returns true    

But if only identifying a small amount of languages is
 required, and their models are available library can be instantiated as 
 
    LanguageIdentifier lid = LanguageIdentifier.fromInternalModelGroup("tr_group");

Here, tr_group contains about 8 languages and a special *uknown* language id.

## Performance. 

Below are the presicion and recall numbers for Turkish and English languages from 60 different language documents
with 20, 50 and 100 character lengths.


| Lang | P (C=20) | R (C=20) | P (C=50) | R (C=50) | P (C=100) | R (C=100) |
|------|----------|----------|----------|----------|-----------|-----------|
| TR   |  0.9590  | 0.9767   |  0.9953  |  0.9953  |  0.9980   |  0.9988   |
| EN   |  0.9496  | 0.9799   |  0.9944  |  0.9958  |  0.9972   |  0.9985   |

Note: These numbers will likely to change after 1.0 release.

## Speed

For a two model identification test speed numbers are:


|       | 20 characters | 50 characters | 100 characters |
|-------|---------------|---------------|----------------|
| Speed (Docs per sec.) | 130,000  | 52,000  |  26,200  |

Note: These numbers will likely to change after 1.0 release.
