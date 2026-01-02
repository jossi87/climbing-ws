package com.buldreinfo.jersey.jaxb.model;

import java.util.Collection;

public record Top(Collection<TopRank> rows, int numUsers) {}
