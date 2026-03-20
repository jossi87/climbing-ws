package com.buldreinfo.jersey.jaxb.beans;

import java.util.Arrays;
import java.util.Optional;

public enum StorageType {
	JPG("image/jpeg", "jpg"),
	PNG("image/png", "png"),
	WEBP("image/webp", "webp"),
	MP4("video/mp4", "mp4"),
	MOV("video/quicktime", "mov"),
	MTS("video/mp2t", "mts"),
	WEBM("video/webm", "webm");

	public static Optional<StorageType> fromFilename(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		String ext = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1).toLowerCase();
		return fromExtension(ext);
	}

	private static Optional<StorageType> fromExtension(String ext) {
		if (ext == null || ext.isBlank()) {
			return Optional.empty();
		}
		final String normalizedExt = ext.equalsIgnoreCase("jpeg") ? "jpg" : ext;
		return Arrays.stream(values())
				.filter(t -> t.extension.equalsIgnoreCase(normalizedExt))
				.findFirst();
	}

	private final String mimeType;
	private final String extension;

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

	public boolean isMovie() {
		return switch (this) {
		case MP4, MOV, MTS, WEBM -> true;
		case JPG, PNG, WEBP -> false;
		};
	}
}