package com.buldreinfo.model;

import java.util.List;

public record AdminUser(int userId, String name, String firstname, String lastname, boolean canEditName,
		MediaIdentity mediaIdentity, String lastLogin, List<String> emails, List<AdminRegion> regions) {
	public record AdminRegion(int id, String name, String url) {}
}
