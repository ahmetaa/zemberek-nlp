CHANGE LOG
==========

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

