package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record Top(int rank, double percentage, List<TopUser> users) {}