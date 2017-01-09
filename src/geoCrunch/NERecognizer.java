package geoCrunch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.util.Triple;

public class NERecognizer {
	
	private Core core;
	private String name;
	private String body;
	private String language;
	private CRFClassifier<CoreLabel> classifier = null;
	private String locationKey = null;
	
	public List<Triple<String, Integer, Integer>> results = null;
	
	public String getLocationKey() {
		return this.locationKey;
	}
	
	
	
	public NERecognizer(Core core, String name, String body) {
		this.core = core;
		this.name = name;
		this.body = body;
		this.detectLanguage();
		this.loadClassifier();
		this.getAnnotation();
	}
	
	
	
	private void detectLanguage() {
		LanguageDetector langDetector = null;
		try{
			// Optimaize is an implementation of the abstract Tika class LanguageDetector
			langDetector = new OptimaizeLangDetector().loadModels();
		}catch(IOException e) {
			e.printStackTrace();
		}
		String line;
		BufferedReader reader = new BufferedReader(new StringReader(this.body));
		try{
			while((line = reader.readLine()) != null) {
				langDetector.addText(line);
				if(langDetector.hasEnoughText()) break;
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
		this.language = langDetector.detect().getLanguage();
	}
	
	
	
	private void loadClassifier() {
		if(this.language != null && this.language != "") {
			boolean lang = true;
			switch(this.language) {
				case "de": this.locationKey = "I-LOC"; break;
				case "en": this.locationKey = "LOCATION"; break;
				default:
					System.out.println("parseNE: language " + this.language + " is not supported");
					lang = false;
			}
			
			if(lang) {
			String modelPath = "ner_models/" + this.language + ".model.gz";
				if(this.core.nerClassifiers != null && this.core.nerClassifiers.containsKey(this.language)) {
					this.classifier = this.core.nerClassifiers.get(this.language);
				}else{
					try{
						this.classifier = CRFClassifier.getClassifier(modelPath);
					}catch (ClassCastException | ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}
					// store the classifier
					this.core.nerClassifiers.put(this.language, this.classifier);
				}
			}
			
		}else{
			System.out.println("parseNE: language of " + this.name + " could not be detected");
		}
	}
	
	
	public void getAnnotation() {
		if(this.classifier != null) {
			this.results = this.classifier.classifyToCharacterOffsets(this.body);
			Iterator <Triple<String, Integer, Integer>> iter = this.results.iterator();
			while(iter.hasNext()) {
				Triple<String, Integer, Integer> item = iter.next();
				if(!item.first.equals(this.locationKey)) iter.remove();
			}
		}
	}
	
	
	
}





