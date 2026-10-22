package com.buldreinfo.model;

import java.time.LocalDateTime;
import java.util.List;

public record Administrator(int userId, String name, List<String> emails, MediaIdentity randomMedia, LocalDateTime lastLogin) {}