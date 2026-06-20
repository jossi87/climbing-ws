package com.buldreinfo.model;

public record VideoInitPayload(Media media, long fileSize, String contentType) {}