package geoCrunch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvParser {
	
	private String body;
	private List<String>lines;
	private String delimiter = null;
	private String[] keys = null;
	private String[] values = null;
	private Boolean valid = false;
	private Integer headerOffset = null;
	private Integer dataOffset = null;
	private Integer currentRow = null;
	private Integer nextRow = null;
	private String quotes = null;
	
	public CsvParser(String body) {
		this.body = body.trim();
		this.autodetectFormat();
		if(this.valid) {
			String line;
			BufferedReader reader = new BufferedReader(new StringReader(this.body));
			this.lines = new ArrayList<String>();
			try{
				while((line = reader.readLine()) != null) {
					lines.add(line);
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
			this.reset();
		}
	}
	
	
	private void autodetectFormat() {
		String[] delimiters = {"\\s*;\\s*", "\\s*:\\s*", "\\s*\\|\\s*", "\\s*,\\s*", "[^\\S ]+"};
		String[] quotes = {"'","\"","`"};
		
		BufferedReader reader = new BufferedReader(new StringReader(this.body));
		List<String> lines = new ArrayList<String>();
		int keysLength = 0;
		String[] keys = null;
		String[] values = null;
		String line;
		String delimiter;
		Boolean valid = true;
		Boolean dataStart = false;
		
		try{
			int i = 0;
			while((line = reader.readLine()) != null && i <= 5) {
				lines.add(line);
				i++;
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		for(int h = 0; lines.size() > h; h++) {
			line = lines.get(h);
			// ignore leading blank lines
			if(line != null && line.length() > 0) {
				this.headerOffset = h;
				this.dataOffset = this.headerOffset + 1;
				// headerOffset set, now test for dataOffset
				for(int n = this.dataOffset; lines.size() > n; n++) {
					// ignore empty lines
					if(lines.get(n) != null && lines.get(n).length() > 0) {
						dataStart = true;
						break;
					}else{
						this.dataOffset++;
					}
				}
				if(!dataStart) {
					this.dataOffset = null;
					break;
				}
				// test for quotes, at least at the beginning and end of line
				String regex;
				for(int q = 0; quotes.length > q; q++) {
					regex = "^" + quotes[q] + ".+" + quotes[q] + "$";
					if(lines.get(this.dataOffset).matches(regex)) {
						this.quotes = quotes[q];
						break;
					}
				}
				// Idea: if we have more keys than during the last run, 
				// it's likely the delimiter is more appropriate than before.
				// Testing the follow up lines if column count matches key count.
				// The delimiter with the most keys && (at least one || all) tested rows will win. 
				// Test all delimiters for the greatest number of keys that can be identified
				for(int i = 0; delimiters.length > i; i++) {
					valid = true;
					delimiter = delimiters[i];
					if(this.quotes != null) {
						delimiter = this.quotes + delimiter + this.quotes;
						keys = line.replaceAll("^"+this.quotes+"|"+this.quotes+"$", "").split(delimiter);
					}else{
						keys = line.split(delimiter);
					}
					// test if we get more keys than before
					if(keys.length > keysLength) {
						// Whitespace as a delimiter is likely to produce a higher key count, 
						// so only test if all others returned the entire headerline as one key. 
						if(delimiters[i] != "\\s*" || keysLength <= 1) {
							keysLength = keys.length;
							// check if row length corresponds to header length
							for(int n = this.dataOffset; lines.size() > n; n++) {
								// ignore empty lines
								if(lines.get(n) != null && lines.get(n).length() > 0) {
									if(this.quotes != null) {
										delimiter = this.quotes + delimiter + this.quotes;
										values = lines.get(n).replaceAll("^"+this.quotes+"|"+this.quotes+"$", "").split(delimiter);
									}else{
										values = lines.get(n).split(delimiter);
									}
									// check if header columns have the same length as table columns
									if(values.length > keysLength) {
										valid = false;
									}
								}
								if(!valid) {
									break;	// try another delimiter by now
								}
							}
							if(valid) {
								// found a possibly matching delimiter
								this.delimiter = delimiters[i];
								this.keys = keys;
								for(int m = 0; keys.length > m; m++) this.keys[m] = keys[m].trim();
								this.valid = valid;
							}
						}
					}
				}
				// header line found
				break;
			}
		}
	}
	
	public Integer getDataOffset() {
		return this.dataOffset;
	}
	
	public String[] getKeys() {
		return this.keys;
	}
	
	public String getDelimiter() {
		return this.delimiter;
	}
	
	public Map<String, String> getRow() {
		if(this.currentRow == null) return null;
		
		String delimiter = this.delimiter;
		String line = this.lines.get(this.currentRow);
		if(this.quotes != null) {
			delimiter = this.quotes + delimiter + this.quotes;
			// remove leading and trailing quotes
			this.values = line.replaceAll("^"+this.quotes+"|"+this.quotes+"$", "").split(delimiter);
		}else{
			this.values = line.split(delimiter);
		}
		
		Map<String,String> result = new HashMap<String, String>();
		String tmp;
		for(int k = 0; this.keys.length > k; k++) {
			if(this.values.length > k) tmp = this.values[k]; 
			else tmp = "";
			result.put(this.keys[k], tmp);
		}
		
		// advance the line pointer
		if(this.hasNext()) {
			this.currentRow = this.nextRow;
			this.nextRow++;
			if(this.nextRow > this.lines.size()-1) this.nextRow = null;
		}else{
			this.currentRow = null;
		}
		
		return result;
	}
	
	public Boolean hasNext() {
		if(this.nextRow != null) return true;
		return false;
	}
	
	public void reset() {
		this.currentRow = this.dataOffset;
		if(this.dataOffset != null) this.nextRow = this.dataOffset + 1;
		if(this.nextRow > this.lines.size()-1) this.nextRow = null;
	}
}
