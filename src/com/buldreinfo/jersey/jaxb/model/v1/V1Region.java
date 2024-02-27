package com.buldreinfo.jersey.jaxb.model.v1;

import java.util.List;

public record V1Region(int id, String name, List<V1Area> areas) {}