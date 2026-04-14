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
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
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

public final class StorageManager {
	private static final StorageManager INSTANCE = new StorageManager();
	public static final String BUCKET_NAME = "climbing-web";
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

	private final S3Client s3Client;

	private StorageManager() {
		BuldreinfoConfig config = BuldreinfoConfig.getConfig();
		AwsBasicCredentials credentials = AwsBasicCredentials.create(
				config.getProperty(BuldreinfoConfig.PROPERTY_KEY_AKAMAI_ACCESS_KEY), 
				config.getProperty(BuldreinfoConfig.PROPERTY_KEY_AKAMAI_SECRET_KEY)
				);

		this.s3Client = S3Client.builder()
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.endpointOverride(URI.create("https://se-sto-1.linodeobjects.com"))
				.region(Region.of("se-sto-1"))
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
		try {
			s3Client.headObject(HeadObjectRequest.builder()
					.bucket(BUCKET_NAME)
					.key(objectKey)
					.build());
			return true;
		} catch (NoSuchKeyException _) {
			return false;
		}
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
			List<ObjectIdentifier> toDelete = page.contents().stream()
					.map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
					.toList();
			s3Client.deleteObjects(DeleteObjectsRequest.builder()
					.bucket(BUCKET_NAME)
					.delete(Delete.builder().objects(toDelete).build())
					.build());
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
	
	public void uploadImage(String objectKey, BufferedImage image, StorageType type, boolean compress) throws IOException {
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(type.getExtension());
	    if (!writers.hasNext()) {
	    	throw new IOException("No writer found");
	    }
	    ImageWriter writer = writers.next();
	    try (ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
	        writer.setOutput(ios);
	        ImageWriteParam param = writer.getDefaultWriteParam();
	        if (compress && param.canWriteCompressed()) {
	            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	            param.setCompressionQuality(.75f); 
	        }
	        writer.write(null, new IIOImage(image, null, null), param);
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
		s3Client.putObject(putRequest, body);
	}
}