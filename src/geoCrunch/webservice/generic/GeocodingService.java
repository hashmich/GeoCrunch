package geoCrunch.webservice.generic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
		//UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		StringBuilder sb = new StringBuilder();
		sb.append(url);
		int i = 0;
		for (Map.Entry<String, String> entry : this.requestMap.entrySet()) {
			//builder.queryParam(entry.getKey(), entry.getValue());
			if(i == 0) sb.append("?");
			else sb.append("&");
			sb.append(entry.getKey() + "=" + entry.getValue());
			i++;
		}
		
		//URI uri = builder.build(true).toUri();
		String uri = sb.toString();
		System.out.println(uri);
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
