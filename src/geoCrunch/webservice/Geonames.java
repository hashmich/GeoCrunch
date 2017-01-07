package geoCrunch.webservice;

import java.util.Map;

import org.geonames.InvalidParameterException;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.ToponymSearchResult;
import org.geonames.WebService;

import geoCrunch.Core;
import geoCrunch.geoRef.RefsysSource;
import geoCrunch.geoRef.GeoRefObject;
import geoCrunch.geoRef.GeoRefSource;
import geoCrunch.geoRef.Place;

public class Geonames extends Geocoder {
	
	
	public Geonames(Core core, String query, GeoRefSource source) {
		super(core, query, source, "Geonames");
		this.result.refsys = "WGS 84";
		this.result.refSource = RefsysSource.IMPLICIT;
	}
	
	public Geonames(Core core, Place queryMap, GeoRefSource source) {
		super(core, queryMap, source, "Geonames");
		this.result.refsys = "WGS 84";
		this.result.refSource = RefsysSource.IMPLICIT;
	}
	
	
	
	public GeoRefObject query() {
		if(this.queryMap != null) return this.mappedQuery();
		if(this.query != "" && this.query != null) return this.stringQuery(true);
		return null;
	}
	
	
	private GeoRefObject mappedQuery() {
		String country = null;
		String query = null;
		for(Map.Entry<String, String> entry : this.queryMap.entrySet()) {
			if(entry.getKey() == "continent") continue;
			if(entry.getValue() != "" && entry.getValue() != null) {
				if(entry.getKey() == "country") {
					country = entry.getValue();
				}else{
					query = entry.getValue();
				}
			}
		}
		if(query == null && country != null) {
			query = country;
			country = null;
		}
		
		WebService.setUserName(this.core.getApplicationProps().getProperty("geonamesUser"));
		if(country != null) {
			ToponymSearchCriteria criteria = new ToponymSearchCriteria();
			criteria.setNameEquals(country);
			criteria.setMaxRows(1);
			ToponymSearchResult searchResult = null;
			try{
				searchResult = WebService.search(criteria);
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			if(searchResult.getTotalResultsCount() > 0){
				for (Toponym toponym : searchResult.getToponyms())
					country = toponym.getCountryCode();
			}
			else country = null;
		}
		if(query != null) {
			for(Map.Entry<String, String> entry : this.queryMap.entrySet()) {
				if(entry.getValue() == null || entry.getValue() == "") continue;
				if(entry.getKey() == "continent" || entry.getKey() == "country") break;
				ToponymSearchCriteria criteria = new ToponymSearchCriteria();
				// possible operators: q, name, nameEquals
				criteria.setNameEquals(entry.getValue());
				criteria.setMaxRows(1);
				String countryScope = this.core.getApplicationProps().getProperty("countryScope");
				if(country != null) countryScope = country;
				if(countryScope != null && countryScope != "")
					try{
						criteria.setCountryCode(countryScope);
					}catch (InvalidParameterException e1) {
						e1.printStackTrace();
					}
				
				ToponymSearchResult searchResult = null;
				try{
					searchResult = WebService.search(criteria);
				}catch (Exception e) {
					e.printStackTrace();
				}
				
				if(searchResult.getTotalResultsCount() > 0){
					for (Toponym toponym : searchResult.getToponyms()) {
						this.result.name = toponym.getName();
						this.result.lon = toponym.getLongitude();
						this.result.lat = toponym.getLatitude();
						// geonames.org doesn't return polygons :(
						return this.result;
					}
				}
			}
		}
		return null;
	}
	
	
	private GeoRefObject stringQuery(Boolean scope) {
		WebService.setUserName(this.core.getApplicationProps().getProperty("geonamesUser"));
		ToponymSearchCriteria criteria = new ToponymSearchCriteria();
		criteria.setNameEquals(this.query);
		criteria.setMaxRows(1);
		if(scope) {
			String countryScope = this.core.getApplicationProps().getProperty("countryScope");
			if(countryScope != null && countryScope != "")
				try{
					criteria.setCountryCode(countryScope);
				}catch (InvalidParameterException e1) {
					e1.printStackTrace();
				}
		}
		ToponymSearchResult searchResult = null;
		try{
			searchResult = WebService.search(criteria);
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		if(searchResult.getTotalResultsCount() > 0){
			for (Toponym toponym : searchResult.getToponyms()) {
				this.result.name = toponym.getName();
				this.result.lon = toponym.getLongitude();
				this.result.lat = toponym.getLatitude();
				// geonames.org doesn't return polygons :(
				return this.result;
			}
		}
		return null;
	}
	
}
