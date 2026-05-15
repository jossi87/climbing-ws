package com.buldreinfo.jersey.jaxb.model;

public record Search(String title, String subTitle, String breadcrumb, String url, String externalUrl, MediaIdentity mediaIdentity, long hits, String pageViews, boolean lockedAdmin, boolean lockedSuperadmin) {}