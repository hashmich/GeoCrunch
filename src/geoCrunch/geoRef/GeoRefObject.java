package geoCrunch.geoRef;

public class GeoRefObject {
	
	
	public GeoRefObject(GeoRefSource source, String detector) {
		this.geoRefSource = source;
		this.detector = detector;
	}
	
	public GeoRefObject(GeoRefSource source, String detector, String excerpt) {
		this.geoRefSource = source;
		this.detector = detector;
		this.excerpt = excerpt;
	}
	
	public GeoRefObject(String query, GeoRefSource source, String detector) {
		this.query = query;
		this.geoRefSource = source;
		this.detector = detector;
	}
	
	public GeoRefObject(String query, Place place, GeoRefSource source, String detector) {
		this.query = query;
		this.queryMap = place;
		this.geoRefSource = source;
		this.detector = detector;
	}
	
	
	public String detector;		// the class that delivered this result (geocoder or coordinate detector)
	public String query;
	public Place queryMap = null;
	public String name;			// a readable alias of query. necessary if query is a map
	public double lon;
	public double lat;
	public double alt;
	public String refsys;
	public RefsysSource refSource;
	// where the reference is coming from (eg. filename, content or metadata)
	public GeoRefSource geoRefSource;
	public Integer line;
	public String excerpt = "";
	
	@Override
	public boolean equals(Object other) {
	    if (!(other instanceof GeoRefObject)) 		return false;
	    // custom equality check
	    GeoRefObject that = (GeoRefObject) other;
	    if(!this.detector.equals(that.detector)) 	return false;
	    if(!this.name.equals(that.name)) 			return false;
	    if(this.lon != that.lon) 					return false;
	    if(this.lat != that.lat) 					return false;
	    if(!this.excerpt.equals(that.excerpt)) 		return false;
	    
	    if(this.line != null && that.line != null) {
	    	if(!this.line.equals(that.line)) 		return false;
	    }else{
	    	if(this.line == null && that.line == null) return true;
	    }
	    
	    return true;
	}
	
}
