package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record Region(String group, String name, String url, boolean active, List<Coordinates> outline) {}