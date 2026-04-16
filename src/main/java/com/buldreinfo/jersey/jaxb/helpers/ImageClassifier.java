package com.buldreinfo.jersey.jaxb.helpers;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;
import com.google.api.gax.core.FixedCredentialsProvider;
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

public class ImageClassifier {
	public record AnalysisResult(String hexColor, List<EntityAnnotation> labels, List<LocalizedObjectAnnotation> objects) {}
	
	public AnalysisResult analyze(String filePath) throws Exception {
		String apiKey = BuldreinfoConfig.getConfig().getProperty(BuldreinfoConfig.PROPERTY_KEY_GOOGLE_APIKEY);
		ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
				.setCredentialsProvider(FixedCredentialsProvider.create(null))
				.setHeaderProvider(() -> Collections.singletonMap("X-Goog-Api-Key", apiKey))
				.build();
		try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
			ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
			Image img = Image.newBuilder().setContent(imgBytes).build();
			Feature labelsF = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
			Feature objectsF = Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION).build();
			Feature propsF = Feature.newBuilder().setType(Feature.Type.IMAGE_PROPERTIES).build();
			AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
					.addFeatures(labelsF).addFeatures(objectsF).addFeatures(propsF)
					.setImage(img).build();
			AnnotateImageResponse res = client.batchAnnotateImages(List.of(request)).getResponsesList().get(0);
			if (res.hasError()) {
				throw new Exception(res.getError().getMessage());
			}
			String hexColor = "#000000";
			if (res.hasImagePropertiesAnnotation() && !res.getImagePropertiesAnnotation().getDominantColors().getColorsList().isEmpty()) {
				ColorInfo color = res.getImagePropertiesAnnotation().getDominantColors().getColors(0);
				hexColor = String.format("#%02x%02x%02x", 
						(int)color.getColor().getRed(), (int)color.getColor().getGreen(), (int)color.getColor().getBlue());
			}
			return new AnalysisResult(hexColor, res.getLabelAnnotationsList(), res.getLocalizedObjectAnnotationsList());
		}
	}
}