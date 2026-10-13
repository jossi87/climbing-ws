package com.buldreinfo.model;

/**
 * One row of the reference table {@code climbing.type} (discipline → type → subtype). {@code isBolted} / {@code tradGear}
 * are the {@code type.is_bolted} / {@code type.trad_gear} columns: they, and the discipline flags, are the only way the
 * API and the web UI reason about a type. Nothing matches on {@code type} or {@code subType} text.
 */
public record Type(int id, String type, String subType, boolean isBolted, boolean tradGear) {}