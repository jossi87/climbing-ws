package com.buldreinfo.jersey.jaxb.model;

public record VideoInitPayload(Media media, long fileSize, String contentType) {}