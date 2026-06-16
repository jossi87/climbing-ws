package com.buldreinfo.jersey.jaxb.io;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public final class StorageManager {
	private record CacheEntry(boolean exists, long timestamp) {}
	public static final String BUCKET_NAME = "climbing-web";
	public static final long MAX_IMAGE_UPLOAD_BYTES = 100L * 1024L * 1024L;
	public static final long MAX_VIDEO_UPLOAD_BYTES = 800L * 1024L * 1024L; 
	private static final long CACHE_TTL_MS = Duration.ofMinutes(15).toMillis(); 
	private static final StorageManager INSTANCE = new StorageManager();
	private static final int MAX_CACHE_SIZE = 50000;

	private static final String PROXY_PATH = "/media-proxy/";

	public static String getDirectStorageUrl(String objectKey) {
		String cleanKey = (objectKey != null && objectKey.startsWith("/")) ? objectKey.substring(1) : objectKey;
		return "https://climbing-web.se-sto-1.linodeobjects.com/" + cleanKey;
	}

	public static StorageManager getInstance() {
		return INSTANCE;
	}

	public static String getPublicUrl(String objectKey, long versionStamp) {
		String cleanKey = (objectKey != null && objectKey.startsWith("/")) ? objectKey.substring(1) : objectKey;
		StringBuilder url = new StringBuilder(PROXY_PATH).append(cleanKey);
		if (versionStamp != 0L) {
			url.append("?v=").append(versionStamp);
		}
		return url.toString();
	}
	
	private final Map<String, CacheEntry> existsCache = Collections.synchronizedMap(
			new LinkedHashMap<>(64, 0.75f, true) {
				@Override
				@SuppressWarnings("unused")
				protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
					return size() > MAX_CACHE_SIZE;
				}
			}
	);
	private final S3Client s3Client;
	private final S3Presigner s3Presigner;

	private StorageManager() {
		BuldreinfoConfig config = BuldreinfoConfig.getConfig();
		AwsBasicCredentials credentials = AwsBasicCredentials.create(
				config.getProperty(BuldreinfoConfig.PROPERTY_KEY_AKAMAI_ACCESS_KEY), 
				config.getProperty(BuldreinfoConfig.PROPERTY_KEY_AKAMAI_SECRET_KEY)
		);

		StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
		URI endpointUri = URI.create("https://se-sto-1.linodeobjects.com");
		Region region = Region.of("se-sto-1");
		this.s3Client = S3Client.builder()
				.credentialsProvider(credentialsProvider)
				.endpointOverride(endpointUri)
				.region(region)
				.httpClientBuilder(ApacheHttpClient.builder()
						.maxConnections(100)
						.connectionMaxIdleTime(Duration.ofSeconds(30))
				)
				.build();
		this.s3Presigner = S3Presigner.builder()
				.credentialsProvider(credentialsProvider)
				.endpointOverride(endpointUri)
				.region(region)
				.build();
	}

	public byte[] downloadBytes(String objectKey) throws IOException {
		try (var response = s3Client.getObject(createGetRequest(objectKey))) {
			return response.readAllBytes();
		}
	}

	public void downloadFile(String objectKey, Path destination) throws IOException {
		try (var s3Stream = s3Client.getObject(createGetRequest(objectKey))) {
			Files.copy(s3Stream, destination, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public BufferedImage downloadImage(String objectKey) throws IOException {
		byte[] data = downloadBytes(objectKey);
		try (var is = new ByteArrayInputStream(data)) {
			return ImageIO.read(is);
		}
	}

	public boolean exists(String objectKey) {
		if (objectKey == null) {
			return false;
		}
		long now = System.currentTimeMillis();
		CacheEntry entry = existsCache.get(objectKey);
		if (entry != null && (now - entry.timestamp() < CACHE_TTL_MS)) {
			return entry.exists();
		}
		try {
			s3Client.headObject(HeadObjectRequest.builder()
					.bucket(BUCKET_NAME)
					.key(objectKey)
					.build());
			existsCache.put(objectKey, new CacheEntry(true, now));
			return true;
		} catch (NoSuchKeyException _) {
			existsCache.put(objectKey, new CacheEntry(false, now));
			return false;
		}
	}

	public String generatePresignedPutUrl(String objectKey, String contentType, long contentLength) {
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(BUCKET_NAME)
				.key(objectKey)
				.contentType(contentType)
				.contentLength(contentLength)
				.acl(ObjectCannedACL.PUBLIC_READ)
				.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(Duration.ofMinutes(15))
				.putObjectRequest(putObjectRequest)
				.build();

		existsCache.remove(objectKey);
		return s3Presigner.presignPutObject(presignRequest).url().toString();
	}

	public InputStream getInputStream(String objectKey) {
		return s3Client.getObject(createGetRequest(objectKey));
	}

	public S3Client getS3Client() {
		return s3Client;
	}

	public void invalidateCache(String prefix) {
		ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
				.bucket(BUCKET_NAME)
				.prefix(prefix)
				.build();
		for (ListObjectsV2Response page : s3Client.listObjectsV2Paginator(listRequest)) {
			if (!page.hasContents()) {
				continue;
			}
			List<ObjectIdentifier> allIdentifiers = page.contents().stream()
					.map(s3Object -> {
						existsCache.remove(s3Object.key());
						return ObjectIdentifier.builder().key(s3Object.key()).build();
					})
					.toList();
			
			for (List<ObjectIdentifier> chunk : Lists.partition(allIdentifiers, 1000)) {
				s3Client.deleteObjects(DeleteObjectsRequest.builder()
						.bucket(BUCKET_NAME)
						.delete(Delete.builder().objects(chunk).build())
						.build());
			}
		}
	}

	public byte[] readBoundedStream(InputStream is) throws IOException {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[16 * 1024];
			long total = 0;
			int read;
			while ((read = is.read(buffer)) != -1) {
				total += read;
				Preconditions.checkArgument(
						total <= MAX_IMAGE_UPLOAD_BYTES, 
						"File too large (max %s bytes)", 
						MAX_IMAGE_UPLOAD_BYTES
				);
				os.write(buffer, 0, read);
			}
			return os.toByteArray();
		}
	}

	public void uploadBytes(String objectKey, byte[] data, StorageType type) {
		if (data == null || data.length == 0) {
			throw new IllegalArgumentException("Byte array is empty for key: " + objectKey);
		}
		uploadRequestBody(objectKey, RequestBody.fromBytes(data), type);
	}

	public void uploadFile(String objectKey, Path path, StorageType type) throws IOException {
		if (Files.size(path) == 0) {
			throw new IllegalArgumentException("File is empty for key: " + objectKey);
		}
		uploadRequestBody(objectKey, RequestBody.fromFile(path), type);
	}

	public void uploadImage(String objectKey, BufferedImage image, StorageType type) throws IOException {
		boolean shouldCompress = objectKey.startsWith("web/");
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(type.getExtension());
		if (!writers.hasNext()) {
			throw new IOException("No writer found for type: " + type.getExtension());
		}
		ImageWriter writer = writers.next();
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
			writer.setOutput(ios);
			ImageWriteParam param = writer.getDefaultWriteParam();
			if (shouldCompress && param.canWriteCompressed()) {
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				if (type == StorageType.WEBP) {
					String[] types = param.getCompressionTypes();
					if (types != null) {
						for (String t : types) {
							if (t.equalsIgnoreCase("Lossy")) {
								param.setCompressionType(t);
								break;
							}
						}
					}
					param.setCompressionQuality(0.75f); 
				} else {
					String[] types = param.getCompressionTypes();
					if (types != null && types.length > 0) {
						param.setCompressionType(types[0]); 
					}
					param.setCompressionQuality(0.80f);
				}
			}
			writer.write(null, new IIOImage(image, null, null), param);
			ios.flush(); 
		} finally {
			writer.dispose();
		}
		uploadBytes(objectKey, os.toByteArray(), type);
	}

	private GetObjectRequest createGetRequest(String objectKey) {
		return GetObjectRequest.builder()
				.bucket(BUCKET_NAME)
				.key(objectKey)
				.build();
	}

	private void uploadRequestBody(String objectKey, RequestBody body, StorageType type) {
		PutObjectRequest putRequest = PutObjectRequest.builder()
				.bucket(BUCKET_NAME)
				.key(objectKey)
				.contentType(type.getMimeType())
				.acl(ObjectCannedACL.PUBLIC_READ)
				.build();
		existsCache.remove(objectKey);
		s3Client.putObject(putRequest, body);
	}
}