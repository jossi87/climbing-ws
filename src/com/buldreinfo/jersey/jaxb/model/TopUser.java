package com.buldreinfo.jersey.jaxb.model;

public record TopUser(int userId, String name, long avatarCrc32, boolean mine) {}