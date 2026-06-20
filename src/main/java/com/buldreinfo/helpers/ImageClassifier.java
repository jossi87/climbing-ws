package com.buldreinfo.helpers;

import java.util.Collections;
import java.util.List;

import com.buldreinfo.config.BuldreinfoConfig;
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

public class ImageClassifier {
	public record AnalysisResult(String hexColor, List<EntityAnnotation> labels, List<LocalizedObjectAnnotation> objects) {}

	private static volatile ImageAnnotatorClient cachedClient;

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			ImageAnnotatorClient client = cachedClient;
			if (client != null) {
				client.close();
			}
		}));
	}

	private static ImageAnnotatorClient getClient() throws Exception {
		ImageAnnotatorClient result = cachedClient;
		if (result == null) {
			synchronized (ImageClassifier.class) {
				result = cachedClient;
				if (result == null) {
					String apiKey = BuldreinfoConfig.getConfig().getProperty(BuldreinfoConfig.PROPERTY_KEY_GOOGLE_APIKEY);
					ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
							.setCredentialsProvider(NoCredentialsProvider.create())
							.setHeaderProvider(() -> Collections.singletonMap("X-Goog-Api-Key", apiKey))
							.build();
					cachedClient = result = ImageAnnotatorClient.create(settings);
				}
			}
		}
		return result;
	}

	public static AnalysisResult analyze(byte[] imgBytesArray) throws Exception {
		ImageAnnotatorClient client = getClient();
		ByteString imgBytes = ByteString.copyFrom(imgBytesArray);
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
			hexColor = String.format("#%02x%02x%02x", (int)color.getColor().getRed(), (int)color.getColor().getGreen(), (int)color.getColor().getBlue());
		}
		return new AnalysisResult(hexColor, res.getLabelAnnotationsList(), res.getLocalizedObjectAnnotationsList());
	}
}