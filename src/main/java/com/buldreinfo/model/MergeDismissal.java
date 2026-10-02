package com.buldreinfo.model;

/**
 * A pair of users that a superadmin has explicitly marked as "not merge candidates", so the pair is
 * excluded from the merge suggestions on the users page. Ids are stored canonically (userId1 &lt; userId2).
 */
public record MergeDismissal(int userId1, int userId2) {}
