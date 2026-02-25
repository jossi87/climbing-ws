package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record Profile(int id, String firstname, String lastname, boolean emailVisibleToAll, int mediaId, long mediaVersionStamp, List<String> emails, List<UserRegion> userRegions, String lastActivity) {}