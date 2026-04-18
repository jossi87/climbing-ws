package com.buldreinfo.jersey.jaxb.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.Server;
import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.beans.StorageType;

public class VideoHelper {
	private static final Logger logger = LogManager.getLogger();

	public static void extractThumbnailToDb(String ffmpegPath, int idMedia, Path src) throws Exception {
        Path tempThumb = Files.createTempFile("thumb-" + idMedia, ".jpg");
        try {
            logger.info("Extracting thumbnail for id={}", idMedia);
            String[] cmd = {ffmpegPath, "-y", "-nostdin", "-sseof", "-10", "-i", src.toString(), 
                            "-t", "00:00:01", "-r", "1", "-f", "mjpeg", tempThumb.toString()};
            runCommand(cmd);
            if (Files.exists(tempThumb) && Files.size(tempThumb) > 0) {
                BufferedImage b = ImageIO.read(tempThumb.toFile());
                if (b != null) {
                    try {
                    	boolean hasTaggedUser = true;
                        Server.runSql((dao, c) -> ImageHelper.saveImage(dao, c, idMedia, b, hasTaggedUser));
                    } finally {
                        b.flush();
                    }
                }
            }
        } finally {
            Files.deleteIfExists(tempThumb);
        }
    }

	public static void generateMp4(String ffmpegPath, Path src, Path dst) throws IOException, InterruptedException {
        logger.info("Generating MP4: {} -> {}", src, dst);
        String[] cmd = {ffmpegPath, "-y", "-nostdin", "-i", src.toString(),
                "-vcodec", "libx264", "-preset", "veryfast", "-crf", "23",
                "-pix_fmt", "yuv420p", "-profile:v", "main", "-level", "3.1",
                "-vf", "scale=-2:1080", "-acodec", "aac", dst.toString()};
        runCommand(cmd);
    }
	
	public static void generateWebm(String ffmpegPath, Path src, Path dst) throws IOException, InterruptedException {
        logger.info("Generating WebM: {} -> {}", src, dst);
        String[] cmd = {ffmpegPath, "-y", "-nostdin", "-i", src.toString(),
                "-codec:v", "libvpx", "-b:v", "1500k", "-cpu-used", "5", "-deadline", "good",
                "-vf", "scale=-2:1080", "-codec:a", "libvorbis", "-b:a", "128k", dst.toString()};
        runCommand(cmd);
    }

    public static void processVideo(int idMedia) throws Exception {
		final String ffmpegPath = "ffmpeg";
		StorageManager storage = StorageManager.getInstance();
		String originalMp4Key = S3KeyGenerator.getOriginalMp4(idMedia);
		String originalJpgKey = S3KeyGenerator.getOriginalJpg(idMedia);
		String webmKey = S3KeyGenerator.getWebWebm(idMedia);
		String mp4Key = S3KeyGenerator.getWebMp4(idMedia);
		Path tempOriginal = Files.createTempFile("original-" + idMedia, "." + StorageType.MP4.getExtension());
		try {
			storage.downloadFile(originalMp4Key, tempOriginal);
			if (!storage.exists(webmKey)) {
				Path tempWebm = Files.createTempFile("webm-" + idMedia, "." + StorageType.WEBM.getExtension());
				try {
                    generateWebm(ffmpegPath, tempOriginal, tempWebm);
                    storage.uploadFile(webmKey, tempWebm, StorageType.WEBM);
                } finally {
                    Files.deleteIfExists(tempWebm);
                }
			}
			if (!storage.exists(mp4Key)) {
				Path tempMp4 = Files.createTempFile("mp4-" + idMedia, "." + StorageType.MP4.getExtension());
				try {
                    generateMp4(ffmpegPath, tempOriginal, tempMp4);
                    storage.uploadFile(mp4Key, tempMp4, StorageType.MP4);
                } finally {
                    Files.deleteIfExists(tempMp4);
                }
			}
			if (!storage.exists(originalJpgKey)) {
                extractThumbnailToDb(ffmpegPath, idMedia, tempOriginal);
            }
		} finally {
			Files.deleteIfExists(tempOriginal);
		}
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