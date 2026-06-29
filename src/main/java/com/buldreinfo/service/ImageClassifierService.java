package com.buldreinfo.service;

import com.buldreinfo.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ImageClassifierService {
    public record AnalysisResult(String hexColor, List<String> labels, List<MediaObject> objects) {}
    public record MediaObject(String name, float score, BoundingPoly boundingPoly) {
        public record BoundingPoly(List<NormalizedVertex> normalizedVertices) {}
        public record NormalizedVertex(float x, float y) {}
    }
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ImageClassifierService(AppConfig appConfig, HttpClient httpClient, ObjectMapper objectMapper) {
        this.apiKey = appConfig.googleApikey();
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public AnalysisResult analyze(byte[] imgBytesArray) {
        try {
            String base64 = Base64.getEncoder().encodeToString(imgBytesArray);
            String jsonRequest = String.format(
                "{\"requests\":[{\"image\":{\"content\":\"%s\"},\"features\":[" +
                "{\"type\":\"IMAGE_PROPERTIES\"},{\"type\":\"LABEL_DETECTION\"},{\"type\":\"OBJECT_LOCALIZATION\"}]}]}", 
                base64
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://vision.googleapis.com/v1/images:annotate"))
                    .header("X-Goog-Api-Key", apiKey)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Google Vision API error: " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body()).path("responses").path(0);
            
            return new AnalysisResult(
                parseHexColor(root),
                parseLabels(root),
                parseObjects(root)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze image", e);
        }
    }

    private String parseHexColor(JsonNode root) {
        JsonNode colorNode = root.at("/imagePropertiesAnnotation/dominantColors/colors/0/color");
        if (colorNode.isMissingNode()) return "#000000";
        return String.format("#%02x%02x%02x", 
            Math.round(colorNode.path("red").floatValue()), 
            Math.round(colorNode.path("green").floatValue()), 
            Math.round(colorNode.path("blue").floatValue()));
    }

    private List<String> parseLabels(JsonNode root) {
        List<String> labels = new ArrayList<>();
        for (JsonNode node : root.path("labelAnnotations")) {
            labels.add(node.path("description").asText());
        }
        return labels;
    }

    private List<MediaObject> parseObjects(JsonNode root) {
        List<MediaObject> objects = new ArrayList<>();
        for (JsonNode node : root.path("localizedObjectAnnotations")) {
            List<MediaObject.NormalizedVertex> vertices = new ArrayList<>();
            for (JsonNode v : node.path("boundingPoly").path("normalizedVertices")) {
                vertices.add(new MediaObject.NormalizedVertex(
                    (float) v.path("x").asDouble(), 
                    (float) v.path("y").asDouble()
                ));
            }
            objects.add(new MediaObject(
                node.path("name").asText(),
                (float) node.path("score").asDouble(),
                new MediaObject.BoundingPoly(vertices)
            ));
        }
        return objects;
    }
}