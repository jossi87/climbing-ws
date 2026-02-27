package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record TopRank(int rank, double percentage, List<TopUser> users) {}