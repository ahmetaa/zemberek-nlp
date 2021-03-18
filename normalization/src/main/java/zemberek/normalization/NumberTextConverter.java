package zemberek.normalization;

import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishNumbers;
import zemberek.tokenization.Token;
import zemberek.tokenization.TurkishTokenizer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberTextConverter {

    private TurkishMorphology morphology;
    private String patternToFind = "(\\s\\d+\\s(buçuk)\\s)|(\\s(bir|iki|üç|dört|beş|altı|yedi|sekiz|dokuz|on|yirmi|otuz|kırk|elli|atmış|altmış|yetmiş|seksen|doksan|yüz|bin|milyon|milyar)\\s(buçuk)\\s)";
    private Pattern halfNumberPatternT2N;

    public NumberTextConverter(TurkishMorphology morphology) {
        this.morphology = morphology;
        this.halfNumberPatternT2N = Pattern.compile(patternToFind);
    }

    public String replaceTextualNumberWithNumerically(String sentence) {
        List<List<String>> numbers = collectNumberStrings(sentence);
        String result = convertNumberListToNumericalString(sentence, numbers);
        return result;
    }

    public String replaceNumericallyWithTextualNumber(String sentence) {
        String result = convertNumberListToTextualString(sentence);
        return result;
    }

    public String concatHalfNumberPair(String text) {
        String sentence = " " + text + " ";
        Matcher match = halfNumberPatternT2N.matcher(sentence);

        StringBuilder spacedSentence = new StringBuilder();
        int lastIndex = 0;
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(4);
        while (match.find()) {
            String matchedPart = sentence.substring(match.start(), match.end());
            String[] split = matchedPart.split(" ");
            String newString = "";
            if(split.length > 1) {
                Double bucukNumber = 0.0;
                if(TurkishNumbers.hasNumber(split[1])){
                    Long number = Long.parseLong(split[1]);
                    bucukNumber = number + 0.5;
                } else {
                    bucukNumber = TurkishNumbers.convertToNumber(split[1]) + 0.5;
                }

                newString = matchedPart.substring(0, matchedPart.indexOf(split[1])) + " " + df.format(bucukNumber);
            } else {
                newString = split[0] + " " + matchedPart.substring(split[0].length()) + " ";
            }
            spacedSentence.append(sentence.substring(lastIndex, match.start())).append(" ").append(newString).append(" ");
            lastIndex = match.end();
        }

        if(lastIndex != 0 && lastIndex != sentence.length()) {
            spacedSentence.append(sentence.substring(lastIndex)).append(" ");
        }

        if (spacedSentence.toString().isEmpty()) {
            return text;
        } else {
            return spacedSentence.toString().trim().replaceAll(" +", " ");
        }
    }

    private List<List<String>> collectNumberStrings(String sentence){
        List<WordAnalysis> analyses = morphology.analyzeSentence(sentence);
        SentenceAnalysis result = morphology.disambiguate(sentence, analyses);
        List<List<String>> numbers = new ArrayList<>();
        List<String> number = new ArrayList<>();
        List<SingleAnalysis> bestAnalysis = result.bestAnalysis();
        for (int i = 0; i < analyses.size(); i++) {
            SingleAnalysis sa = bestAnalysis.get(i);
            String lemma = sa.getDictionaryItem().lemma;

            if ((sa.getDictionaryItem().primaryPos == PrimaryPos.Numeral
                    && sa.getDictionaryItem().secondaryPos == SecondaryPos.Cardinal) && !lemma.equals("buçuk")) {
                if(!TurkishNumbers.hasOnlyNumber(lemma)) {
                    number.add(lemma);
                } else {
                    String convertNumberToString = TurkishNumbers.convertNumberToString(lemma);
                    number.addAll(Arrays.asList(convertNumberToString.trim().replaceAll(" +", " ").split(" ")));
                }
            } else {
                if(number.size() > 0) {
                    numbers.add(number);
                    number = new ArrayList<>();
                }
            }
        }
        if(!number.isEmpty()) {
            numbers.add(number);
        }
        return numbers;
    }

    private String convertNumberListToTextualString(String sentence){
        List<zemberek.tokenization.Token> tokens = TurkishTokenizer.ALL.tokenize(sentence);
        String resultText = "";
        for (zemberek.tokenization.Token token: tokens) {
            if(token.getText().equals(" ")){
                resultText += token.getText();
            } else {
                if(TurkishNumbers.hasOnlyNumber(token.getText())){
                    resultText += TurkishNumbers.convertNumberToString(token.getText());
                } else {
                    resultText += token.getText();
                }
            }
        }
        return resultText;
    }

    private String convertNumberListToNumericalString(String sentence, List<List<String>> numbers){
        String resultText = sentence;
        for (List<String> list : numbers) {
            List<List<String>> multipleSubNumbers = getMultipleSubNumbers(list);
            for (List<String> subNumbers : multipleSubNumbers) {
                Long numberValue = TurkishNumbers.convertToNumber(subNumbers.toArray(new String[subNumbers.size()]));

                List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(resultText);
                String combineWords = "";
                for (int i = 0; i < tokens.size(); i++) {
                    long l = TurkishNumbers.convertToNumber(subNumbers.get(0));
                    if(subNumbers.get(0).contains(tokens.get(i).getText()) || (l+"").contains(tokens.get(i).getText())) {
                        int subCounter = 0;
                        boolean isMatch = true;
                        if(i + subNumbers.size() < tokens.size()) {
                            for (int j = i; j < i + subNumbers.size(); j++) {
                                String numberStr = "UNK_NUMBER_FORMAT";
                                try {
                                    numberStr = TurkishNumbers.convertNumberToString(tokens.get(j).getText());
                                } catch (Exception e){}
                                if(!(tokens.get(j).getText().equals(subNumbers.get(subCounter)) || numberStr.equals(subNumbers.get(subCounter)))) {
                                    isMatch = false;
                                    break;
                                }
                                subCounter++;
                            }
                        } else {
                            isMatch = false;
                        }
                        if(isMatch) {
                            combineWords += numberValue + " ";
                            i = i + subNumbers.size() - 1;
                        } else {
                            combineWords += tokens.get(i).getText() + " ";
                        }
                    } else {
                        combineWords += tokens.get(i).getText() + " ";
                    }
                }
                resultText = combineWords.trim();
            }
        }
        resultText = concatHalfNumberPair(resultText);
        return resultText;
    }

    private static List<List<String>> getMultipleSubNumbers(List<String> textualNumbers){
        int startIndex = 0;
        int endIndex = textualNumbers.size();
        boolean isEnd = false;
        List<List<String>> muliptleNumbers = new ArrayList<>();
        int counterForLimit = 0;
        while (!isEnd) {
            counterForLimit++;
            List<String> subList = textualNumbers.subList(startIndex, endIndex);
            String[] array = subList.toArray(new String[endIndex - startIndex]);
            Long numberValue = -1L;
            if(array == null || array.length == 0) {
                numberValue = -1L;
            } else {
                numberValue = TurkishNumbers.convertToNumber(array);
            }

            if(numberValue == -1) {
                endIndex--;
            } else {
                muliptleNumbers.add(textualNumbers.subList(startIndex, endIndex));
                startIndex = endIndex;
                endIndex = textualNumbers.size();
            }
            if(startIndex >= textualNumbers.size() || startIndex == endIndex || counterForLimit > 500) {
                isEnd = true;
            }
        }
        return muliptleNumbers;
    }
}
