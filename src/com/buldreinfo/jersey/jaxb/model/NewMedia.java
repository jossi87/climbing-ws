package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record NewMedia(String name, String photographer, List<User> inPhoto, int pitch, boolean trivia, String description, String embedVideoUrl, String embedThumbnailUrl, long embedMilliseconds) {}