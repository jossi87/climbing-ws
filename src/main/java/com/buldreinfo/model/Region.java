package com.buldreinfo.model;

import java.util.List;

public record Region(int id, String discipline, boolean isBouldering, boolean isRopeClimbing, boolean isIceClimbing,
		String name, String url, boolean active, List<Coordinates> outline) {}