package com.buldreinfo.jersey.jaxb.helpers;

import java.util.List;

import com.google.common.collect.Lists;

public class TimeAgo {
	private static final List<Long> times = Lists.newArrayList(365l, 30l, 7l, 1l);
	private static final List<String> timesString = Lists.newArrayList("year","month","week","day");

	public static String toDuration(long daysAgo) {
		StringBuffer res = new StringBuffer();
		for(int i=0;i< TimeAgo.times.size(); i++) {
			Long current = TimeAgo.times.get(i);
			long temp = daysAgo/current;
			if (temp>0) {
				res.append(temp).append(" ").append( TimeAgo.timesString.get(i) ).append(temp != 1 ? "s" : "").append(" ago");
				break;
			}
		}
		if("".equals(res.toString())) {
			return "today";
		}
		return res.toString();
	}

	public static void main(String args[]) {
		for (int daysBetween : Lists.newArrayList(0, 1, 6, 7, 13, 14, 30, 365)) {
			System.out.println(daysBetween + ": " + toDuration(daysBetween));
		}
	}
}