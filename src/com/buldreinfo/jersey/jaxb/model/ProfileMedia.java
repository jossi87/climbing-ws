package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class ProfileMedia extends Media {
	private final String url;

	public ProfileMedia(int id, boolean uploadedByMe, int crc32, int pitch, boolean trivia, int width, int height, int idType, String t, List<MediaSvgElement> mediaSvgs,
			int svgProblemId, List<Svg> svgs, MediaMetadata mediaMetadata, String embedUrl, String url) {
		super(id, uploadedByMe, crc32, pitch, trivia, width, height, idType, t, mediaSvgs, svgProblemId, svgs, mediaMetadata, embedUrl);
		this.url = url;
	}

	public String geUrl() {
		return url;
	}
}
