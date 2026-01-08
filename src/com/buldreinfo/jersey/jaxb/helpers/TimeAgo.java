package com.buldreinfo.jersey.jaxb.helpers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.google.common.collect.Lists;

public class TimeAgo {
	private static final List<Long> times = Lists.newArrayList(365l, 30l, 7l, 1l);
	private static final List<String> timesString = Lists.newArrayList("year","month","week","day");

	public static String getTimeAgo(LocalDate date) {
		if (date == null || date.getYear() < 1971) {
			return "";
		}
		final LocalDate today = LocalDate.now();
		return toDuration(ChronoUnit.DAYS.between(date, today));
	}

	private static String toDuration(long daysAgo) {
		StringBuffer res = new StringBuffer();
		for (int i = 0; i < TimeAgo.times.size(); i++) {
			Long current = TimeAgo.times.get(i);
			long temp = daysAgo/current;
			if (temp > 0) {
				res.append(temp).append(" ").append(TimeAgo.timesString.get(i)).append(temp != 1 ? "s" : "").append(" ago");
				break;
			}
		}
		return switch (res.toString()) {
		case "" -> "today";
		case "1 day ago" -> "yesterday";
		default -> {
			if (res.toString().startsWith("1 ")) {
				yield res.toString().replace("1 ", "a ");
			}
			yield res.toString();
		}
		};
	}
}