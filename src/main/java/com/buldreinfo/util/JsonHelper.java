package com.buldreinfo.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.buldreinfo.model.MediaSvgElement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class JsonHelper {

	private final ObjectMapper objectMapper;

	public JsonHelper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public ObjectNode createObjectNode() {
	    return objectMapper.createObjectNode();
	}
	
	public <T> List<T> parseArray(String json, Class<T[]> clazz) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return List.of(objectMapper.readValue(json, clazz));
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error parsing JSON array to " + clazz.getSimpleName(), e);
		}
	}

	public <T> List<T> parseArray(String json, Class<T[]> clazz, Comparator<T> comparator) {
		List<T> list = parseArray(json, clazz);
		if (comparator != null && !list.isEmpty()) {
			return list.stream().sorted(comparator).toList();
		}
		return list;
	}
	
	public <T> T parseObject(String json, Class<T> clazz) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(json, clazz);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error parsing JSON object to " + clazz.getSimpleName(), e);
		}
	}
	
	public List<MediaSvgElement> parseSvgElements(String json) {
		if (json == null || json.isBlank()) return List.of();
		try {
			List<MediaSvgElement> list = new ArrayList<>();
			for (JsonNode obj : objectMapper.readTree(json)) {
				int id = obj.get("id").asInt();
				if (obj.hasNonNull("path")) {
					list.add(MediaSvgElement.fromPath(id, obj.get("path").asText()));
				} else {
					list.add(MediaSvgElement.fromRappel(id, obj.path("rappelX").asInt(), 
							obj.path("rappelY").asInt(), 
							obj.path("rappelBolted").asBoolean()));
				}
			}
			return list;
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error parsing MediaSvgElements", e);
		}
	}

	public JsonNode parseTree(String json) {
	    if (json == null || json.isBlank()) {
	        return MissingNode.getInstance();
	    }
	    try {
	        return objectMapper.readTree(json);
	    } catch (JsonProcessingException e) {
	        throw new RuntimeException("Error parsing JSON string to tree", e);
	    }
	}

	public String toJson(Object object) {
	    try {
	        return objectMapper.writeValueAsString(object);
	    } catch (JsonProcessingException e) {
	        throw new RuntimeException("Error serializing object to JSON", e);
	    }
	}
}