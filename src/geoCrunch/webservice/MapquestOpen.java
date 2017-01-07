package geoCrunch.webservice;

import com.bytebybyte.mapquest.geocoding.service.request.GeocodeRequest;
import com.bytebybyte.mapquest.geocoding.service.request.GeocodeRequestBuilder;
import com.bytebybyte.mapquest.geocoding.service.response.LatLng;
import com.bytebybyte.mapquest.geocoding.service.response.Location;
import com.bytebybyte.mapquest.geocoding.service.response.Response;
import com.bytebybyte.mapquest.geocoding.service.response.Result;
import com.bytebybyte.mapquest.geocoding.service.standard.StandardGeocodingService;
import com.neovisionaries.i18n.CountryCode;

import geoCrunch.Core;
import geoCrunch.geoRef.GeoRefObject;
import geoCrunch.geoRef.GeoRefSource;
import geoCrunch.geoRef.Place;
import geoCrunch.geoRef.RefsysSource;

public class MapquestOpen extends Geocoder {

	public MapquestOpen(Core core, String placename, GeoRefSource source) {
		super(core, placename, source, "MapquestOpen");
		this.result.refsys = "WGS 84";
		this.result.refSource = RefsysSource.IMPLICIT;
	}
	
	public MapquestOpen(Core core, Place queryMap, GeoRefSource source) {
		super(core, queryMap, source, "MapquestOpen");
		this.result.refsys = "WGS 84";
		this.result.refSource = RefsysSource.IMPLICIT;
	}
	

	public GeoRefObject query() {
		// one cannot tweak that API to go for an exact string comparison, 
		// input language cannot be set.
		// workaround 1: increase the minimum name length (...)
		// workaround 2: check for exactMatch (won't return correct matches with lang conversion)
		Integer minLen = Integer.parseInt(this.core.getApplicationProps().getProperty("minimumNameLength", "0"));
		if(minLen < 6) minLen = 6;
		if(this.query.length() < minLen) return null;
		
		GeocodeRequestBuilder queryBuilder = new GeocodeRequestBuilder();
		queryBuilder.key(this.core.getApplicationProps().getProperty("mapquestKey"));
		queryBuilder.location(this.query);
		queryBuilder.maxResults(1);
		
		String countryScope = this.core.getApplicationProps().getProperty("countryScope");
		if(countryScope != null && countryScope != "") {
			// we have to translate the ISO codes into proper country names, CountryCode is a Maven dependency
			CountryCode iso = CountryCode.getByCodeIgnoreCase(countryScope);
			queryBuilder.country(iso.getName());
		}
		
		GeocodeRequest request = queryBuilder.build();
		
		StandardGeocodingService service = new StandardGeocodingService();
		Response response = (Response) service.geocode(request);
		Integer status = response.getInfo().getStatusCode();
		if(status > 0) return null;
		
		Result[] coderResults = response.getResults();
		for(Result coderResult : coderResults) {
			Location[] locations = coderResult.getLocations();
			if(locations.length > 0) {
				
				for(Location location : locations) {
					// we're not interested in company addresses
					if(location.getGeocodeQuality().toString().equalsIgnoreCase("ADDRESS")) return null;
					
					LatLng latLon = location.getLatLng();
					this.result.lat = latLon.getLat();
					this.result.lon = latLon.getLng();
					String newName = "";
					Boolean exactMatch = false;
					// streets: are not of interest
					// neighbourhood
					if(location.getAdminArea6() != null && location.getAdminArea6() != "") {
						newName += location.getAdminArea6();
						if(query.equalsIgnoreCase(location.getAdminArea6())) exactMatch = true;
					}
					// city
					if(location.getAdminArea5() != null && location.getAdminArea5() != "") {
						if(newName != "") newName += ", ";
						newName += location.getAdminArea5();
						if(query.equalsIgnoreCase(location.getAdminArea5())) exactMatch = true;
					}
					// county: not of interest
					// state
					if(location.getAdminArea3() != null && location.getAdminArea3() != "") {
						if(newName != "") newName += ", ";
						newName += location.getAdminArea3();
						if(query.equalsIgnoreCase(location.getAdminArea3())) exactMatch = true;
					}
					// country gives too many false-positives for US, if "US" is the only thing returned...
					if(newName != "" && location.getAdminArea1() != null && location.getAdminArea1() != "") {
						String country = CountryCode.getByCodeIgnoreCase(location.getAdminArea1()).getName();
						if(newName != "") newName += ", ";
						newName += country;
						if(query.equalsIgnoreCase(country)) exactMatch = true;
					}
					if(newName != "") query = newName;
					this.result.name = query;
					
					// workaround 2: check for exactMatch (won't return correct matches with language conversion :(
					if(exactMatch) {
						return this.result;
					}
				}
			}
		}
		
		return null;
	}

}
