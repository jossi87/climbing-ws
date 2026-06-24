package com.buldreinfo.service;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.buldreinfo.config.AppConfig;
import com.buldreinfo.model.MediaObject;

@Service
public class ImageClassifierService {

    public record AnalysisResult(String hexColor, List<String> labels, List<MediaObject> objects) {}

    private final String apiKey;
    private final RestClient restClient;

    public ImageClassifierService(AppConfig appConfig) {
        this.apiKey = appConfig.googleApikey();
        this.restClient = RestClient.builder()
                .baseUrl("https://vision.googleapis.com/v1")
                .defaultHeader("Content-Type", "application/json; charset=utf-8")
                .build();
    }

    public AnalysisResult analyze(byte[] imgBytesArray) {
        String base64 = Base64.getEncoder().encodeToString(imgBytesArray);
        String jsonRequest = "{\"requests\":[{\"image\":{\"content\":\"" + base64 + "\"},\"features\":[{\"type\":\"IMAGE_PROPERTIES\"}]}]}";

        String responseBody = restClient.post()
                .uri("/images:annotate?key=" + apiKey)
                .body(jsonRequest)
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.contains("\"error\":")) {
            throw new RuntimeException("API error: " + responseBody);
        }

        return new AnalysisResult(parseHexColor(responseBody), Collections.emptyList(), Collections.emptyList());
    }

    private String parseHexColor(String json) {
        int r = extractRegex(json, "red");
        int g = extractRegex(json, "green");
        int b = extractRegex(json, "blue");

        return String.format("#%02x%02x%02x", r, g, b);
    }

    private int extractRegex(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return 0;

        int colonIndex = json.indexOf(":", keyIndex);
        int commaIndex = json.indexOf(",", colonIndex);
        int braceIndex = json.indexOf("}", colonIndex);
        
        int end = (commaIndex != -1 && commaIndex < braceIndex) ? commaIndex : braceIndex;
        
        String val = json.substring(colonIndex + 1, end).trim();
        try {
            return Math.round(Float.parseFloat(val));
        } catch (NumberFormatException _) {
            return 0;
        }
    }
}