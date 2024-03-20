package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record TocRegion(int id, String name, List<TocArea> areas) {}