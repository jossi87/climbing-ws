package com.buldreinfo.jersey.jaxb.model;

public record Search(String title, String description, String url, String externalUrl, MediaIdentity mediaIdentity, boolean lockedAdmin, boolean lockedSuperadmin, long hits, String pageViews) {}