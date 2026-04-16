package com.buldreinfo.jersey.jaxb.model;

public record PermissionUser(int userId, String name, MediaIdentity mediaIdentity, String lastLogin, boolean adminRead, boolean adminWrite, boolean superadminRead, boolean superadminWrite, boolean readOnly) {}