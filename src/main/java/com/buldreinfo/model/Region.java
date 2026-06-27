package com.buldreinfo.model;

import java.util.List;

public record Region(int id, String group, String name, String url, boolean active, List<Coordinates> outline) {}