package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record Profile(int id, String picture, String firstname, String lastname, boolean emailVisibleToAll, List<String> emails, List<UserRegion> userRegions) {}