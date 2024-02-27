package com.buldreinfo.jersey.jaxb.model;

public record NewMedia(String name, String photographer, String inPhoto, int pitch, boolean trivia, String description, String embedVideoUrl, String embedThumbnailUrl, long embedMilliseconds) {}