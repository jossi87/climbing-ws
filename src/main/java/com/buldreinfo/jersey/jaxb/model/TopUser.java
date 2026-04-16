package com.buldreinfo.jersey.jaxb.model;

public record TopUser(int userId, String name, MediaIdentity mediaIdentity, boolean mine) {}