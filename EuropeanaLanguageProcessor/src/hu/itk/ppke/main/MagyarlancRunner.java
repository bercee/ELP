package hu.itk.ppke.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.swing.SwingWorker;

import hu.itk.ppke.main.WordCollection.Word;
import utils.AlfanumComparator;

public class MagyarlancRunner extends SwingWorker<WordCollection, String>{
	
	public enum Input{
		FromFileOrFolder, 
		FromMetadata;
	}
	
	public enum Output{
		Basic, 
		None;
	}

	private List<String> inputList = new ArrayList<>();
	@SuppressWarnings("unused")
	private File f;
	private List<List<List<String>>> parsedList = new ArrayList<List<List<String>>>();
	private volatile int status = 0;
//	private HashSet<Word> words = new HashSet<Word>();
//	private HashMap<Word, Word> words = new HashMap<Word, Word>();
	private WordCollection words = new WordCollection();
	
	private static boolean isInitialized = false;
	
	private String magyarlancURL;
	private URLClassLoader loader;
	@SuppressWarnings("rawtypes")
	private static Class magyarlanc;
	@SuppressWarnings("rawtypes")
	private static Class stringCleaner;
	@SuppressWarnings("rawtypes")
	private static Class splitter;
	private static Method morphParseSentence;
	private static Method splitToArray;
	private static Method cleanString;
	@SuppressWarnings("unused")
	private static Method init;
	private static Object mySplitterInst;
	private static Object stringCleanerInst;
	@SuppressWarnings("rawtypes")
	private static Class myPurePos;
	private static Method getInstance;
	@SuppressWarnings("rawtypes")
	private static Class resourceHolder;
	private static List<Method> initMethods = new ArrayList<>();
	
	
//	public static void main(String[] args) {
//		
//		try {
//			Descriptions d = new Descriptions(new File("voros_okor"), null, OutputFormat.None);
//			
//			MagyarlancRunner mr = new MagyarlancRunner("E:\\Dokumentumok\\IRUN\\magyarlánc\\magyarlanc-3.0.jar", new File("kosztolanyi_desc.txt"));
//			mr.execute();
//			WordCollection wc = mr.get();
//		}catch (Exception e){
//			
//		}
//		
//	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void loadMagyarlanc() throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException{
		URL url = new File(magyarlancURL).toURI().toURL();
		loader = new URLClassLoader(new URL[]{url});
		magyarlanc = loader.loadClass("hu.u_szeged.magyarlanc.Magyarlanc");
		stringCleaner = loader.loadClass("splitter.archive.StringCleaner");
		splitter = loader.loadClass("splitter.MySplitter");
		myPurePos = loader.loadClass("hu.u_szeged.pos.purepos.MyPurePos");
		resourceHolder = loader.loadClass("hu.u_szeged.magyarlanc.resource.ResourceHolder");
		
		
		//init = magyarlanc.getMethod("init");
		
		Method getInst = splitter.getMethod("getInstance");
		mySplitterInst = getInst.invoke(null);
		splitToArray = splitter.getMethod("splitToArray", String.class);
		
		morphParseSentence = magyarlanc.getMethod("morphParseSentence", String[].class);
		
		Constructor c = stringCleaner.getConstructor();
		stringCleanerInst = c.newInstance();
		cleanString = stringCleaner.getMethod("cleanString", String.class);
		
		getInstance = myPurePos.getMethod("getInstance");
		
		initMethods.add(resourceHolder.getMethod("initTokenizer"));
		initMethods.add(resourceHolder.getMethod("initCorpus"));
		initMethods.add(resourceHolder.getMethod("initMSDReducer"));
		initMethods.add(resourceHolder.getMethod("initPunctations"));
		initMethods.add(resourceHolder.getMethod("initRFSA"));
		initMethods.add(resourceHolder.getMethod("initKRToMSD"));
		initMethods.add(resourceHolder.getMethod("initMSDToCoNLLFeatures"));
		initMethods.add(resourceHolder.getMethod("initCorrDic"));
		initMethods.add(resourceHolder.getMethod("initMorPhonDir"));
		
	}
	
	
//	{
//		addPropertyChangeListener(new PropertyChangeListener() {
//			
//			@Override
//			public void propertyChange(PropertyChangeEvent evt) {
//				if (evt.getPropertyName().equals("progress")){
//					if (((Integer) evt.getNewValue()) == 0){
//						System.err.println("Initializing magyarlánc...");
//					}else {
//						System.err.println(getProgress()+"% ("+status+" of "+parsedList.size()+")");
//					}
//				}
//				
//			}
//		});
//	}

	public MagyarlancRunner(String magyarlancURL, List<String> inputList) throws Exception{
		this.inputList = inputList;
		this.magyarlancURL = magyarlancURL;
		loadMagyarlanc();
	}

	public MagyarlancRunner(String magyarlancURL, File f) throws FileNotFoundException, IOException, Exception {
		this.magyarlancURL = magyarlancURL;
		this.f = f;
		inputList = new ArrayList<String>();
		if (f.isDirectory()) {
			File[] fs = f.listFiles();
			List<File> fl = Arrays.asList(fs);
			fl.sort(new AlfanumComparator());
			try {
				for (File file : fl) {
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
					while (br.ready())
						inputList.add(br.readLine());
					br.close();
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		} else if (f.isFile()) {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
			while (br.ready())
				inputList.add(br.readLine());
			br.close();
		} else
			throw new FileNotFoundException("Input file or folder does not exist.");
		loadMagyarlanc();

	}


	/**
	 * This method parses a line and returns a two-dimensional array with the
	 * parsed, tokenized, lemmatized words.
	 * <p>
	 * This is the core method. All other methods have to invoke this one.
	 * <p>
	 * It uses the {@link Magyarlanc#morphParse(String[][])} method.
	 * 
	 * @param line
	 *            the input line
	 * @return a two dimensional array with the parsed words.
	 */
	private List<List<String>> getParsedSentence(String line) {
		List<List<String>> list = new ArrayList<List<String>>();
		try{
//		line = new StringCleaner().cleanString(line.trim());
//		line = cleanString(line.trim());
		line = (String) cleanString.invoke(stringCleanerInst, line);
//		String[][] sentences = MySplitter.getInstance().splitToArray(line);
//		String[][] sentences = splitToArray(line);
		String[][] sentences = (String[][]) splitToArray.invoke(mySplitterInst, line);
		for (String[] sentence : sentences) {
//			String[][] morphed = Magyarlanc.morphParseSentence(sentence);
//			String[][] morphed = morphParseSentence(sentence);
			String[][] morphed = (String[][]) morphParseSentence.invoke(null, (Object) sentence);
			for (int j = 0; j < morphed.length; j++) {
				list.add(Arrays.asList(morphed[j]));
			}
		}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return list;
	}
	
	public LinkedHashMap<String, Integer> getWordMap(){
		LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
		
		
		for (String lemma : words.map.keySet()){
			
			for (String lexForm : words.map.get(lemma).formsTable.keySet()){
				HashMap<Integer, Integer> freqMap = words.map.get(lemma).formsTable.get(lexForm);
				int count = 0;
				for (Integer i : freqMap.keySet())
					count+=freqMap.get(i);
				map.put(lexForm, count);
			}
		}
		
		return map;
	}

	@Override
	protected WordCollection doInBackground() throws Exception {
		if (isCancelled())
			return words;
		firePropertyChange("progress", 1, 0);
		publish("Initializing magyarlánc...");	
		if (!isInitialized){
			isInitialized = true;
			for (Method m : initMethods){
				if (isCancelled())
					return words;
				m.invoke(null);
//				publish("...");
				
			}
		}
		getInstance.invoke(null);
		publish("Initialization done.");
		int c = 1;
		for (String s : inputList){
			if (isCancelled())
				return words;
			status = c++;
			publish("Parsing "+status + " of " + inputList.size());
			parsedList.add(getParsedSentence(s));
			setProgress((status*100 ) / inputList.size());
		}
		publish("Doing word frequency analysis...");
		for (int objNum = 0; objNum < parsedList.size();objNum++){
			for (List<String> l : parsedList.get(objNum)){
				words.addNew(l.get(1), l.get(0), l.get(2), objNum);
			}
			
		}
		publish("NLP done.");
		
		return words;
	}
	
	public List<String> getBasicInfo(){
		List<String> l = new ArrayList<>();
		l.add("Number of words: "+words.getSize());
		l.add("Number of words with at least 2 different lexical forms: "+words.getWordsWithDiffLexForm());
		l.add("Number of words with at least 2 different lexical forms that occur in different objects: "+words.getWordsWihtAmbigousOccurrence());
		return l;
	}
	
	public List<String> getFullInfo(){
		List<String> l = new ArrayList<>();
		l.add("[lemma] (number of all occurrences) - word type");
		l.add("\t[lexical form]");
		l.add("\t\t[contanied in in object] - [how many times]");
		l.add("");
		
		words.map.entrySet().stream()
		.sorted(new Comparator<Entry<String, Word>>() {

			@Override
			public int compare(Entry<String, Word> o1, Entry<String, Word> o2) {
				return Integer.compare(o2.getValue().getAllOccurrenceNum(), o1.getValue().getAllOccurrenceNum());
			}
		}).forEach(new Consumer<Entry<String,Word>>() {

			@Override
			public void accept(Entry<String, Word> t) {
				String lemma = t.getKey();
//				Pattern p = Pattern.compile("^[a-zA-Z]");
//				Matcher m = p.matcher(lemma);
//				if (!m.find())
//					return;
				Word word = words.map.get(lemma);
				if (word.type.equals("PUNCT"))
					return;
				l.add(lemma + "  (" + t.getValue().getAllOccurrenceNum()+")" + "  "+word.type);
				HashMap<String, HashMap<Integer, Integer>> lexForms = word.formsTable;
				for (String form : lexForms.keySet()){
					l.add(new StringBuilder("\t").append(form).toString());
					for (Integer objNum : lexForms.get(form).keySet()){
						l.add(new StringBuilder("\t\t").append(objNum).append(" - ").append(lexForms.get(form).get(objNum)).toString());
					}
				}
			}
		});
		
		
//		for (String lemma : words.map.keySet()){
//			Pattern p = Pattern.compile("^[a-zA-Z]");
//			Matcher m = p.matcher(lemma);
//			if (!m.find())
//				continue;
//			l.add(lemma);
//			Word word = words.map.get(lemma);
//			HashMap<String, HashMap<Integer, Integer>> lexForms = word.formsTable;
//			for (String form : lexForms.keySet()){
//				l.add(new StringBuilder("\t").append(form).toString());
//				for (Integer objNum : lexForms.get(form).keySet()){
//					l.add(new StringBuilder("\t\t").append(objNum).append(" - ").append(lexForms.get(form).get(objNum)).toString());
//				}
//			}
//			
//		}
		
		return l;
	}
	
	@Override
	protected void process(List<String> chunks) {
		for (String s : chunks)
			System.err.println(s);
	}
	
//	private static void initialize(){
//		if (!isInitialized){
//	//		Magyarlanc.init();
//			try {
//				Method m = magyarlanc.getMethod("init");
//				m.setAccessible(true);
//				m.invoke(null);
//			} catch (NoSuchMethodException e) {
//				e.printStackTrace();
//			} catch (SecurityException e) {
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				e.printStackTrace();
//			} catch (IllegalArgumentException e) {
//				e.printStackTrace();
//			} catch (InvocationTargetException e) {
//				e.printStackTrace();
//			}
//			
//			isInitialized = true;
//		}
//			
//	}
	
	
//	private String[][] splitToArray(String line){
//		try {
//		//	Constructor c = splitter.getConstructor();
//			Method getInst = splitter.getMethod("getInstance");
//			Object mySplitterInst = getInst.invoke(null);
//			Method m = splitter.getMethod("splitToArray", String.class);
//			m.setAccessible(true);
//			return (String[][]) m.invoke(mySplitterInst, line);
//		} catch (NoSuchMethodException e) {
//			e.printStackTrace();
//		} catch (SecurityException e) {
//			e.printStackTrace();
//		}catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.getTargetException().printStackTrace();
//			e.printStackTrace();
//		}finally {
//		}
//		return null;
//	}
//
//	
//	private String[][] morphParseSentence(String[] sentence){
//		try {
//			Method morphParseSentence = magyarlanc.getMethod("morphParseSentence", String[].class);
//			return (String[][]) morphParseSentence.invoke(null, (Object) sentence);
//		} catch (NoSuchMethodException e) {
//			e.printStackTrace();
//		} catch (SecurityException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		}finally{
//		}
//		return null;
//	}
//	
//	private String cleanString(String line){
//		try {
//			Constructor c = stringCleaner.getConstructor();
//			Object stringCleanerInst = c.newInstance();
//			Method cleanString = stringCleaner.getMethod("cleanString", String.class);
//			cleanString.setAccessible(true);
//			return (String) cleanString.invoke(stringCleanerInst, line);
//			
//		} catch (NoSuchMethodException e) {
//			e.printStackTrace();
//		} catch (SecurityException e) {
//			e.printStackTrace();
//		} catch (InstantiationException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		}
//		return null;
//		
//	}
//	

}
