package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record FrontpageActivity(List<FrontpageFirstAscent> firstAscents,
		List<FrontpageActivityAscent> recentAscents,
		List<FrontpageActivityMedia> newestMedia,
		List<FrontpageActivityComment> FrontpageActivityComment) {
}