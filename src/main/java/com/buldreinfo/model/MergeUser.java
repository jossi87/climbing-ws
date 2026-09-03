package com.buldreinfo.model;

import java.util.List;

public record MergeUser(int userId, String name, MediaIdentity mediaIdentity, List<String> emails, List<String> regions) {}
