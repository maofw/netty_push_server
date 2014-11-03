package com.netty.push.utils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
	private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US);

	public static String getDateString(Date date) {
		if (date != null) {
			return sdf.format(date).toString();
		}
		return null;
	}

	public static void main(String[] args) throws ParseException {
		String s = getDateString(new Date());

		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		System.out.println(sdf2.format(sdf.parse(s)));

	}
}
