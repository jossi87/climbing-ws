package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class MediaSvg implements IMetadata {
	private final int mediaId;
	private final List<MediaSvgElement> elements;
	private Metadata metadata;
	
	public MediaSvg(int mediaId, List<MediaSvgElement> elements) {
		this.mediaId = mediaId;
		this.elements = elements;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public int getMediaId() {
		return mediaId;
	}

	public List<MediaSvgElement> getElements() {
		return elements;
	}
}