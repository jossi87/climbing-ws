package com.buldreinfo.model;

import java.util.List;

public record Ticks(List<PublicAscent> ticks, int currPage, int numPages) {}