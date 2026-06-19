package com.buldreinfo.jersey.jaxb.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.dao.MediaRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.google.common.base.Stopwatch;

public class VideoHelper {
	private static final Logger logger = LogManager.getLogger();
	private static final String FFMPEG_DEFAULT = "ffmpeg";

	public static void extractThumbnail(TransactionManager txManager, MediaRepository mediaRepo, int idMedia, Path src, int thumbnailSeconds) throws Exception {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Path tempThumb = Files.createTempFile("thumb-" + idMedia, ".jpg");
		try {
			String seekFlag = thumbnailSeconds < 0 ? "-sseof" : "-ss";
			String[] cmd = {FFMPEG_DEFAULT, "-y", "-nostdin", seekFlag, String.valueOf(thumbnailSeconds), "-i", src.toString(), 
							"-t", "00:00:01", "-r", "1", "-f", "mjpeg", tempThumb.toString()};
			runCommand(cmd);
			if (Files.exists(tempThumb) && Files.size(tempThumb) > 0) {
				BufferedImage b = ImageIO.read(tempThumb.toFile());
				if (b != null) {
					try {
						ImageHelper.saveImage(txManager, mediaRepo, idMedia, b);
					} finally {
						b.flush();
					}
				}
			}
		} finally {
			Files.deleteIfExists(tempThumb);
		}
		logger.info("extractThumbnail(idMedia={}, src={}, thumbnailSeconds={}) - duration={}", idMedia, src, thumbnailSeconds, stopwatch);
	}

	public static void processVideo(TransactionManager txManager, MediaRepository mediaRepo, int idMedia, int thumbnailSeconds) throws Exception {
		StorageManager storage = StorageManager.getInstance();
		String webmKey = S3KeyGenerator.getWebWebm(idMedia);
		String mp4Key = S3KeyGenerator.getWebMp4(idMedia);
		String originalJpgKey = S3KeyGenerator.getOriginalJpg(idMedia);
		boolean needsWebm = !storage.exists(webmKey);
		boolean needsMp4 = !storage.exists(mp4Key);
		boolean needsThumb = !storage.exists(originalJpgKey);
		if (!needsWebm && !needsMp4 && !needsThumb) {
			logger.info("Video id={} is already fully processed. Skipping entirely.", idMedia);
			return;
		}
		String originalMp4Key = S3KeyGenerator.getOriginalMp4(idMedia);
		Path tempOriginal = Files.createTempFile("original-" + idMedia, "." + StorageType.MP4.getExtension());
		try {
			storage.downloadFile(originalMp4Key, tempOriginal);
			if (needsWebm) {
				Path tempWebm = Files.createTempFile("webm-" + idMedia, "." + StorageType.WEBM.getExtension());
				try {
					generateWebm(FFMPEG_DEFAULT, tempOriginal, tempWebm);
					storage.uploadFile(webmKey, tempWebm, StorageType.WEBM);
				} finally {
					Files.deleteIfExists(tempWebm);
				}
			}
			if (needsMp4) {
				Path tempMp4 = Files.createTempFile("mp4-" + idMedia, "." + StorageType.MP4.getExtension());
				try {
					generateMp4(FFMPEG_DEFAULT, tempOriginal, tempMp4);
					storage.uploadFile(mp4Key, tempMp4, StorageType.MP4);
				} finally {
					Files.deleteIfExists(tempMp4);
				}
			}
			if (needsThumb) {
				extractThumbnail(txManager, mediaRepo, idMedia, tempOriginal, thumbnailSeconds);
			}
		} finally {
			Files.deleteIfExists(tempOriginal);
		}
	}
	
	private static void generateMp4(String ffmpegPath, Path src, Path dst) throws IOException, InterruptedException {
		logger.info("Generating MP4: {} -> {}", src, dst);
		String[] cmd = {ffmpegPath, "-y", "-nostdin", "-i", src.toString(),
				"-vcodec", "libx264", "-preset", "veryfast", "-crf", "23",
				"-pix_fmt", "yuv420p", "-profile:v", "main", "-level", "3.1",
				"-vf", "scale=-2:1080", "-acodec", "aac", dst.toString()};
		runCommand(cmd);
	}

	private static void generateWebm(String ffmpegPath, Path src, Path dst) throws IOException, InterruptedException {
		logger.info("Generating WebM: {} -> {}", src, dst);
		String[] cmd = {ffmpegPath, "-y", "-nostdin", "-i", src.toString(),
				"-codec:v", "libvpx", "-b:v", "1500k", "-cpu-used", "5", "-deadline", "good",
				"-vf", "scale=-2:1080", "-codec:a", "libvorbis", "-b:a", "128k", dst.toString()};
		runCommand(cmd);
	}

	private static void runCommand(String[] cmd) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.inheritIO();
		Process p = pb.start();
		boolean finished = p.waitFor(30, TimeUnit.MINUTES);
		if (!finished) {
			p.destroyForcibly();
			throw new IOException("Command timed out after 30 minutes: " + String.join(" ", cmd));
		}
		int exitCode = p.exitValue();
		if (exitCode != 0) {
			throw new IOException("Command failed with exit code " + exitCode + ": " + String.join(" ", cmd));
		}
	}
}