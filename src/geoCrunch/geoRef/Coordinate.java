package geoCrunch.geoRef;

import java.util.HashMap;
import java.util.Map;

public class Coordinate {
	
	private HashMap<String, String> coordinate;
	
	public Coordinate() {
		this.coordinate = new HashMap<String,String>() {
			private static final long serialVersionUID = -3249989639050558243L;
			{
				put("lon",null);
				put("lat",null);
				put("refX",null);
				put("refY",null);
				put("coordinate",null);
				put("reverse",null);
				put("refsys",null);
				put("altitude",null);
			}
		};
	}
	
	public HashMap<String,String> getMap() {
		for(Map.Entry<String, String> entry : this.coordinate.entrySet())
			if(entry.getValue() != null) return this.coordinate;	
		return null;
	}

	public void put(String key, String value) {
		this.coordinate.put(key, value);
	}

	public String get(String key) {
		return this.coordinate.get(key);
	}
}
