package com.buldreinfo.model;

import java.util.Set;

public record Search(String title, String subTitle, String breadcrumb, String url, String externalUrl, MediaIdentity mediaIdentity, long hits, String pageViews, boolean lockedAdmin, boolean lockedSuperadmin, Set<String> regions) {}
