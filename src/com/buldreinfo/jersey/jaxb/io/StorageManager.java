package com.buldreinfo.jersey.jaxb.io;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import javax.imageio.ImageIO;

import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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
    private static final String PUBLIC_BASE_URL = "https://climbing-web.se-sto-1.linodeobjects.com/";

    public static StorageManager getInstance() {
    	return INSTANCE;
    }

    public static String getPublicUrl(String objectKey, long versionStamp) {
        if (objectKey != null && objectKey.startsWith("/")) {
            objectKey = objectKey.substring(1);
        }
        String url = PUBLIC_BASE_URL + objectKey;
        if (versionStamp != 0L) {
            url += "?v=" + versionStamp;
        }
        return url;
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
    
    public void deleteObject(String objectKey) {
		s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(BUCKET_NAME)
				.key(objectKey)
				.build());
	}

    public void deleteResizedCache(String prefix) {
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

    public byte[] downloadBytes(String objectKey) throws IOException {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(objectKey)
                .build();
        try (var response = s3Client.getObject(getRequest)) {
            return response.readAllBytes();
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
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    public InputStream getInputStream(String objectKey) {
        return s3Client.getObject(GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(objectKey)
                .build());
    }

    public S3Client getS3Client() {
        return s3Client;
    }

    public void uploadBytes(String objectKey, byte[] data, StorageType type) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(objectKey)
                .contentType(type.getMimeType())
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();
        s3Client.putObject(putRequest, RequestBody.fromBytes(data));
    }

    public void uploadImage(String objectKey, BufferedImage image, StorageType type) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        boolean writerFound = ImageIO.write(image, type.getExtension(), os);
        if (!writerFound) {
            throw new IOException("No writer found for format: " + type.getExtension());
        }
        uploadBytes(objectKey, os.toByteArray(), type);
    }
}