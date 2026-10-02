package com.buldreinfo.model;

import java.util.Set;

public record UserSearchResult(int id, String name, MediaIdentity mediaIdentity, Set<String> regions) {}

