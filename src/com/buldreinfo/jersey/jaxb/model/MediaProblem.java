package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class MediaProblem extends Media {
	private final int problemId;

	public MediaProblem(int id, int pitch, int width, int height, int idType, String t, List<MediaSvgElement> mediaSvgs,
			int svgProblemId, List<Svg> svgs, MediaMetadata mediaMetadata, String embedUrl, int problemId) {
		super(id, pitch, width, height, idType, t, mediaSvgs, svgProblemId, svgs, mediaMetadata, embedUrl);
		this.problemId = problemId;
	}

	public int getProblemId() {
		return problemId;
	}
}
