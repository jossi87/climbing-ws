package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

@Deprecated // TODO Remove when the postMedia is used in production
public record NewMedia(String name, String photographer, List<User> inPhoto, int pitch, boolean trivia, String description, String embedVideoUrl, String embedThumbnailUrl, long embedMilliseconds, int thumbnailSeconds) {}