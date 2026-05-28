package com.buldreinfo.jersey.jaxb.model;

import java.util.Collection;
import java.util.List;

public record Top(Collection<TopRank> rows, int numUsers) {
	public record TopRank(int rank, double percentage, List<TopUser> users) {}
	public record TopUser(int userId, String name, MediaIdentity mediaIdentity, boolean mine) {}
}
