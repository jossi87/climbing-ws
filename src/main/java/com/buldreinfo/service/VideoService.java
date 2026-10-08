package com.buldreinfo.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.beans.StorageType;
import com.buldreinfo.io.StorageManager;

@Service
public class VideoService {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * One rung of the adaptive HLS ladder. Only rungs the source is tall enough for are emitted (no upscaling),
	 * and the top rung is capped at the source resolution rather than a fixed 1080p. Capped-CRF keeps quality
	 * high on easy content while {@code maxrate} protects the bitrate on busy footage.
	 */
	private record HlsRung(int height, int crf, int maxrateKbps) {}

	private static final List<HlsRung> HLS_LADDER = List.of(
			new HlsRung(360, 24, 600),
			new HlsRung(720, 23, 2500),
			new HlsRung(1080, 22, 6000),
			new HlsRung(1440, 21, 12000),
			new HlsRung(2160, 21, 20000));

	private static final int HLS_SEGMENT_SECONDS = 6;
	private static final int HLS_AUDIO_KBPS = 128;
	/**
	 * Audio codecs we can carry into the fMP4/HLS output verbatim. When the source already uses one we copy the audio
	 * instead of re-encoding: that is lossless, faster, and keeps the audio exactly as uploaded (an AAC→AAC re-encode
	 * only loses quality). Anything else is transcoded to {@link #HLS_AUDIO_KBPS} kbit/s stereo AAC.
	 */
	private static final Set<String> HLS_COPY_AUDIO_CODECS = Set.of("aac");
	/** Grabbing a single thumbnail frame is quick; a few minutes is already generous. */
	private static final int THUMBNAIL_TIMEOUT_MINUTES = 5;
	/**
	 * The whole ladder is encoded in a single ffmpeg pass. That is heavy: long clips (30 min+) on an oversubscribed
	 * CPU run barely faster than real time, and tall sources add up-to-2160p rungs. Give ffmpeg hours, not minutes —
	 * a timed-out encode just throws away everything it had produced.
	 */
	private static final int HLS_ENCODE_TIMEOUT_MINUTES = 360;

	private final StorageManager storage;
	private final ImageService imageService;
	private final String ffmpegPath;
	private final String ffprobePath;

	public VideoService(StorageManager storage, ImageService imageService,
			@Value("${ffmpeg.path:ffmpeg}") String ffmpegPath,
			@Value("${ffprobe.path:ffprobe}") String ffprobePath) {
		this.storage = storage;
		this.imageService = imageService;
		this.ffmpegPath = ffmpegPath;
		this.ffprobePath = ffprobePath;
	}

	public void extractThumbnail(int idMedia, Path src, int thumbnailSeconds) throws IOException, InterruptedException {
		Path tempThumb = Files.createTempFile("thumb-" + idMedia, ".jpg");
		try {
			String seekFlag = thumbnailSeconds < 0 ? "-sseof" : "-ss";
			String[] cmd = {ffmpegPath, "-y", "-nostdin", seekFlag, String.valueOf(thumbnailSeconds), "-i", src.toString(), 
					"-t", "00:00:01", "-r", "1", "-f", "mjpeg", tempThumb.toString()};
			runCommand(null, cmd, THUMBNAIL_TIMEOUT_MINUTES);
			if (Files.exists(tempThumb) && Files.size(tempThumb) > 0) {
				BufferedImage b = ImageIO.read(tempThumb.toFile());
				if (b != null) {
					try {
						imageService.saveImage(idMedia, b);
					} finally {
						b.flush();
					}
				}
			}
		} finally {
			Files.deleteIfExists(tempThumb);
		}
	}

	public void processVideo(int idMedia, StorageType storageType, int thumbnailSeconds) throws IOException {
		String masterKey = S3KeyGenerator.getWebHlsMaster(idMedia);
		String originalJpgKey = S3KeyGenerator.getOriginalJpg(idMedia);
		boolean needsHls = !storage.exists(masterKey);
		boolean needsThumb = !storage.exists(originalJpgKey);
		if (!needsHls && !needsThumb) {
			logger.info("Video id={} is already fully processed. Skipping entirely.", idMedia);
			return;
		}
		String originalKey = S3KeyGenerator.getOriginalMp4(idMedia, storageType);
		Path tempOriginal = Files.createTempFile("original-" + idMedia, "." + storageType.getExtension());
		try {
			storage.downloadFile(originalKey, tempOriginal);
			if (needsHls) {
				generateHlsAndUpload(idMedia, tempOriginal);
			}
			if (needsThumb) {
				extractThumbnail(idMedia, tempOriginal, thumbnailSeconds);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Video processing interrupted for id=" + idMedia, e);
		} catch (Exception e) {
			throw new RuntimeException("Video processing failed for id=" + idMedia, e);
		} finally {
			try {
				Files.deleteIfExists(tempOriginal);
			} catch (IOException e) {
				logger.error("Failed to delete temp original {}", tempOriginal, e);
			}
		}
	}

	/** Runs the ladder into a fresh temp dir and uploads every produced file to S3 under the movie's HLS prefix. */
	private void generateHlsAndUpload(int idMedia, Path src) throws IOException, InterruptedException {
		Path outDir = Files.createTempDirectory("hls-" + idMedia);
		try {
			generateHls(src, outDir);
			String prefix = S3KeyGenerator.getWebHlsPrefix(idMedia);
			try (var files = Files.list(outDir)) {
				for (Path file : files.toList()) {
					StorageType type = StorageType.fromFilename(file.getFileName().toString())
							.orElseThrow(() -> new IllegalArgumentException("Unexpected HLS output file: " + file));
					storage.uploadFile(prefix + file.getFileName(), file, type);
				}
			}
			logger.info("Uploaded HLS renditions for id={} to {}", idMedia, prefix);
		} finally {
			deleteRecursively(outDir);
		}
	}

	/**
	 * Generates the adaptive HLS ladder for {@code src} into {@code outDir}, producing {@code master.m3u8}, one
	 * {@code v<height>p.m3u8} playlist per rung, and a matching {@code v<height>p.m4s} segment file. The
	 * {@code single_file} HLS flag packs the init segment and every media fragment into one byte-range addressed
	 * file, so a movie costs roughly two objects per rung plus the master. Rungs the source is too small for are
	 * dropped and the top rung is the source resolution, so nothing is ever upscaled.
	 */
	public void generateHls(Path src, Path outDir) throws IOException, InterruptedException {
		int sourceHeight = probeSourceHeight(src);
		String audioCodec = probeAudioCodec(src);
		boolean hasAudio = !audioCodec.isEmpty();
		boolean copyAudio = HLS_COPY_AUDIO_CODECS.contains(audioCodec);
		List<HlsRung> rungs = selectRungs(sourceHeight);
		Files.createDirectories(outDir);
		logger.info("Generating HLS: {} ({}p, audio={}) -> {} rung(s) {}", src, sourceHeight, audioCodec.isEmpty() ? "none" : audioCodec, rungs.size(), rungs);

		List<String> cmd = new ArrayList<>();
		cmd.add(ffmpegPath);
		cmd.add("-y");
		cmd.add("-nostdin");
		cmd.add("-i");
		cmd.add(src.toString());
		cmd.add("-filter_complex");
		cmd.add(buildFilterComplex(rungs));
		for (int i = 0; i < rungs.size(); i++) {
			cmd.add("-map");
			cmd.add("[o" + i + "]");
			if (hasAudio) {
				cmd.add("-map");
				cmd.add("0:a:0");
			}
		}
		// -sc_threshold 0 keeps keyframes on the segment grid, so every rung can switch at the same timestamp.
		cmd.add("-c:v");
		cmd.add("libx264");
		cmd.add("-preset");
		cmd.add("fast");
		cmd.add("-sc_threshold");
		cmd.add("0");
		cmd.add("-pix_fmt");
		cmd.add("yuv420p");
		for (int i = 0; i < rungs.size(); i++) {
			HlsRung rung = rungs.get(i);
			cmd.add("-crf:v:" + i);
			cmd.add(String.valueOf(rung.crf()));
			cmd.add("-maxrate:v:" + i);
			cmd.add(rung.maxrateKbps() + "k");
			cmd.add("-bufsize:v:" + i);
			cmd.add((rung.maxrateKbps() * 2) + "k");
		}
		if (hasAudio) {
			cmd.add("-c:a");
			if (copyAudio) {
				cmd.add("copy");
			} else {
				cmd.add("aac");
				cmd.add("-b:a");
				cmd.add(HLS_AUDIO_KBPS + "k");
				cmd.add("-ac");
				cmd.add("2");
			}
		}
		cmd.add("-f");
		cmd.add("hls");
		cmd.add("-hls_time");
		cmd.add(String.valueOf(HLS_SEGMENT_SECONDS));
		cmd.add("-hls_playlist_type");
		cmd.add("vod");
		cmd.add("-hls_segment_type");
		cmd.add("fmp4");
		cmd.add("-hls_flags");
		cmd.add("single_file+independent_segments");
		cmd.add("-hls_segment_filename");
		cmd.add("v%v.m4s");
		cmd.add("-master_pl_name");
		cmd.add("master.m3u8");
		cmd.add("-var_stream_map");
		cmd.add(buildVarStreamMap(rungs, hasAudio));
		cmd.add("v%v.m3u8");
		runCommand(outDir, cmd.toArray(String[]::new), HLS_ENCODE_TIMEOUT_MINUTES);
	}

	private static String buildFilterComplex(List<HlsRung> rungs) {
		StringBuilder split = new StringBuilder("[0:v]split=").append(rungs.size());
		StringBuilder scales = new StringBuilder();
		for (int i = 0; i < rungs.size(); i++) {
			split.append("[v").append(i).append(']');
			if (i > 0) {
				scales.append(';');
			}
			// -2 keeps the aspect ratio and rounds width to an even number (required by yuv420p).
			scales.append("[v").append(i).append("]scale=-2:").append(rungs.get(i).height()).append("[o").append(i).append(']');
		}
		return split.append(';').append(scales).toString();
	}

	private static String buildVarStreamMap(List<HlsRung> rungs, boolean hasAudio) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < rungs.size(); i++) {
			if (i > 0) {
				sb.append(' ');
			}
			sb.append("v:").append(i);
			if (hasAudio) {
				sb.append(",a:").append(i);
			}
			sb.append(",name:").append(rungs.get(i).height()).append('p');
		}
		return sb.toString();
	}

	/** Rungs the source is tall enough for; the top rung is capped at the source height so nothing is upscaled. */
	private static List<HlsRung> selectRungs(int sourceHeight) {
		List<HlsRung> selected = new ArrayList<>();
		int lastHeight = -1;
		for (HlsRung rung : HLS_LADDER) {
			int outputHeight = Math.min(rung.height(), sourceHeight);
			// Sources shorter than the next rung collapse onto the previous height; skip the duplicate.
			if (outputHeight == lastHeight) {
				continue;
			}
			selected.add(new HlsRung(outputHeight, rung.crf(), rung.maxrateKbps()));
			lastHeight = outputHeight;
		}
		return selected;
	}

	private int probeSourceHeight(Path src) throws IOException, InterruptedException {
		// "default=noprint_wrappers=1:nokey=1" prints the bare value. The csv writer we used before appends an extra
		// (empty) column whenever the stream carries side data — rotation, HDR mastering metadata, etc. — which turned
		// the output into "1080," and made Integer.parseInt throw for a large share of phone .mov files.
		String[] cmd = {ffprobePath, "-v", "error", "-select_streams", "v:0",
				"-show_entries", "stream=height", "-of", "default=noprint_wrappers=1:nokey=1", src.toString()};
		String out = valueLines(runCommandOutput(cmd)).trim();
		int newline = out.indexOf('\n');
		if (newline >= 0) {
			out = out.substring(0, newline).trim();
		}
		try {
			int height = Integer.parseInt(leadingDigits(out));
			if (height <= 0) {
				throw new NumberFormatException(out);
			}
			return height;
		} catch (NumberFormatException e) {
			throw new IOException("Unable to probe video height for " + src + " (ffprobe output: '" + out + "')", e);
		}
	}

	/** The leading run of digits in {@code value}, or "" when it does not start with a digit. */
	private static String leadingDigits(String value) {
		int end = 0;
		while (end < value.length() && Character.isDigit(value.charAt(end))) {
			end++;
		}
		return value.substring(0, end);
	}

	/**
	 * The value lines of a probe's merged output, with ffmpeg's own log lines removed. ffprobe writes the values to
	 * stdout and diagnostics to stderr, and {@link #runCommandOutput} merges the two; a file with slightly malformed
	 * AAC makes the parser log {@code [aac @ 0x…] Input buffer exhausted before END element found} ahead of the value.
	 * Every log line starts with '[', a value never does, so dropping those leaves just the value.
	 */
	private static String valueLines(String mergedOutput) {
		StringBuilder values = new StringBuilder();
		for (String line : mergedOutput.lines().toList()) {
			String value = line.strip();
			if (!value.isEmpty() && !value.startsWith("[")) {
				values.append(value).append('\n');
			}
		}
		return values.toString();
	}

	/** The first audio stream's codec name (e.g. {@code aac}), or "" when the source carries no audio. */
	private String probeAudioCodec(Path src) throws IOException, InterruptedException {
		String[] cmd = {ffprobePath, "-v", "error", "-select_streams", "a:0",
				"-show_entries", "stream=codec_name", "-of", "default=noprint_wrappers=1:nokey=1", src.toString()};
		return valueLines(runCommandOutput(cmd)).lines().findFirst().orElse("");
	}

	private void deleteRecursively(Path dir) {
		try (var walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					logger.warn("Failed to delete {}: {}", path, e.getMessage());
				}
			});
		} catch (IOException e) {
			logger.warn("Failed to clean up HLS output dir {}: {}", dir, e.getMessage());
		}
	}

	/** Runs a probe-style command and returns its combined output (stdout + stderr), failing on a non-zero exit. */
	private String runCommandOutput(String[] cmd) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectErrorStream(true);
		Process p = pb.start();
		boolean finished = p.waitFor(5, TimeUnit.MINUTES);
		if (!finished) {
			p.destroyForcibly();
			throw new IOException("Command timed out after 5 minutes: " + String.join(" ", cmd));
		}
		String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		if (p.exitValue() != 0) {
			throw new IOException("Command failed with exit code " + p.exitValue() + ": " + String.join(" ", cmd) + " -> " + out.trim());
		}
		return out;
	}

	private void runCommand(Path workingDir, String[] cmd, int timeoutMinutes) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		if (workingDir != null) {
			pb.directory(workingDir.toFile());
		}
		pb.inheritIO();
		Process p = pb.start();
		boolean finished = p.waitFor(timeoutMinutes, TimeUnit.MINUTES);
		if (!finished) {
			p.destroyForcibly();
			throw new IOException("Command timed out after " + timeoutMinutes + " minutes: " + String.join(" ", cmd));
		}
		int exitCode = p.exitValue();
		if (exitCode != 0) {
			throw new IOException("Command failed with exit code " + exitCode + ": " + String.join(" ", cmd));
		}
	}
}