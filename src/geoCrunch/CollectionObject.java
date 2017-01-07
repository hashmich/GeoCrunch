package geoCrunch;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import geoCrunch.geoRef.Coordinate;
import geoCrunch.geoRef.CoordinateParser;
import geoCrunch.geoRef.GeoRefObject;
import geoCrunch.geoRef.GeoRefSource;
import geoCrunch.geoRef.Place;
import geoCrunch.webservice.Geocoder;
import edu.stanford.nlp.util.Triple;





public class CollectionObject {
	
	private Core core;
	private String[] geocoderPrecedence;
	private Boolean parseToXml = false;
	private String body;
	private FieldDataReader fieldReader;
	
	public long size = 0;
	public String readable_size = "0,00 Byte";
	
	public String pathId;
	public String name;
	public Boolean is_dir;
	public Boolean is_file;
	public String absolutepath;
	public String webpath;
	public String collectionpath;
	
	public Integer directories_count = 0;
	public Integer files_count = 0;
	public Integer cumulated_directory_count = 0;
	public Integer cumulated_file_count = 0;
	
	public String tika_mediatype;
	public Map<String,String> tika_metadata;
	
	public List<GeoRefObject> name_results;
	
	public Place metadata_place;
	public GeoRefObject metadata_place_result;
	public Coordinate metadata_coordinate;
	public GeoRefObject metadata_coordinate_result;
	
	public String[] content_place;
	public List<GeoRefObject> content_place_result;
	public List<GeoRefObject> content_coordinate_result;
	
	public List<CollectionObject> files;
	public List<CollectionObject> directories;
	
	
	
	public CollectionObject(Core core, File file, String pathId) {
		this.core = core;
		this.name = file.getName();
		this.pathId = pathId;
		
		this.absolutepath = file.getAbsolutePath();
		String root = this.core.getInputDirectory().getParentFile().getAbsolutePath();
		this.collectionpath = this.absolutepath.substring(root.length() + 1);
		this.webpath = this.collectionpath.replace("\\", "/");
		
		this.files = new ArrayList<CollectionObject>();
		this.directories = new ArrayList<CollectionObject>();
		
		this.geocoderPrecedence = this.core.getApplicationProps().getProperty("geocoderPrecedence").split(",\\s*");
			
		this.name_results = this.processFileName(file);
		
		if(file.isDirectory()) {
			this.is_dir = true;
		}else{
			this.is_dir = false;
			this.size = file.length();
			this.readable_size = convertFilesize(this.size);
			
			// read in the file with Tika
			this.body = null;
			try{
				body = this.tikaParse(file);
			}catch(IOException | SAXException | TikaException e) {
				e.printStackTrace();
			}
			
			this.fieldReader = new FieldDataReader(this.core, this.tika_metadata);
			this.metadata_place = this.fieldReader.findPlacenames();
			this.metadata_place_result = this.queryWebservices(this.metadata_place, GeoRefSource.METADATA_MENTION);
			
			this.metadata_coordinate = fieldReader.findCoordinate();
			this.metadata_coordinate_result = CoordinateParser.parseCoordinate(this.metadata_coordinate, GeoRefSource.METADATA_COORDINATE);
			
			
			this.content_coordinate_result = new ArrayList<GeoRefObject>();
			this.content_place_result = new ArrayList<GeoRefObject>();
			
			if(this.tika_mediatype.toLowerCase().contains("csv")) {
				this.parseCsv();
			}
			// TODO: XML handling for other tabular data
			// TODO: enable NER for additional text-based file formats
			if(this.tika_mediatype.equals("application/pdf")) {
				this.parseNamedEntities();
			}
		}
		this.is_file = !this.is_dir;
	}
	
	
	
	public void addDescendant(CollectionObject descendant) {
		this.size += descendant.size;
		this.readable_size = convertFilesize(this.size);
		if(descendant.is_dir) {
			this.cumulated_directory_count += descendant.cumulated_directory_count + 1;
			this.cumulated_file_count += descendant.cumulated_file_count;
			this.directories_count += 1;
			this.directories.add(descendant);
		}else{
			this.files_count += 1;
			this.cumulated_file_count += 1;
	    	this.files.add(descendant);
		}
	}
	
	
	public static String convertFilesize(long bytes) {
		String result = "0,00 Byte";
		Map<String, Double> cases = new HashMap<String, Double>();
		cases.put("TB", Math.pow(1024,4));
		cases.put("GB", Math.pow(1024,3));
		cases.put("MB", Math.pow(1024,2));
		cases.put("KB", (double) 1024);
		cases.put("Byte", (double) 1);
		
		for(Map.Entry<String, Double> item : cases.entrySet()) {
			if(bytes >= item.getValue()) {
				result = String.valueOf(round(bytes / item.getValue(), 2)) + " " + item.getKey();
				break;
			}
		}
		return result;
	}
	
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    long factor = (long) Math.pow(10, places);
	    value = value * factor;
	    long tmp = Math.round(value);
	    return (double) tmp / factor;
	}
	
	
	private List<GeoRefObject> processFileName(File file) {
		List<GeoRefObject> resultList = new ArrayList<GeoRefObject>();
		String name = file.getName();
		// remove the extension
		if(!file.isDirectory() && name.indexOf(".") > 0)
		    name = name.substring(0, name.lastIndexOf("."));
		
		String[] tokens = this.core.splitName(name);
		
		// iterate the fragments
		GeoRefObject result;
		for(String token : tokens) {
			result = this.getGeoRef(token, GeoRefSource.FILENAME_MENTION);
			if(result != null) resultList.add(result);
		}
		
		return resultList;
	}
	
	
	private GeoRefObject getGeoRef(String name, GeoRefSource source) {
		Object tmp = null;
		// check the cache
		tmp = this.core.nameCache.get(name.toLowerCase());
		if(tmp == null) {
			// token has not been processed before, perform queries and store the result
			GeoRefObject result = this.queryWebservices(name, source);
			
			if(result != null) {
				// check the bounding box, discard if outside the box
				String[] boundingBox = this.core.getApplicationProps().getProperty("boundingBox", "").split(",\\s*");
				if(boundingBox.length == 4) {
					Double north, east, south, west;
					north = Double.parseDouble(boundingBox[0]);
					east = Double.parseDouble(boundingBox[1]);
					south = Double.parseDouble(boundingBox[2]);
					west = Double.parseDouble(boundingBox[3]);
					if(result.lat > north || result.lat < south || result.lon > east || result.lon < west) {
						this.core.nameCache.put(name.toLowerCase(), "out of box");
						return null;
					}
				}
				// store result in cache
				this.core.nameCache.put(name.toLowerCase(), result);
				return result;
				
			}else{
				this.core.nameCache.put(name.toLowerCase(), "non-spatial");
			}
			
		}else{
			// use the cached result
			if(tmp instanceof String) {
				return null;
			}
			else if(tmp instanceof GeoRefObject) {
				// update the source
				((GeoRefObject) tmp).geoRefSource = source;
			}	
		}
		return (GeoRefObject) tmp;
	}
	
	
	private GeoRefObject getGeoRef(Triple<String, Integer, Integer> item, GeoRefSource source) {
		Object tmp = null;
		String name = this.body.substring(item.second(), item.third());
		tmp = this.getGeoRef(name, source);
		
		if(tmp instanceof GeoRefObject)
			((GeoRefObject) tmp).excerpt = this.getExcerpt(item);
		
		return (GeoRefObject) tmp;
	}
	
	
	private String getExcerpt(Triple<String, Integer, Integer> item) {
		String excerpt;
		Integer begin = 0;
		if(item.second() > 100) begin = item.second() - 100;
		Integer end = this.body.length() - 1;
		if(item.third() < this.body.length() - 100) end = item.third() + 100;
		excerpt = this.body.substring(begin, item.second() - 1) + " <span class=\"highlight\">"
				+ this.body.substring(item.second(), item.third()) + "</span> "
				+ this.body.substring(item.third() + 1, end);
		
		return excerpt;
	}
	
	
	private String tikaParse(File file) throws IOException, SAXException, TikaException {
		String body = null;
		ContentHandler handler;
		TikaInputStream stream = TikaInputStream.get(file.toPath());
		Metadata metadata = new Metadata();
		metadata.set(Metadata.RESOURCE_NAME_KEY, file.getName());
		AutoDetectParser parser = new AutoDetectParser();
		ParseContext context = new ParseContext();
		
		String mediaType = null;
		Detector detector = new DefaultDetector();
	    mediaType = detector.detect(stream, metadata).toString();
	    this.tika_mediatype = mediaType;
	    
	    String[] parseToXml = {"xls"};
		for(int i = 0; parseToXml.length > i; i++)
	    	if(this.tika_mediatype.contains(parseToXml[i])) this.parseToXml = true;
	    
		if(this.parseToXml)
			handler = new ToXMLContentHandler();
		else
			handler = new BodyContentHandler(-1);
		parser.parse(stream, handler, metadata, context);
		body = handler.toString();
	    
	    Map <String, String> map = new HashMap<String, String>();
		String[] names = metadata.names();
		for(String key : names) {
			map.put(key, metadata.get(key));
		}
		this.tika_metadata = map;
		
		return body;
	}
	
	
	private void parseCsv() {
		if(Boolean.parseBoolean(this.core.getApplicationProps().getProperty("parseCsv")) == false)
			return;
		GeoRefObject result = null;
		CsvParser csv = new CsvParser(this.body);
		String[] keys = csv.getKeys();
		if(keys != null) {
			Coordinate keyset = this.fieldReader.getCoordinateKeyset(keys);
			if(keyset != null) {
				Map<String,String> row;
				Coordinate coordinate;
				int line = csv.getDataOffset();
				while((row = csv.getRow()) != null) {
					coordinate = this.fieldReader.getRowValues(row, keyset);
					if(coordinate != null) {
						result = CoordinateParser.parseCoordinate(coordinate, GeoRefSource.CONTENT_COORDINATE);
						if(result != null) {
							result.line = line;
							for(Map.Entry<String,String> entry : row.entrySet())
								result.excerpt +=  entry.getKey() + ": " + entry.getValue() + "<br>";
							this.content_coordinate_result.add(result);
						}
					}
					line++;
				}
			}
			// else: no geo column found
		}
	}
	
	
	private void parseNamedEntities() {
		if(Boolean.parseBoolean(this.core.getApplicationProps().getProperty("parseNamedEntities")) == false)
			return;
		
		GeoRefObject result;
		NERecognizer classifier = new NERecognizer(this.core, this.name, this.body);
		if(classifier.results != null) {
			for(Triple<String, Integer, Integer> item : classifier.results) {
				result = this.getGeoRef(item, GeoRefSource.CONTENT_MENTION);
				if(result != null) {
					// some entity mentions sometimes show up multiple times... (strange!)
					Boolean existant = false;
					for(int i = 0; i < this.content_place_result.size(); i++) {
						if(result.equals(this.content_place_result.get(i))) {
							existant = true;
							break;
						}
					} 
					if(!existant) this.content_place_result.add(result);
				}
			}
		}
	}
	
	
	private GeoRefObject queryWebservices(Place place, GeoRefSource type) {
		GeoRefObject result = null;
		if(place.getMap() != null && Boolean.parseBoolean(this.core.getApplicationProps().getProperty("enableWebservices", "0"))) {
			for(int i = 0; this.geocoderPrecedence.length > i; i++) {
				Class<?> clazz = null;
				try{
					clazz = Class.forName(this.geocoderPrecedence[i]);
				}catch(ClassNotFoundException e1) {
					e1.printStackTrace();
				}
				Constructor<?> constructor = null;
				try{
					constructor = clazz.getConstructor(Core.class, Place.class, GeoRefSource.class);
				}catch(NoSuchMethodException | SecurityException e2) {
					e2.printStackTrace();
				}
				Geocoder geocoder = null;
				try{
					geocoder = (Geocoder) constructor.newInstance(this.core, place, type);
				}catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e3) {
					e3.printStackTrace();
				}
				result = geocoder.query();
				if(result != null){
					// do not query further webservices
					break;
				}
			}
		}
		return result;
	}


	private GeoRefObject queryWebservices(String query, GeoRefSource type) {
		GeoRefObject result = null;
		if(	query != "" && query != null
		&& 	Boolean.parseBoolean(this.core.getApplicationProps().getProperty("enableWebservices", "0"))) {
			for(int i = 0; this.geocoderPrecedence.length > i; i++) {
				Class<?> clazz = null;
				try{
					clazz = Class.forName(this.geocoderPrecedence[i]);
				}catch(ClassNotFoundException e1) {
					e1.printStackTrace();
				}
				Constructor<?> constructor = null;
				try{
					constructor = clazz.getConstructor(Core.class, String.class, GeoRefSource.class);
				}catch(NoSuchMethodException | SecurityException e2) {
					e2.printStackTrace();
				}
				Geocoder geocoder = null;
				try{
					geocoder = (Geocoder) constructor.newInstance(this.core, query, type);
				}catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e3) {
					e3.printStackTrace();
				}
				result = geocoder.query();
				if(result != null){
					// do not query further webservices
					break;
				}
			}
		}
		return result;
	}
	
	
}
