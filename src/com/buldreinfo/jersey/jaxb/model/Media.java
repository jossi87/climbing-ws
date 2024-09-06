package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record Media(int id, boolean uploadedByMe, int crc32, int pitch, boolean trivia, int width, int height, MediaRegion region, int idType, String t, List<MediaSvgElement> mediaSvgs, int svgProblemId, List<Svg> svgs, MediaMetadata mediaMetadata, String embedUrl, boolean inherited, int enableMoveToIdArea, int enableMoveToIdSector, int enableMoveToIdProblem, String url) {}