package com.buldreinfo.model;

public record AuthenticatedUser(boolean isAuthenticated, boolean isAdmin, boolean isSuperAdmin, int userId, String authenticatedName, String themePreference, MediaIdentity mediaIdentity) {
}