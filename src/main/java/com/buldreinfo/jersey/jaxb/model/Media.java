package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record Media(MediaIdentity identity, boolean uploadedByMe, int pitch, boolean trivia, int width, int height, boolean isMovie,
		String dateCreated, String dateTaken, User photographer, List<User> tagged, String description, String location,
		List<MediaSvgElement> mediaSvgs, int svgProblemId, List<Svg> svgs,
		String embedUrl, int thumbnailSeconds,
		boolean inherited,
		int enableMoveToIdArea, int enableMoveToIdSector, int enableMoveToIdProblem,
		String url, List<VideoChapter> chapters) {
	public Media withMediaSvgs(List<MediaSvgElement> mediaSvgs) {
        return new Media(identity, uploadedByMe, pitch, trivia, width, height, isMovie, dateCreated, dateTaken, photographer, tagged, description, location, mediaSvgs, svgProblemId, svgs, embedUrl, thumbnailSeconds, inherited, enableMoveToIdArea, enableMoveToIdSector, enableMoveToIdProblem, url, chapters);
    }
	public Media withSvgs(List<Svg> svgs, int enableMoveToIdArea) {
		return new Media(identity, uploadedByMe, pitch, trivia, width, height, isMovie, dateCreated, dateTaken, photographer, tagged, description, location, mediaSvgs, svgProblemId, svgs, embedUrl, thumbnailSeconds, inherited, enableMoveToIdArea, enableMoveToIdSector, enableMoveToIdProblem, url, chapters);
	}
	public Media withVideoChapters(List<VideoChapter> chapters) {
		return new Media(identity, uploadedByMe, pitch, trivia, width, height, isMovie, dateCreated, dateTaken, photographer, tagged, description, location, mediaSvgs, svgProblemId, svgs, embedUrl, thumbnailSeconds, inherited, enableMoveToIdArea, enableMoveToIdSector, enableMoveToIdProblem, url, chapters);
	}
}