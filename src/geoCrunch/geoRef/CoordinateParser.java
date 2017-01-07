package geoCrunch.geoRef;

public class CoordinateParser {
	
	
	
	public static GeoRefObject parseCoordinate(Coordinate metadata_coordinate, GeoRefSource source) {
		if(metadata_coordinate == null) return null;
		GeoRefObject result = null;
		String tmp;
		if(metadata_coordinate.get("lon") != null && metadata_coordinate.get("lat") != null) {
			tmp = metadata_coordinate.get("refX");
			char cardinal = 'e';
			if(tmp != null) cardinal = tmp.toLowerCase().toCharArray()[0];
			double lon = parsePolarOrdinate(metadata_coordinate.get("lon"), cardinal);
			
			tmp = metadata_coordinate.get("refY");
			cardinal = 'n';
			if(tmp != null) cardinal = tmp.toLowerCase().toCharArray()[0];
			double lat = parsePolarOrdinate(metadata_coordinate.get("lat"), cardinal);
			
			if(lon == 1000 || lat == 1000) return null;
			result = new GeoRefObject(source, "CoordinateParser");
			result.lon = lon;
			result.lat = lat;
		}
		else if(metadata_coordinate.get("coordinate") != null) {
			// TODO parse complete coordinate strings
		}
		
		if(result != null) {
			tmp = metadata_coordinate.get("refsys");
			if(tmp != null) { 
				result.refsys = tmp;
				result.refSource = RefsysSource.EXPLICIT;
			}else{
				result.refsys = "WGS 84";
				result.refSource = RefsysSource.ASSUMED;
			}
			
			tmp = metadata_coordinate.get("altitude");
			if(tmp != null) {
				try{
					result.alt = Double.parseDouble(tmp);
				}catch(NumberFormatException e) {
				    //e.printStackTrace();
				}
			}
		}
		
		return result;
	}
	
	/**
	 * @param value
	 * @param char checkCardinal, check if result is in allowed range of [n,s,w,e]
	 * @return double, 1000 on error (out of range)
	 */
	public static double parsePolarOrdinate(String value, char checkCardinal) {
		// separators
		boolean seenDot = false;
	    boolean seenComma = false;
	    // collect the preceding integer digits and check if in allowed axis range (x: 180, y: 90)
	    String finalValue = "";
	    boolean seenCardinal = false;
	    boolean seenDigit = false;
	    char cardinal = 0;
	    char degFractions[] = "′″‴'\"".toCharArray();
	    
	    if(occurs(degFractions, value)) {
	    	// TODO convert to decimal degree
	    }
	    
	    for(int i=0; i < value.length(); i++) {
	        char c = value.charAt(i);
	        if(c >= '0' && c <= '9') {
	            seenDigit = true;
	            finalValue += c;
	            continue;
	        }
	        if((c == '-' || c=='+') && i == 0) {
	            finalValue += c;
	            continue;
	        }
	        if(c == '.' && !seenDot && !seenComma) {
	            seenDot = true;
	            finalValue += '.';
	            continue;
	        }
	        if(c == ',' && !seenDot && !seenComma) {
	            seenComma = true;
	            finalValue += '.';
	            continue;
	        }
	        if(c == '°' && !occurs(degFractions, value)) {
	        	continue;
	        }
	        if(c == ' ' ) {
	        	// allow spaces only after leading sign and before a trailing cardinal point
	        	if(i == 1 && occurs(value.charAt(0), "+-")) continue;
	        	if(i == value.length() - 2 && !occurs(value.charAt(i+1), "nswe", false)) continue;
	        }
	        if(occurs(c, "nswe", false) && !seenCardinal && i == value.length() - 1) {
	        	seenCardinal = true;
	        	cardinal = Character.toLowerCase(c);
	            continue;
	        }
	        // found illegal char or sequence
	        return 1000;
	    }
	    if(!seenDigit) {
	        return 1000;
	    }
	    
	    double result = 1000;
	    try{
	        result = Double.parseDouble(finalValue);
	    }catch(NumberFormatException e) {
	        return 1000;
	    }
	    
	    if(cardinal != 0) {
	    	if(occurs(cardinal, "ns"))
	    		if(result >= 90 || result <= -90) return 1000;
	    	if(occurs(cardinal, "we"))
	    		if(result >= 180 || result <= -180) return 1000;
	    }
	    if(result >= 180 || result <= -180) return 1000;
	    
	    if(checkCardinal != 0) {
		    checkCardinal = Character.toLowerCase(checkCardinal);
		    if(occurs(checkCardinal, "nswe")) {
		    	if(occurs(checkCardinal, "ns"))
		    		if(result >= 90 || result <= -90) result = 1000;
		    	if(occurs(checkCardinal, "we"))
		    		if(result >= 180 || result <= -180) result = 1000;
		    	if(cardinal != 0 && checkCardinal != cardinal) {
		    		if(occurs(checkCardinal, "ns") && occurs(cardinal, "we")) return 1000;
		    		if(occurs(checkCardinal, "we") && occurs(cardinal, "ns")) return 1000;
		    	}
		    }
	    }
	    
	    // convert to standard conform easting and northing
	    if(checkCardinal != 0) cardinal = checkCardinal;
	    if(occurs(cardinal, "ws")) result = -result;
	    
	    return result;
	}
	
	
	public static boolean occurs(char needle, String haystack) {
		return occurs(needle, haystack, true);
	}
	
	public static boolean occurs(char needle, String haystack, boolean casitive) {
	    for(int i = 0; i < haystack.length(); i++) {
	    	if(needle == haystack.charAt(i)) return true;
	    	if(!casitive && Character.toLowerCase(needle) == Character.toLowerCase(haystack.charAt(i)))
	    		return true;
	    }
		return false;
	}
	
	public static boolean occurs(char[] needle, String haystack) {
		for(int i = 0; i < needle.length; i++) {
			if(occurs(needle[i], haystack)) return true;
		}
		return false;
	}
	
	
	
	
}
