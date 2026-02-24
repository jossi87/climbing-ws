package com.buldreinfo.jersey.jaxb.io;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.imgscalr.Scalr;

import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.google.common.base.Preconditions;

public class ImageSaver {
    public static int IMAGE_WEB_WIDTH = 2560;
    public static int IMAGE_WEB_HEIGHT = 1440;

    protected static class ImageSaverBuilder {
        private BufferedImage bufferedImage;
        private String keyOriginalJpg;
        private String keyWebJpg;
        private String keyWebWebP;
        private TiffOutputSet metadata;

        protected ImageSaver save() { return new ImageSaver(this); }

        protected ImageSaverBuilder withBufferedImage(BufferedImage bufferedImage) {
            this.bufferedImage = bufferedImage;
            return this;
        }
        protected ImageSaverBuilder withMetadata(TiffOutputSet metadata) {
            this.metadata = metadata;
            return this;
        }
        protected ImageSaverBuilder withKeyOriginalJpg(String keyOriginalJpg) {
            this.keyOriginalJpg = keyOriginalJpg;
            return this;
        }
        protected ImageSaverBuilder withKeyWebJpg(String keyWebJpg) {
            this.keyWebJpg = keyWebJpg;
            return this;
        }
        protected ImageSaverBuilder withKeyWebWebP(String keyWebWebP) {
            this.keyWebWebP = keyWebWebP;
            return this;
        }
    }

    protected static ImageSaverBuilder newBuilder() { return new ImageSaverBuilder(); }

    private ImageSaver(ImageSaverBuilder builder) {
        Preconditions.checkNotNull(builder.bufferedImage);
        StorageManager storage = StorageManager.getInstance();
        if (builder.keyOriginalJpg != null) {
            CompletableFuture<Void> fOriginal = CompletableFuture.runAsync(() -> {
                try {
                    if (builder.metadata == null) {
                        storage.uploadImage(builder.keyOriginalJpg, builder.bufferedImage, StorageType.JPG);
                    }
                    else {
                        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                            ImageIO.write(builder.bufferedImage, "jpg", baos);
                            try (ByteArrayOutputStream finalOs = new ByteArrayOutputStream()) {
                                new ExifRewriter().updateExifMetadataLossless(baos.toByteArray(), finalOs, builder.metadata);
                                storage.uploadBytes(builder.keyOriginalJpg, finalOs.toByteArray(), StorageType.JPG);
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Original upload failed: " + e.getMessage(), e);
                }
            });
            CompletableFuture<Void> fWeb = CompletableFuture.runAsync(() -> {
                try {
                    BufferedImage webImage = builder.bufferedImage;
                    if (builder.bufferedImage.getWidth() > IMAGE_WEB_WIDTH || builder.bufferedImage.getHeight() > IMAGE_WEB_HEIGHT) {
                        webImage = Scalr.resize(builder.bufferedImage, Scalr.Method.ULTRA_QUALITY, 
                                                Scalr.Mode.AUTOMATIC, IMAGE_WEB_WIDTH, IMAGE_WEB_HEIGHT, Scalr.OP_ANTIALIAS);
                    }
                    storage.uploadImage(builder.keyWebJpg, webImage, StorageType.JPG);
                    storage.uploadImage(builder.keyWebWebP, webImage, StorageType.WEBP);
                    if (webImage != builder.bufferedImage) {
                        webImage.flush();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Web upload failed: " + e.getMessage(), e);
                }
            });
            CompletableFuture.allOf(fOriginal, fWeb).join();
        } else {
            throw new RuntimeException("Invalid builder: Missing S3 keys");
        }
    }
}