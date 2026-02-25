package com.buldreinfo.jersey.jaxb.model;

public record TopUser(int userId, String name, int mediaId, long mediaVersionStamp, boolean mine) {}