package com.buldreinfo.beans;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum StorageType {
	JPG("image/jpeg", "jpg"),
	/** HLS master/media playlist produced by the adaptive streaming pipeline. */
	M3U8("application/vnd.apple.mpegurl", "m3u8"),
	/** fMP4 (CMAF) media segment referenced by the HLS playlists. */
	M4S("video/iso.segment", "m4s"),
	MOV("video/quicktime", "mov"),
	MP4("video/mp4", "mp4"),
	MTS("video/mp2t", "mts"),
	PDF("application/pdf", "pdf"),
	PNG("image/png", "png"),
	WEBM("video/webm", "webm"),
	WEBP("image/webp", "webp"),
	XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

	/**
	 * Containers a user-uploaded movie source is accepted in, in the order an existing original is looked for.
	 * Which one a given movie actually used is not recorded anywhere, so code that needs the original file itself
	 * — as opposed to its derived HLS ladder and images — has to probe them in turn. {@link #WEBM} is deliberately
	 * absent: {@link S3KeyGenerator#getOriginalMp4} cannot even build a key for it, and the upload endpoint
	 * promises no more than these three ({@link #isMovie()} still lists webm because a browser may offer it).
	 */
	public static final List<StorageType> MOVIE_SOURCE_TYPES = List.of(MP4, MOV, MTS);

	public static Optional<StorageType> fromExtension(String ext) {
		if (ext == null || ext.isBlank()) {
			return Optional.empty();
		}
		final String normalizedExt = (ext.equalsIgnoreCase("jpeg")|| ext.equalsIgnoreCase("jfif")) ? "jpg" : ext;
		return Arrays.stream(values())
				.filter(t -> t.extension.equalsIgnoreCase(normalizedExt))
				.findFirst();
	}

	public static Optional<StorageType> fromFilename(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		String ext = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1).toLowerCase();
		return fromExtension(ext);
	}

	public static Optional<StorageType> fromMimeType(String mimeType) {
		if (mimeType == null || mimeType.isBlank()) {
			return Optional.empty();
		}
		return Arrays.stream(values())
				.filter(t -> t.mimeType.equalsIgnoreCase(mimeType))
				.findFirst();
	}

	private final String extension;
	private final String mimeType;

	private StorageType(String mimeType, String extension) {
		this.mimeType = mimeType;
		this.extension = extension;
	}

	public String getExtension() {
		return extension;
	}

	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Whether this type is a user-uploaded movie source. The HLS streaming artifacts ({@link #M3U8},
	 * {@link #M4S}) are server-generated derivatives, not movie sources, so they are excluded here.
	 */
	public boolean isMovie() {
		return switch (this) {
		case MP4, MOV, MTS, WEBM -> true;
		case JPG, PNG, WEBP, PDF, XLSX, M3U8, M4S -> false;
		};
	}
}