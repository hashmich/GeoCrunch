package geoCrunch;

import java.util.HashMap;
import java.util.Map;

import geoCrunch.geoRef.Coordinate;
import geoCrunch.geoRef.Place;


public class FieldDataReader {
	
	private Core core;
	private Map<String,String> metadata;
	
	private String[][] coordinates = {
		{"lonlat","longlat","coord","coordinates","koord","koordinate","koordinaten"},
		{"latlon","latlong"}	// reverse!
	};
	
	// koo_refsys: ADEX
	private String[] refsys = {"epsg","crs","crf","crfs","gps map datum","kartendatum","datum","koo_refsys"};
	
	private String[] altitude = {"gps altitude","altitude","alt","elevation","höhe"};
	
	private String[][] coordinatePairs = {
		{"lon","lat"},
		{"long","lat"},
		{"longitude","latitude"},
		{"länge","breite"},
		{"geo:long","geo:lat"},
		{"gps longitude","gps latitude","gps longitude ref","gps latitude ref"},
		{"x_koord","y_koord"},	// ADEX
		{"x_wert","y_wert"},
		{"e_wert","n_wert"},
		{"east","north"}
	};
	
	
	// we have to preserve key order, use LinkedHashMap instead of Map
	private Place hierarchicPlace;
	private Map<String,String[]> placeStopWords;
	private Coordinate coordinate;
	

	
	private void instantiatePlaceStopWords() {
		this.placeStopWords = new HashMap<String,String[]>() {
			private static final long serialVersionUID = -1675426898721380926L;
			{
		        put("continent", new String[]{"continent","kontinent"});
		        put("country", new String[]{"country","land","staat"});
		        put("state", new String[]{"state","province","bundesland"});
		        put("region", new String[]{"region"});
		        put("county", new String[]{"county","kreis"});
		        put("city", new String[]{"city","village","stadt"});
		        put("place", new String[]{"place","spot","findspot","ort","standort","standpunkt","fundort"});
		    }
		};
	}
	
	
	
	
	public FieldDataReader(Core core, Map<String,String> metadata) {
		this.core = core;
		this.metadata = metadata;
		this.hierarchicPlace = new Place();
		this.instantiatePlaceStopWords();
	}
	
	
	


	
	public Place findPlacenames() {
		for(Map.Entry<String, String[]> stopList : this.placeStopWords.entrySet()) {
			for(int n = 0; stopList.getValue().length > n; n++) {
				for(Map.Entry<String, String> entry : this.metadata.entrySet()) {
					// simple string comparison on stop words
					if(entry.getKey().equalsIgnoreCase(stopList.getValue()[n])) {
						this.hierarchicPlace.put(stopList.getKey(), entry.getValue());
						break;
					}
					// as we don't need to look out for corresponding pairs, partial matches are also allowed
					// split the field names into their components, as some stop words might get false positives ("sort" matches "ort")
					// use strict splitting (second parameter true)
					Boolean match = false;
					String[] tokens = this.core.splitName(entry.getKey(), true);
					for(int t = 0; tokens.length > t; t++) {
						if(tokens[t].equalsIgnoreCase(stopList.getValue()[n])) {
							match = true;
							break;
						}
					}
					if(match) {
						this.hierarchicPlace.put(stopList.getKey(), entry.getValue());
						break;
					}
				}
				// if we already found a mention for this stage in hierarchy, we don't need another one 
				if(this.hierarchicPlace.get(stopList.getKey()) != null) break;
			}
		}
		
		return this.hierarchicPlace;
	}
	
	
	
	
	
	public Coordinate findCoordinate() {
		Boolean seenLon = false, seenLat = false;
		Boolean seenX = false, seenY = false;
		String lon = null, lat = null;
		String kardX = null, kardY = null;
		this.coordinate = new Coordinate();
		// try finding corresponding pairs first
		for(int i = 0; this.coordinatePairs.length > i; i++) {
			seenLon = seenLat = false;
			seenX = seenY = false;
			
			for(Map.Entry<String, String> entry : this.metadata.entrySet()) {
				// longitude
				if(entry.getKey().equalsIgnoreCase(coordinatePairs[i][0])) {
					lon = entry.getValue();
					seenLon = true;
				}
				if(entry.getKey().equalsIgnoreCase(coordinatePairs[i][1])) {
					lat = entry.getValue();
					seenLat = true;
				}
				if(seenLon && seenLat) {
					this.coordinate.put("lon", lon);
					this.coordinate.put("lat", lat);
					if(this.coordinatePairs[i].length == 2 || (seenX && seenY)) break;
				}
				// fetch cardinals, if any
				if(this.coordinatePairs[i].length == 4) {
					if(entry.getKey().equalsIgnoreCase(this.coordinatePairs[i][2])) {
						kardX = entry.getValue();
						seenX = true;
					}
					if(entry.getKey().equalsIgnoreCase(this.coordinatePairs[i][3])) {
						kardY = entry.getValue();
						seenY = true;
					}
					if(seenX && seenY) {
						this.coordinate.put("refX", kardX);
						this.coordinate.put("lat", kardY);
						if(seenLon && seenLat) break;
					}
				}
			}
			if(seenLon && seenLat) break;
		}
		// go for everything else (single values)
		for(Map.Entry<String, String> entry : this.metadata.entrySet()) {
			// complete coordinate strings
			if(!seenLon || !seenLat) {
				for(int i = 0; this.coordinates[0].length > i; i++) {
					if(entry.getKey().equalsIgnoreCase(this.coordinates[0][i])) {
						this.coordinate.put("coordinate", entry.getValue());
						this.coordinate.put("reverse", "false");
						seenLon = seenLat = true;
						break;
					}
				}
				if(!seenLon || !seenLat) {
					for(int i = 0; this.coordinates[1].length > i; i++) {
						if(entry.getKey().equalsIgnoreCase(this.coordinates[1][i])) {
							this.coordinate.put("coordinate", entry.getValue());
							this.coordinate.put("reverse", "true");
							seenLon = seenLat = true;
							break;
						}
					}
				}
			}
			// coordinate reference system
			if(this.coordinate.get("refsys") == null) {
				for(int i = 0; this.refsys.length > i; i++) {
					if(entry.getKey().equalsIgnoreCase(this.refsys[i])) {
						this.coordinate.put("refsys", entry.getValue());
						break;
					}
				}
			}
			// altitude
			if(this.coordinate.get("altitude") == null) {
				for(int i = 0; this.altitude.length > i; i++) {
					if(entry.getKey().equalsIgnoreCase(this.altitude[i])) {
						this.coordinate.put("altitude", entry.getValue());
						break;
					}
				}
			}
		}
		
		return this.coordinate;
	}
	
	
	public Coordinate getCoordinateKeyset(String[] keys) {
		this.coordinate = new Coordinate();
		Boolean seenLon = false, seenLat = false;
		String lon = null, lat = null;
		// try finding corresponding pairs
		for(int i = 0; this.coordinatePairs.length > i; i++) {
			seenLon = seenLat = false;
			for(int k = 0; keys.length > k; k++) {
				// longitude
				if(keys[k].equalsIgnoreCase(this.coordinatePairs[i][0])) {
					seenLon = true;
					lon = keys[k];
				}
				if(keys[k].equalsIgnoreCase(this.coordinatePairs[i][1])) {
					seenLat = true;
					lat = keys[k];
				}
				if(seenLon && seenLat) {
					this.coordinate.put("lon", lon);
					this.coordinate.put("lat", lat);
					// TODO: return a dedicated keyset structure, we're misusing the coordinate object here
					return this.coordinate;
				}
			}
		}
		
		return null;
	}
	
	
	public Coordinate getRowValues(Map<String,String> row, Coordinate keyset) {
		this.coordinate = new Coordinate();
		HashMap<String,String> map = keyset.getMap();
		for(Map.Entry<String,String> entry : map.entrySet()) {
			this.coordinate.put(entry.getKey(), row.get(entry.getValue()));
		}
		
		return this.coordinate;
	}
	
	
	
}
