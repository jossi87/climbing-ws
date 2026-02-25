package com.buldreinfo.jersey.jaxb.model;

public record PermissionUser(int userId, String name, int mediaId, long mediaVersionStamp, String lastLogin, boolean adminRead, boolean adminWrite, boolean superadminRead, boolean superadminWrite, boolean readOnly) {}