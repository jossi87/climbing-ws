package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class MediaProblem extends Media {
	private final int problemId;

	public MediaProblem(int id, int crc32, int pitch, boolean trivia, int width, int height, int idType, String t, List<MediaSvgElement> mediaSvgs,
			int svgProblemId, List<Svg> svgs, MediaMetadata mediaMetadata, String embedUrl, int problemId) {
		super(id, crc32, pitch, trivia, width, height, idType, t, mediaSvgs, svgProblemId, svgs, mediaMetadata, embedUrl);
		this.problemId = problemId;
	}

	public int getProblemId() {
		return problemId;
	}
}
