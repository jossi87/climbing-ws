package com.buldreinfo.io;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Rotation;

import com.buldreinfo.helpers.GlobalFunctions;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ImageReader implements AutoCloseable {

	public static class ImageReaderBuilder {
		private byte[] bytes;
		private String embedVideoUrl;
		private Rotation rotation;

		public ImageReader build() throws IOException, InterruptedException {
			return new ImageReader(this);
		}

		public ImageReaderBuilder withBytes(byte[] bytes) {
			this.bytes = bytes;
			return this;
		}

		public ImageReaderBuilder withEmbedVideoUrl(String embedVideoUrl) {
			this.embedVideoUrl = embedVideoUrl;
			return this;
		}

		public ImageReaderBuilder withRotation(Rotation rotation) {
			this.rotation = rotation;
			return this;
		}
	}
	private static final Pattern VIMEO_PATTERN = Pattern.compile("^https://player\\.vimeo\\.com/video/([0-9]+)");

	private static final Pattern YT_PATTERN = Pattern.compile("^https://www\\.youtube\\.com/embed/([a-zA-Z0-9_-]{11})");

	public static ImageReaderBuilder newBuilder() {
		return new ImageReaderBuilder();
	}

	private final BufferedImage jpgBufferedImage;

	private ImageReader(ImageReaderBuilder builder) throws IOException, InterruptedException {
		BufferedImage bufferedImage = null;
		if (builder.bytes != null) {
			try (ByteArrayInputStream stream = new ByteArrayInputStream(builder.bytes)) {
				bufferedImage = ImageIO.read(stream);
			}
		} else if (builder.embedVideoUrl != null) {
			bufferedImage = generateEmbedVideoImage(builder.embedVideoUrl);
		} else {
			throw new RuntimeException("Invalid builder configuration");
		}

		Objects.requireNonNull(bufferedImage, "BufferedImage could not be read");

		if (builder.rotation != null) {
			BufferedImage rotated = Scalr.rotate(bufferedImage, builder.rotation, Scalr.OP_ANTIALIAS);
			if (rotated != bufferedImage) {
				bufferedImage.flush();
			}
			bufferedImage = rotated;
		}

		this.jpgBufferedImage = convertToJpg(bufferedImage);
	}

	@Override
	public void close() {
		if (jpgBufferedImage != null) {
			jpgBufferedImage.flush();
		}
	}

	public BufferedImage getJpgBufferedImage() {
		return jpgBufferedImage;
	}

	private BufferedImage convertToJpg(BufferedImage b) {
		BufferedImage newImage = new BufferedImage(b.getWidth(), b.getHeight(), BufferedImage.TYPE_INT_RGB);
		var g = newImage.createGraphics();
		try {
			g.drawImage(b, 0, 0, Color.WHITE, null); 
		} finally {
			g.dispose();
		}
		b.flush();
		return newImage;
	}

	private BufferedImage generateEmbedVideoImage(String embedUrl) throws IOException, InterruptedException {
		String imgUrl = null;
		Matcher ytMatcher = YT_PATTERN.matcher(embedUrl);
		Matcher vimeoMatcher = VIMEO_PATTERN.matcher(embedUrl);

		if (ytMatcher.find()) {
			String id = ytMatcher.group(1);
			imgUrl = "https://img.youtube.com/vi/" + id + "/0.jpg";
		} else if (vimeoMatcher.find()) {
			String id = vimeoMatcher.group(1);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://vimeo.com/api/v2/video/" + id + ".json"))
					.timeout(Duration.ofSeconds(10))
					.GET()
					.build();
			HttpResponse<String> response = GlobalFunctions.HTTP_CLIENT.send(request, BodyHandlers.ofString());
			Preconditions.checkArgument(response.statusCode() == HttpURLConnection.HTTP_OK, "Vimeo API error status: " + response.statusCode());
			
			JsonArray arr = new Gson().fromJson(response.body(), JsonArray.class);
			JsonObject obj = arr.get(0).getAsJsonObject();
			imgUrl = obj.get("thumbnail_large").getAsString();
		}

		Preconditions.checkArgument(imgUrl != null, "Could not extract video ID or determine thumbnail URL for: " + embedUrl);
		
		URI targetUri = URI.create(imgUrl);
		String host = targetUri.getHost().toLowerCase();
		Preconditions.checkArgument(
			host.endsWith(".youtube.com") || host.equals("youtube.com") ||
			host.endsWith(".vimeo.com") || host.equals("vimeo.com") ||
			host.endsWith(".vimeocdn.com") || host.equals("vimeocdn.com"),
			"Untrusted thumbnail URL origin detected: " + host
		);

		HttpRequest imgRequest = HttpRequest.newBuilder()
				.uri(targetUri)
				.timeout(Duration.ofSeconds(15))
				.GET()
				.build();

		HttpResponse<byte[]> imgResponse = GlobalFunctions.HTTP_CLIENT.send(imgRequest, BodyHandlers.ofByteArray());
		Preconditions.checkArgument(imgResponse.statusCode() == HttpURLConnection.HTTP_OK, "Failed downloading image content stream");
		
		try (ByteArrayInputStream bais = new ByteArrayInputStream(imgResponse.body())) {
			return ImageIO.read(bais);
		}
	}
}