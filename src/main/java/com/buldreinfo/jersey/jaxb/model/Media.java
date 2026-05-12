package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record Media(MediaIdentity identity, boolean uploadedByMe, int pitch, boolean trivia, int width, int height, int idType, String t, List<MediaSvgElement> mediaSvgs, int svgProblemId, List<Svg> svgs, MediaMetadata mediaMetadata, String embedUrl, boolean inherited, int enableMoveToIdArea, int enableMoveToIdSector, int enableMoveToIdProblem, String url) {
	public Media withMediaSvgs(List<MediaSvgElement> mediaSvgs) {
        return new Media(identity, uploadedByMe, pitch, trivia, width, height, idType, t, mediaSvgs, svgProblemId, svgs, mediaMetadata, embedUrl, inherited, enableMoveToIdArea, enableMoveToIdSector, enableMoveToIdProblem, url);
    }
	public Media withSvgs(List<Svg> svgs, int enableMoveToIdArea) {
		return new Media(identity, uploadedByMe, pitch, trivia, width, height, idType, t, mediaSvgs, svgProblemId, svgs, mediaMetadata, embedUrl, inherited, enableMoveToIdArea, enableMoveToIdSector, enableMoveToIdProblem, url);
	}
}