package com.buldreinfo.jersey.jaxb.model;

public record UserRegion(int id, String name, String role, boolean enabled, boolean readOnly) {}