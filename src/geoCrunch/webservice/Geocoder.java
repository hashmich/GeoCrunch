package geoCrunch.webservice;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import geoCrunch.Core;
import geoCrunch.geoRef.GeoRefObject;
import geoCrunch.geoRef.GeoRefSource;
import geoCrunch.geoRef.Place;

public abstract class Geocoder {
	protected Core core;
	protected String query;
	protected HashMap<String,String> queryMap = null;
	protected GeoRefSource source;
	protected GeoRefObject result;
	
	public Geocoder(Core core, String query, GeoRefSource source, String detector) {
		this.core = core;
		this.query = query;
		this.source = source;
		this.result = new GeoRefObject(query, source, detector);
	}
	
	public Geocoder(Core core, Place queryMap, GeoRefSource source, String detector) {
		this.query = "";
		if(queryMap.getMap() != null) {
			Iterator<Entry<String, String>> iterator = queryMap.getMap().entrySet().iterator();
			Map.Entry<String, String> entry;
			while(iterator.hasNext()) {
				entry = iterator.next();
				if(entry.getValue() != null) {
					if(this.query != "") this.query += ", ";
					this.query += entry.getValue();
				}
			}
		}
		this.core = core;
		this.queryMap = queryMap.getMap();
		this.source = source;
		this.result = new GeoRefObject(this.query, queryMap, source, detector);
	}
	
	
	/**
	 * It's up to the implementation, how string query or mapped query are handled. 
	 * Also, implementations must obey the country scope and bounding box settings, if possible. 
	 */
	public abstract GeoRefObject query();
	
	
}
