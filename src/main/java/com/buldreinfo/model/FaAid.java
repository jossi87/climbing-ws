package com.buldreinfo.model;

import java.util.List;

public record FaAid(int problemId, String date, String dateHr, String description, List<User> users) {}