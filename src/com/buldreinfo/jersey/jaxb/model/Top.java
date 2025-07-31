package com.buldreinfo.jersey.jaxb.model;

public record Top(int rank, int userId, String name, long avatarCrc32, double percentage, boolean mine) {}