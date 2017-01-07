package geoCrunch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

public class Core {
	
	
	private File inputDirectory;
	private File outputDirectory;
	private String outputFilename;
	public Map<String, CRFClassifier<CoreLabel>> nerClassifiers = null;
	private Properties applicationProps;
	// nameCache entries are either of type GeocoderResult or String (non-spatial), thus use type Object for declaration
	Map<String, Object> nameCache = new HashMap<String, Object>();
	
	CollectionObject resultTree;
	
	
	
	public File getInputDirectory() {
		return this.inputDirectory;
	}
	
	public File getOutputDirectory() {
		return this.outputDirectory;
	}
	
	public String getOutputFilename() {
		return this.outputFilename;
	}
	
	public Properties getApplicationProps() {
		return this.applicationProps;
	}

	
	
	private void loadProps() throws IOException {
		//load properties
		Properties defaultProps = new Properties();
		FileInputStream propInput = new FileInputStream("config/default.properties");
		defaultProps.load(propInput);
		propInput.close();
		// create application properties with default
		this.applicationProps = new Properties(defaultProps);

		// load user properties, if any
		File userProps = new File("config/user.properties");
		if(userProps.exists()) {
			propInput = new FileInputStream(userProps);
			this.applicationProps.load(propInput);
			propInput.close();
		}
	}
	
	private void construct() {
		this.resultTree = null;
		this.nerClassifiers = new HashMap<String, CRFClassifier<CoreLabel>>();
		
		File wd = new File(System.getProperty("user.dir"));
		if(!this.inputDirectory.isAbsolute()) {
			this.inputDirectory = new File(wd, this.inputDirectory.toString());
		}
		if(!this.outputDirectory.isAbsolute()) {
			this.outputDirectory = new File(wd, this.outputDirectory.toString());
		}
		
		this.outputFilename = this.getApplicationProps().getProperty("outputFilename");
	}
	
	public Core() {
		try{
			this.loadProps();
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		this.inputDirectory = new File(this.getApplicationProps().getProperty("inputDirectory"));
		this.outputDirectory = new File(this.getApplicationProps().getProperty("outputDirectory"));
		
		this.construct();
	}
	
	public Core(File inputDirectory) {
		try{
			this.loadProps();
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		this.inputDirectory = inputDirectory;
		this.outputDirectory = new File(this.getApplicationProps().getProperty("outputDirectory"));
		
		this.construct();
	}
	
	public Core(File inputDirectory, File outputDirectory) {
		try{
			this.loadProps();
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		this.inputDirectory = inputDirectory;
		this.outputDirectory = outputDirectory;
		
		this.construct();
	}
	
	
	
	public void main() throws Exception {
		if(!this.inputDirectory.exists()) {
            throw new Exception("input directory does not exist");
        }
		if(!this.outputDirectory.exists()) this.outputDirectory.mkdir();
		
		System.out.println("Scanning directory " + this.inputDirectory.toString());
		this.resultTree = this.readDir(this.inputDirectory, "0");
		
		System.out.println("Writing results to " + new File(this.getOutputDirectory(), this.getOutputFilename()).toString());
		this.writeFileTree();
		
		System.out.println("---------------------------------");
		System.out.println("DONE");
		System.out.println("---------------------------------");
	}
	
	
	private CollectionObject readDir(File inputDirectory, String path) {
		CollectionObject result = new CollectionObject(this, inputDirectory, path);
		Integer files = 0;
		Integer directories = 0;
		for(final File currentFile : inputDirectory.listFiles()) {
			// skip hidden files & output directory (if created inside the collection)
			if(currentFile.isHidden()
					|| currentFile.getName().startsWith(".")
					|| currentFile.getName().startsWith("~")
					|| currentFile.getName() == this.getApplicationProps().getProperty("outputDirectory"))
				continue;
        	
            CollectionObject descendant = null;
            if(currentFile.isDirectory()) {
        		descendant = this.readDir(currentFile, path + ".directories." + directories);
        		result.addDescendant(descendant);
        		directories++;
	       	}else{
	        	descendant = new CollectionObject(this, currentFile, path + ".files." + files);
	        	// pass the result as the ancestor to bump up size and file counters
	        	result.addDescendant(descendant);
	   			files++;
	       	}
	    }
		
		return result;
	}
	
	
	public String[] splitName(String name) {
		return this.splitName(name, false);
	}
	
	
	public String[] splitName(String name, Boolean strict) {
		// split by _., whitespace, numbers, other illegal characters for filenames
		String regex = "[0-9]+|\\s|\\.|[_\\^#@%&$\\*:<>\\?!/\\{\\|\\}+~,;\\[\\]]+";
		// if strict, additionally split by - and camelCased and TITLECased boundaries
		if(strict) regex = "[0-9]+|\\s|\\.|[-_\\^#@%&$\\*:<>\\?!/\\{\\|\\}+~,;\\[\\]]+|(?<=\\p{Ll})(?=\\p{Lu})|(?<=\\p{L})(?=\\p{Lu}\\p{Ll})";
		
		String[] tokens = name.split(regex);
		// remove all leading or trailing hyphens
		if(!strict) {
			for(int i = 0; i < tokens.length; i++) {
				tokens[i] = tokens[i].replaceAll("^[-]+|[-]+$", "");
			}
		}
		// remove empty or too short strings from array
		Integer minLen = Integer.parseInt(this.getApplicationProps().getProperty("minimumNameLength", "3")); 
		String[] ignore = this.getApplicationProps().getProperty("ignoreNames").split(",\\s*");
		return tokens = Arrays.stream(tokens)
				.filter(s -> (s != null && s.length() >= minLen && !Arrays.asList(ignore).contains(s.toLowerCase())))
				.toArray(String[]::new); 
	}
	
	
	private void writeFileTree() throws IOException {
		String json = this.toJson(this.resultTree);
		Writer output = null;
		File file = new File(this.outputDirectory, this.outputFilename);
		output = new BufferedWriter(new FileWriter(file));
		output.write(json);
		output.close();
	}
	
	
	private String toJson(CollectionObject object) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
		String json = null;
		try{
			json = ow.writeValueAsString(object);
		}catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return json;
	}

}
