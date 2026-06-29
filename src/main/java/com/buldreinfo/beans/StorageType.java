package com.buldreinfo.beans;

import java.util.Arrays;
import java.util.Optional;

public enum StorageType {
	JPG("image/jpeg", "jpg"),
	MOV("video/quicktime", "mov"),
	MP4("video/mp4", "mp4"),
	MTS("video/mp2t", "mts"),
	PDF("application/pdf", "pdf"),
	PNG("image/png", "png"),
	WEBM("video/webm", "webm"),
	WEBP("image/webp", "webp"),
	XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

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

	public boolean isMovie() {
		return switch (this) {
		case MP4, MOV, MTS, WEBM -> true;
		case JPG, PNG, WEBP, PDF, XLSX -> false;
		};
	}
}