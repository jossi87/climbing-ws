package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record TodoProblem(int id, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade, List<User> partners) {}