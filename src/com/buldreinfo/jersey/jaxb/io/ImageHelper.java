package com.buldreinfo.jersey.jaxb.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Rotation;

import com.buldreinfo.jersey.jaxb.db.Dao;
import com.google.common.base.Preconditions;

public class ImageHelper {
	public static void rotateImage(Dao dao, Connection c, int idMedia, Rotation rotation) throws ImageReadException, ImageWriteException, IOException, SQLException, InterruptedException {
		Path original = IOHelper.getPathMediaOriginalJpg(idMedia);
		byte[] bytes = Files.readAllBytes(original);
		ExifReader exifReader = new ExifReader(bytes);
		ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(rotation)
				.build();
		ImageSaver.newBuilder()
		.withBufferedImage(imageReader.getJpgBufferedImage())
		.withMetadata(exifReader.getOutputSet())
		.withPathOriginalJpg(original)
		.withPathWebJpg(IOHelper.getPathMediaWebJpg(idMedia))
		.withPathWebWebP(IOHelper.getPathMediaWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, imageReader.getJpgBufferedImage().getHeight(), imageReader.getJpgBufferedImage().getWidth(), exifReader.getDateTaken());
	}
	
	public static void saveAvatar(int userId, String avatarUrl) throws IOException, InterruptedException {
		Path original = IOHelper.getPathOriginalUsers(userId);
		IOHelper.createDirectories(original.getParent());
		try (InputStream in = URI.create(avatarUrl).toURL().openStream()) {
			Files.copy(in, original, StandardCopyOption.REPLACE_EXISTING);
			IOHelper.setFilePermission(original);
		}
		Path resized = IOHelper.getPathWebUsers(userId);
		IOHelper.deleteIfExistsCreateParent(resized.getParent());
		BufferedImage b = ImageReader.newBuilder().withPath(original).build().getJpgBufferedImage();
		b = Scalr.resize(b, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, 35, 35, Scalr.OP_ANTIALIAS);
		Preconditions.checkArgument(ImageIO.write(b, "jpg", resized.toFile()));
		IOHelper.setFilePermission(resized);
	}

	public static void saveImage(Dao dao, Connection c, int idMedia, BufferedImage bufferedImage) throws IOException, SQLException {
		ImageSaver.newBuilder()
		.withBufferedImage(bufferedImage)
		.withPathOriginalJpg(IOHelper.getPathMediaOriginalJpg(idMedia))
		.withPathWebJpg(IOHelper.getPathMediaWebJpg(idMedia))
		.withPathWebWebP(IOHelper.getPathMediaWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, bufferedImage.getHeight(), bufferedImage.getWidth(), null);
	}

	public static void saveImage(Dao dao, Connection c, int idMedia, byte[] bytes) throws ImageReadException, ImageWriteException, IOException, SQLException, InterruptedException {
		ExifReader exifReader = new ExifReader(bytes);
		ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(exifReader.getRotation())
				.build();
		ImageSaver.newBuilder()
		.withBufferedImage(imageReader.getJpgBufferedImage())
		.withMetadata(exifReader.getOutputSet())
		.withPathOriginalJpg(IOHelper.getPathMediaOriginalJpg(idMedia))
		.withPathWebJpg(IOHelper.getPathMediaWebJpg(idMedia))
		.withPathWebWebP(IOHelper.getPathMediaWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, imageReader.getJpgBufferedImage().getHeight(), imageReader.getJpgBufferedImage().getWidth(), exifReader.getDateTaken());
	}

	public static void saveImageFromEmbedVideo(Dao dao, Connection c, int idMedia, String embedVideoUrl) throws IOException, SQLException, InterruptedException {
		ImageReader imageReader = ImageReader.newBuilder().withEmbedVideoUrl(embedVideoUrl).build();
		ImageSaver.newBuilder()
		.withBufferedImage(imageReader.getJpgBufferedImage())
		.withPathOriginalJpg(IOHelper.getPathMediaOriginalJpg(idMedia))
		.withPathWebJpg(IOHelper.getPathMediaWebJpg(idMedia))
		.withPathWebWebP(IOHelper.getPathMediaWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, imageReader.getJpgBufferedImage().getHeight(), imageReader.getJpgBufferedImage().getWidth(), null);
	}
}