package geoCrunch.geoRef;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Place {
	
	private LinkedHashMap<String,String> hierarchicPlace;
	
	public Place() {
		// use a linked HashMap to preserve key order
		this.hierarchicPlace = new LinkedHashMap<String,String>() {
			private static final long serialVersionUID = -7849660714838102733L;
			{
				put("place", null);
		        put("city", null);
		        put("county", null);
		        put("region", null);
		        put("state", null);
				put("country", null);
		       	put("continent", null);
		    }
		};
	}
	
	public HashMap<String,String> getMap() {
		for(Map.Entry<String, String> entry : this.hierarchicPlace.entrySet())
			if(entry.getValue() != null) return this.hierarchicPlace;	
		return null;
	}

	public void put(String key, String value) {
		// While adding values to hierarchicPlace make sure, that
		// the same value does not occur more than once on several stages.
		Boolean valuePresent = false;
		for(Map.Entry<String, String> entry : this.hierarchicPlace.entrySet()) {
			if(value.equalsIgnoreCase(entry.getValue())) {
				valuePresent = true;
				break;
			}
		}
		if(!valuePresent) this.hierarchicPlace.put(key, value);
	}

	public String get(String key) {
		return this.hierarchicPlace.get(key);
	}
}
