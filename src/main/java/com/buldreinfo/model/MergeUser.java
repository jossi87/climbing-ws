package com.buldreinfo.model;

import java.util.List;

public record MergeUser(int userId, String name, MediaIdentity mediaIdentity, String lastLogin, List<String> emails, List<MergeRegion> regions) {
	public record MergeRegion(int id, String name, String url) {}
}
