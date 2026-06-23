package com.buldreinfo.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.buldreinfo.config.AppConfig;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.LocalizedObjectAnnotation;
import com.google.protobuf.ByteString;

import jakarta.annotation.PreDestroy;

@Service
public class ImageClassifierService {
	public record AnalysisResult(String hexColor, List<EntityAnnotation> labels, List<LocalizedObjectAnnotation> objects) {}
	private final ImageAnnotatorClient client;

	public ImageClassifierService(AppConfig appConfig) throws Exception {
		ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
				.setCredentialsProvider(NoCredentialsProvider.create())
				.setHeaderProvider(() -> Collections.singletonMap("X-Goog-Api-Key", appConfig.googleApikey()))
				.build();
		this.client = ImageAnnotatorClient.create(settings);
	}

	public AnalysisResult analyze(byte[] imgBytesArray) throws Exception {
		ByteString imgBytes = ByteString.copyFrom(imgBytesArray);
		Image img = Image.newBuilder().setContent(imgBytes).build();

		AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
				.addFeatures(Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION))
				.addFeatures(Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION))
				.addFeatures(Feature.newBuilder().setType(Feature.Type.IMAGE_PROPERTIES))
				.setImage(img)
				.build();

		AnnotateImageResponse res = client.batchAnnotateImages(List.of(request)).getResponsesList().get(0);

		if (res.hasError()) {
			throw new RuntimeException("Vision API error: " + res.getError().getMessage());
		}

		String hexColor = "#000000";
		if (res.hasImagePropertiesAnnotation() && !res.getImagePropertiesAnnotation().getDominantColors().getColorsList().isEmpty()) {
			ColorInfo color = res.getImagePropertiesAnnotation().getDominantColors().getColors(0);
			hexColor = "#%02x%02x%02x".formatted(
					(int)color.getColor().getRed(), 
					(int)color.getColor().getGreen(), 
					(int)color.getColor().getBlue()
					);
		}
		return new AnalysisResult(hexColor, res.getLabelAnnotationsList(), res.getLocalizedObjectAnnotationsList());
	}

	@PreDestroy
	public void cleanup() {
		if (client != null) {
			client.close();
		}
	}
}