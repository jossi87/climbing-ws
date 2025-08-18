package com.buldreinfo.jersey.jaxb.io;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Rotation;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ImageReader {
	public static class ImageReaderBuilder {
		private Path path;
		private byte[] bytes;
		private String embedVideoUrl;
		private Rotation rotation;
		protected ImageReader build() throws IOException, InterruptedException {
			return new ImageReader(this);
		}
		protected ImageReaderBuilder withBytes(byte[] bytes) {
			this.bytes = bytes;
			return this;
		}
		protected ImageReaderBuilder withEmbedVideoUrl(String embedVideoUrl) {
			this.embedVideoUrl = embedVideoUrl;
			return this;
		}
		protected ImageReaderBuilder withPath(Path path) {
			this.path = path;
			return this;
		}
		protected ImageReaderBuilder withRotation(Rotation rotation) {
			this.rotation = rotation;
			return this;
		}
	}

	protected static ImageReaderBuilder newBuilder() {
		return new ImageReaderBuilder();
	}
	private final BufferedImage jpgBufferedImage;

	private ImageReader(ImageReaderBuilder builder) throws IOException, InterruptedException {
		BufferedImage bufferedImage = null;
		if (builder.bytes != null) {
			try (ByteArrayInputStream stream = new ByteArrayInputStream(builder.bytes)) {
				bufferedImage = ImageIO.read(stream);
			}
		}
		else if (builder.embedVideoUrl != null) {
			bufferedImage = generateEmbedVideoImage(builder.embedVideoUrl);
		}
		else if (builder.path != null) {
			bufferedImage = ImageIO.read(builder.path.toFile());
		}
		else {
			throw new RuntimeException("Invalid builder");
		}
		Preconditions.checkNotNull(bufferedImage);
		if (builder.rotation != null) {
			bufferedImage = Scalr.rotate(bufferedImage, builder.rotation, Scalr.OP_ANTIALIAS);
		}
		this.jpgBufferedImage = convertToJpg(bufferedImage);
	}
	
	public BufferedImage getJpgBufferedImage() {
		return jpgBufferedImage;
	}

	private final BufferedImage convertToJpg(BufferedImage b) {
		boolean isPng = ImageIO.getImageWriters(ImageTypeSpecifier.createFromRenderedImage(b), "png").hasNext();
		if (isPng) {
			BufferedImage newImage = new BufferedImage(b.getWidth(), b.getHeight(), BufferedImage.TYPE_INT_RGB);
			newImage.createGraphics().drawImage(b, 0, 0, Color.BLACK, null);
			return newImage;
		}
		return b;
	}
	
	private BufferedImage generateEmbedVideoImage(String embedUrl) throws MalformedURLException, IOException, InterruptedException {
		String imgUrl = null;
		if (embedUrl.startsWith("https://www.youtube.com/embed/")) {
			String id = embedUrl.replace("https://www.youtube.com/embed/", "");
			imgUrl = "https://img.youtube.com/vi/" + id + "/0.jpg";
		}
		else if (embedUrl.startsWith("https://player.vimeo.com/video/")) {
			String id = embedUrl.replace("https://player.vimeo.com/video/", "");
			try (HttpClient client = HttpClient.newHttpClient()) {
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create("https://vimeo.com/api/v2/video/" + id + ".json"))
						.GET()
						.build();
				HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());
				Preconditions.checkArgument(response.statusCode() == HttpURLConnection.HTTP_OK, "HTTP-" + response.statusCode());
				try (InputStream is = response.body()) {
					try (Reader targetReader = new InputStreamReader(is, Charset.forName("UTF-8"))) {
						JsonArray arr = new Gson().fromJson(targetReader, JsonArray.class);
						JsonObject obj = arr.get(0).getAsJsonObject();
						imgUrl = obj.get("thumbnail_large").getAsString();
					}
				}
			}
		}
		Preconditions.checkArgument(imgUrl != null, "imgUrl is null");
		try (InputStream is = URI.create(imgUrl).toURL().openStream()){
			return ImageIO.read(is);
		}
	}
}