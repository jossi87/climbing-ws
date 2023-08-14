package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class Media {
	private final int id;
	private final boolean uploadedByMe;
	private final int crc32;
	private final int pitch;
	private final boolean trivia;
	private final int width;
	private final int height;
	private final int idType;
	private final String t;
	private final List<MediaSvgElement> mediaSvgs;
	private final int svgProblemId;
	private final List<Svg> svgs;
	private final MediaMetadata mediaMetadata;
	private final String embedUrl;
	private final boolean inherited;
	private final int enableMoveToIdSector;
	private final int enableMoveToIdProblem;
	
	public Media(int id, boolean uploadedByMe, int crc32, int pitch, boolean trivia, int width, int height, int idType, String t, List<MediaSvgElement> mediaSvgs, int svgProblemId, List<Svg> svgs, MediaMetadata mediaMetadata, String embedUrl) {
		this.id = id;
		this.uploadedByMe = uploadedByMe;
		this.crc32 = crc32;
		this.pitch = pitch;
		this.trivia = trivia;
		this.width = width;
		this.height = height;
		this.idType = idType;
		this.t = t;
		this.mediaSvgs = mediaSvgs;
		this.svgProblemId = svgProblemId;
		this.svgs = svgs;
		this.mediaMetadata = mediaMetadata;
		this.embedUrl = embedUrl;
		this.inherited = false;
		this.enableMoveToIdSector = 0;
		this.enableMoveToIdProblem = 0;
	}

	public Media(int id, boolean uploadedByMe, int crc32, int pitch, boolean trivia, int width, int height, int idType, String t, List<MediaSvgElement> mediaSvgs, int svgProblemId, List<Svg> svgs, MediaMetadata mediaMetadata, String embedUrl, boolean inherited, int enableMoveToIdSector, int enableMoveToIdProblem) {
		this.id = id;
		this.uploadedByMe = uploadedByMe;
		this.crc32 = crc32;
		this.pitch = pitch;
		this.trivia = trivia;
		this.width = width;
		this.height = height;
		this.idType = idType;
		this.t = t;
		this.mediaSvgs = mediaSvgs;
		this.svgProblemId = svgProblemId;
		this.svgs = svgs;
		this.mediaMetadata = mediaMetadata;
		this.embedUrl = embedUrl;
		this.inherited = inherited;
		this.enableMoveToIdSector = enableMoveToIdSector;
		this.enableMoveToIdProblem = enableMoveToIdProblem;
	}
	
	public int getCrc32() {
		return crc32;
	}
	
	public String getEmbedUrl() {
		return embedUrl;
	}
	
	public int getEnableMoveToIdProblem() {
		return enableMoveToIdProblem;
	}

	public int getEnableMoveToIdSector() {
		return enableMoveToIdSector;
	}
	
	public int getHeight() {
		return height;
	}

	public int getId() {
		return id;
	}
	
	public int getIdType() {
		return idType;
	}

	public MediaMetadata getMediaMetadata() {
		return mediaMetadata;
	}

	public List<MediaSvgElement> getMediaSvgs() {
		return mediaSvgs;
	}
	
	public int getPitch() {
		return pitch;
	}

	public int getSvgProblemId() {
		return svgProblemId;
	}
	
	public List<Svg> getSvgs() {
		return svgs;
	}
	
	public String getT() {
		return t;
	}
	
	public int getWidth() {
		return width;
	}
	
	public boolean isInherited() {
		return inherited;
	}
	
	public boolean isTrivia() {
		return trivia;
	}
	
	public boolean isUploadedByMe() {
		return uploadedByMe;
	}
}