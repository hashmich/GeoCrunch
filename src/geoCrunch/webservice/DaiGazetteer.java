package geoCrunch.webservice;


import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import geoCrunch.Core;

import geoCrunch.geoRef.GeoRefObject;
import geoCrunch.geoRef.GeoRefSource;
import geoCrunch.geoRef.Place;
import geoCrunch.geoRef.RefsysSource;
import geoCrunch.webservice.generic.GeocodingService;

public class DaiGazetteer extends Geocoder {

	private List<String> requestKeys = null; 
	
	private void _construct() {
		// see documentation: http://dai-softsource.uni-koeln.de/projects/gazetteer/wiki/API
		this.requestKeys = Arrays.asList("q", "limit", "offset", "fq", "sort", "order", "type", "showInReview");
	}
	
	public DaiGazetteer(Core core, String query, GeoRefSource source) {
		super(core, query, source, "DaiGazetteer");
		this.result.refsys = "WGS 84";
		this.result.refSource = RefsysSource.IMPLICIT;
		this._construct();
	}

	public DaiGazetteer(Core core, Place queryMap, GeoRefSource source) {
		super(core, queryMap, source, "DaiGazetteer");
		this.result.refsys = "WGS 84";
		this.result.refSource = RefsysSource.IMPLICIT;
		this._construct();
	}

	@Override
	public GeoRefObject query() {
		GeocodingService service = new GeocodingService(this.requestKeys, "https://gazetteer.dainst.org/search.json");
		// we're not using authenticated requests and go content with fuzzy results for protected places
		// country scope is not supported
		JsonNode response = null;
		JsonNode location = null;
		String query;
		if(this.queryMap != null) {
			service.put("split_on_whitespace", "false");
			service.put("limit", "1");
			for(Entry<String, String> item : this.queryMap.entrySet()) {
				if(item.getValue() == null) continue;
				query = item.getValue().replaceAll("[+=&|><!(){}\\[\\]\\^\"~*?:/]+", " ").replaceAll("-", "\\-");
				service.put("q","title:" + query);
				response = service.query();
				if(response != null) {
					location = response.path("result").path(0);
					// if we have a result, go through with it
					if(!location.isMissingNode()) break;
				}
			}
		}else{
			// as of elasticsearch query syntax, reserved characters: 
			// https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#_reserved_characters
			query = this.query.replaceAll("[+=&|><!(){}\\[\\]\\^\"~*?:/]+", " ").replaceAll("-", "\\-");
			service.put("q","title:" + query);
			service.put("split_on_whitespace", "false");
			service.put("limit", "1");
			response = service.query();
		}
		
		if(response != null) {
			location = response.path("result").path(0);
			
			if(location.isMissingNode()) return null;
			if(location.path("prefName").path("title").isMissingNode()) return null;
			if(location.path("prefLocation").path("coordinates").isMissingNode()) return null;
				
			this.result.query = this.query;
			this.result.name = location.path("prefName").path("title").toString();
			this.result.lon = location.path("prefLocation").path("coordinates").path(0).doubleValue();
			this.result.lat = location.path("prefLocation").path("coordinates").path(1).doubleValue();
			
			return this.result;
		}
		
		return null;
	}
	
	

}
