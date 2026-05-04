package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record Profile(int id, String firstname, String lastname, boolean emailVisibleToAll, MediaIdentity mediaIdentity, List<String> emails, List<UserRegion> userRegions, String lastActivity, String themePreference) {}
