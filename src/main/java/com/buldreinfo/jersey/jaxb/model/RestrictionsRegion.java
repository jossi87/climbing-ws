package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record RestrictionsRegion(int id, String name, List<RestrictionsArea> areas) {
	public record RestrictionsArea(int id, boolean lockedAdmin, boolean lockedSuperadmin, String name, String accessClosed, String accessInfo, List<RestrictionsSector> sectors) {}
	public record RestrictionsSector(int id, boolean lockedAdmin, boolean lockedSuperadmin, String name, String accessClosed, String accessInfo) {}
}
