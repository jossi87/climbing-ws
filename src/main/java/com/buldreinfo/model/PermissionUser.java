package com.buldreinfo.model;

import java.time.LocalDateTime;

public record PermissionUser(int userId, String name, MediaIdentity mediaIdentity, LocalDateTime lastLogin, boolean adminRead, boolean adminWrite, boolean superadminRead, boolean superadminWrite, boolean readOnly) {}