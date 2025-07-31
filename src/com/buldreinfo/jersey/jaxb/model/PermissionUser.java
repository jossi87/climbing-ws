package com.buldreinfo.jersey.jaxb.model;

public record PermissionUser(int userId, String name, long avatarCrc32, String lastLogin, boolean adminRead, boolean adminWrite, boolean superadminRead, boolean superadminWrite, boolean readOnly) {}