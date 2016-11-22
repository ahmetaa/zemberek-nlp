CHANGE LOG
==========

## 0.9.3

- Improved morphological analysis coverage by cross checking with Oflazer-Analyzer. For this, a list of more than 7 million words is created from a 2 billion word corpora. Then words that can be analyzed only by Oflazer-Analyzer is generated and Zemberek is fixed as much possible.  
- Breaking change: zemberek.morphology.parse package is now zemberek.morphology.analysis #67
- Breaking change: Several classes are renamed. #67 
   TurkishWordParserGenerator -> TurkishMorphology 
   SentenceMorphParse -> SentenceAnalysis
   MorphParse -> WordAnalysis
   WordParser -> WordAnalyzer
- Breaking change: Methods with name "parse" are renamed to "analyze" #67
- Custom Antlr dependency is removed. We now use latest stable Antlr version. We decided this because maintaining a patch of Antlr was time consuming. We were using a patched Antlr version because original version was around 3 times slower. We may remove Antlr dependency alltogether in future releases because tokenization should be less strict and it should not do detailed classification. #68
- Add Oflazer compatible secondary POS information for Postp #65
- Tokenization problem after capital letters after apostrophe #64
- Cannot parse diyebil*, diyecek*, diyen* #61
- Proper nouns should not have Voicing attribute automatically. #57
- Can parse reçelsi but not reçelimsi #54
- Cannot parse maviceydi, yeşilcedir... #53
- Cannot parse "soyadları" #55
- Wrong start and stop indexes for abbreviation words on tokenization. #51
- Fixes in caching mechanism in TurkishMorphology.
- Added a dependency module. It does not perform parsing yet.

## 0.9.2

A lot of internal code changes. Added static and dynamic cache mechanisms for word parsing.

### Some Issues Fixed:
- Can parse [abdye ABDye] but not [abd'ye] [ABD'ye] #44
- Cannot parse words : [ cevaplandırmak çeşitlendirmek ] #42
- System can parse [ankaraya] but not [ankara'ya] #40
- Add ability to add a new Dictionary Item in run-time. #37
- resource test-lexicon-nouns.txt not found #36 (elifkus)
- Garip bir tokenization and stem problemi #30
- Cannot parse the word: yiyen #25 (volkanagun)

## 0.9.0

- First unstable public release.
- Removed language identification and spelling modules. They are different applications now.

