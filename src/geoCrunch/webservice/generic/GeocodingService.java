package geoCrunch.webservice.generic;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;



public class GeocodingService {

	protected static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);

	protected String url = "";
	
	protected RestTemplate restTemplate;
	
	protected Map<String, String> requestMap = new LinkedHashMap<String, String>();
	
	protected List<String> requestKeys = null;
	
	public JsonNode result = null;

	
	
	public GeocodingService(List<String> keys, String url) {
		this.requestKeys = keys;
		this.restTemplate = new RestTemplate();
		this.url = url;
	}

	
	
	
	public void put(String key, String value) {
		if(this.requestKeys.contains(key)) {
			requestMap.put(key, value);
		}
	}

	

	
	public JsonNode query() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		for (Map.Entry<String, String> entry : this.requestMap.entrySet()) {
			builder.queryParam(entry.getKey(), entry.getValue());
		}
		URI uri = builder.build().toUri();
		ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
		
		if(!response.getStatusCode().equals(HttpStatus.OK)) return null;
		
		String stringResult = response.getBody();
		ObjectMapper mapper = new ObjectMapper();
		
		if(stringResult == null) return null;
		
		try {
			this.result = mapper.readTree(stringResult);
			//mapper.readValue(stringResult, Map.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return this.result;
	}

	
}
