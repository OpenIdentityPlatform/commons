package org.forgerock.commons.ui.functionaltests.utils;

import java.io.StringWriter;
import java.io.Writer;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonUtils {
	
	private ObjectMapper m = new ObjectMapper();
	
	public JsonNode readJsonFromFile(String path) {
		try {
			return m.readValue(JsonUtils.class.getResourceAsStream(path), JsonNode.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public JsonNode readJsonFromString(String json) {
		try {
			return m.readValue(json, JsonNode.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String jsonToString(JsonNode object) {
		Writer w = new StringWriter();
		
		try {
			m.writeValue(w, object);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return w.toString();
		
	}
	
}
