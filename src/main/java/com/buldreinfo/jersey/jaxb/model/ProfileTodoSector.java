package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record ProfileTodoSector(int id, String name, boolean lockedAdmin, boolean lockedSuperadmin, List<ProfileTodoProblem> problems) {}