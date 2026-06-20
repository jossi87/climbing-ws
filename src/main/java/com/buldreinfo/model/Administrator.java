package com.buldreinfo.model;

import java.util.List;

public record Administrator(int userId, String name, List<String> emails, MediaIdentity randomMedia, String lastLogin) {}