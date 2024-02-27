package com.buldreinfo.jersey.jaxb.model;

public record Search(String title, String description, String url, String externalurl, String mediaurl, int mediaid, int crc32, boolean lockedadmin, boolean lockedsuperadmin) {}