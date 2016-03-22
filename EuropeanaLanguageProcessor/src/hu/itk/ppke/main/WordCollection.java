package hu.itk.ppke.main;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

public class WordCollection {
	
	TreeMap<String, Word> map = new TreeMap<String, Word>();
	
	
	public synchronized void addNew(String lemma, String lexicalForm, int objNum){
		Word w = map.get(lemma);
		if (w == null){
			map.put(lemma, new Word(lemma, lexicalForm, "", objNum));
		}else{
			w.addLexicalForm(lexicalForm, objNum);
		}
		
	}
	

	public synchronized void addNew(String lemma, String lexicalForm, String type, int objNum){
		Word w = map.get(lemma);
		if (w == null){
			map.put(lemma, new Word(lemma, lexicalForm, type, objNum));
		}else{
			w.addLexicalForm(lexicalForm, objNum);
		}
		
	}
	
	public synchronized int getSize() {
		return map.size();
	}
	
	public synchronized int getWordsWithDiffLexForm(){
		int c = 0;
		
		for (String s : map.keySet()){
			Word w = map.get(s);
			if (w.formsTable.size() >= 2)
				c++;
		}
		
		return c;
	}
	
	public synchronized int getWordsWihtAmbigousOccurrence(){
		int c = 0;
		
		for (String s : map.keySet()){
			Word w = map.get(s);
			HashMap<String, HashMap<Integer,Integer>> formsTable = w.formsTable;
			if (formsTable.size()<2)
				continue;
			HashSet<Integer> containedBy = new HashSet<>();
			boolean broken = false;
			for (String ss : formsTable.keySet()){
				if (broken)
					break;
				HashMap<Integer, Integer> containerTable  = formsTable.get(ss);
				if (containedBy.isEmpty())
					containedBy.addAll(containerTable.keySet());
				else {
					for (Integer i : containerTable.keySet()){
						if (!containedBy.contains(i)){
							c++;
							broken = true;
							break;
						}
					}
				}
			}
		}
		
		return c;
	}
	
	class Word {
		private int c;
		String lemma;
		String type;
//		private HashSet<String> forms = new HashSet<String>();
		HashMap<String, HashMap<Integer, Integer>> formsTable = new HashMap<String, HashMap<Integer, Integer>>();
		
		public Word(String lemma) {
			this.lemma = lemma;
		}
		
		public Word(String lemma, String lexForm, String type, int objNum) {
			this.lemma = lemma;
			this.type = type;
			HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
			map.put(objNum, 1);
			formsTable.put(lexForm, map);
		}
		
		public void addLexicalForm(String lexForm, int objNum){
			HashMap<Integer, Integer> map = formsTable.get(lexForm);
			if (map != null){
				if (map.containsKey(objNum)){
					map.put(objNum, map.get(objNum)+1);
				}else {
					map.put(objNum,1);
				}
			}else {
				map = new HashMap<Integer, Integer>();
				map.put(objNum, 1);
				formsTable.put(lexForm, map);
			}
		}
		
		public int getAllOccurrenceNum(){
			c = 0;
			
			formsTable.entrySet().stream().forEach(e -> e.getValue().entrySet().stream().forEach(ee -> c+=ee.getValue()));
			
			return c;
		}
	}
	
}
