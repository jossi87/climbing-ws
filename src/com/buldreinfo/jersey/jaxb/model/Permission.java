package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

@Deprecated
public class Permission {
	private final String token;
	private final List<Integer> adminRegionIds;
	private final List<Integer> superAdminRegionIds;
	
	public Permission(String token, List<Integer> adminRegionIds, List<Integer> superAdminRegionIds) {
		this.token = token;
		this.adminRegionIds = adminRegionIds;
		this.superAdminRegionIds = superAdminRegionIds;
	}

	public List<Integer> getAdminRegionIds() {
		return adminRegionIds;
	}

	public List<Integer> getSuperAdminRegionIds() {
		return superAdminRegionIds;
	}
	
	public String getToken() {
		return token;
	}
	
	@Override
	public String toString() {
		return "Permission [token=" + token + ", adminRegionIds=" + adminRegionIds + ", superAdminRegionIds="
				+ superAdminRegionIds + "]";
	}
}