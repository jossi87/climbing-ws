package com.buldreinfo.model;

import java.util.List;

public record UserSearchResult(int id, String name, MediaIdentity mediaIdentity, List<String> regions) {}

