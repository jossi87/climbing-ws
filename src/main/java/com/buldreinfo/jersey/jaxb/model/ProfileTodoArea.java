package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record ProfileTodoArea(int id, String name, boolean lockedAdmin, boolean lockedSuperadmin, List<ProfileTodoSector> sectors) {}