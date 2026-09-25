package zemberek.normalization;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.WordAnalysis;

public class WordSegmenter {
	
	private TurkishMorphology morphology;
	private int topSuggestionCount = 10;
	
	public WordSegmenter(TurkishMorphology morphology) {
		this.morphology = morphology;
	}
	public WordSegmenter(TurkishMorphology morphology, int topSuggestionCount) {
		this(morphology);
		this.topSuggestionCount = topSuggestionCount;
	}

	public List<String> wordBreak(String s) {
		return wordBreaker(s, this.topSuggestionCount);
	}
	public List<String> wordBreak(String s, int suggestCount) {
		return wordBreaker(s, suggestCount);
	}

	private List<String> wordBreaker(String s, int topSuggestionCount) {
	    ArrayList<String> [] pos = new ArrayList[s.length()+1];
	    pos[0]=new ArrayList<String>();
	 
	    int lastIndex = 0;
	    for(int i=0; i<s.length(); i++){
	        if(pos[i]!=null){
	            for(int j=i+1; j<=s.length(); j++){
	                String sub = s.substring(i,j);
	                if(sub.length() > 2) {
	                	WordAnalysis analyze = morphology.analyze(sub);
		                if(analyze.isCorrect()){
		                    if(pos[j]==null){
		                        ArrayList<String> list = new ArrayList<String>();
		                        list.add(sub);
		                        pos[j]=list;
		                    }else{
		                        pos[j].add(sub);
		                    }
		                    lastIndex = j;
		                }
	                }
	            }
	        }
	    }
	 
	    if(pos[lastIndex]==null){
	        return new ArrayList<String>();
	    }else{
			LinkedHashSet<String> resultSet = new LinkedHashSet<String>();
			dfs(pos, resultSet, "", s.length());
	        List<String> result = resultSet.stream().collect(Collectors.toList());
	        if(!result.isEmpty() && result.get(0).isEmpty()) {
	        	result.clear();
	        }
	        if(result.size() < topSuggestionCount){
				return result;
			} else {
				return result.subList(0, topSuggestionCount);
			}
	    }
	}
	 
	private void dfs(ArrayList<String> [] pos, Set<String> result, String curr, int i){
	    if(i==0){
	        result.add(curr.trim());
	        return;
	    }

	    if(pos[i] == null){
			dfs(pos, result, curr, i-1);
		} else {
			for(String s: pos[i]){
				String combined = s + " "+ curr;
				dfs(pos, result, combined.trim().replaceAll("\\s+", " "), i-s.length());
			}
		}
	}
}
