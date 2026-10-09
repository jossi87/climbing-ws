package com.buldreinfo.batch.maintenance;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buldreinfo.beans.StorageType;

/**
 * Whether the bucket's copy of a file is byte-for-byte the local one, for the files whose length cannot answer that
 * on its own.
 * <p>
 * Length plus content type identifies uploaded media — encoding a source never reproduces its exact size — but it
 * cannot judge an HLS playlist, which is nothing but a list of byte ranges into its segment. Regenerating a segment
 * moves every offset while the number of digits in those offsets stays where it was, so the new playlist is
 * <em>exactly</em> as long as the old one. Both directions of the maintenance sync then quietly keep the wrong file:
 * the upload skipped a playlist pointing at ranges the uploaded segment no longer contains (which left a repaired
 * movie unplayable in the browser), and the download likewise keeps a playlist whose segment has been replaced. A
 * playlist is a few hundred bytes, so its content is compared — against the MD5 that S3 publishes as the ETag of a
 * single-part upload, which is how every object in the bucket is written. Media files keep the cheap length check:
 * hashing each segment would mean reading the whole library out of the mirror on every run.
 */
final class SyncedContent {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private SyncedContent() {
	}

	/**
	 * True when the file in the bucket may be left alone: either its content cannot change without its length
	 * changing, or the bucket's ETag proves it holds the same bytes as {@code localFile}.
	 * <p>
	 * An unknown extension needs no check, and an ETag that is not a plain MD5 — from a multipart or encrypted
	 * upload — proves nothing at all, so it is reported as a difference and the file is copied rather than wrongly
	 * kept.
	 */
	static boolean matches(String relativePath, Path localFile, String remoteETag) {
		boolean isPlaylist = StorageType.fromFilename(relativePath).map(type -> type == StorageType.M3U8).orElse(false);
		if (!isPlaylist) {
			return true;
		}
		String remoteMd5 = remoteETag == null ? null : remoteETag.replace("\"", "");
		if (remoteMd5 == null || !remoteMd5.matches("[0-9a-fA-F]{32}")) {
			return false;
		}
		try {
			return remoteMd5.equalsIgnoreCase(md5(localFile));
		} catch (IOException | NoSuchAlgorithmException e) {
			logger.warn("Could not hash {} to compare it with the bucket ({}); copying it.", localFile, e.getMessage());
			return false;
		}
	}

	private static String md5(Path file) throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("MD5");
		try (DigestInputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
			in.transferTo(OutputStream.nullOutputStream());
		}
		return HexFormat.of().formatHex(digest.digest());
	}
}
