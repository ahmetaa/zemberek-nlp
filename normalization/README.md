Turkish Text Normalization
============

This module will contain Turkish text normalization methods. So far it only contain a simple
dictionary based single word distance matching mechanism. Later versions will have more advanced 
mechanisms for normalization and spelling suggestion.

Current code does not do much, here is an example:

        SingleWordSpellChecker spellChecker = new SingleWordSpellChecker(1);
        spellChecker.addWords("çak", "sak", "sek", saka", "bak", "çaka", "çakal", "sakal");
        List<String> result = spellChecker.getSuggestionsSorted("çak");
        
result will contain:

        [çak, çaka, bak, sak]
                        
But there is no language model based scoring, de-asciifier etc. This only gives possible matches from a
 given dictionary of words with a certain distance. 



