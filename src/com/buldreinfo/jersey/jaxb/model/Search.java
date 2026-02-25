package com.buldreinfo.jersey.jaxb.model;

public record Search(String title, String description, String url, String externalUrl, int mediaId, long mediaVersionStamp, boolean lockedAdmin, boolean lockedSuperadmin, long hits, String pageViews) {}