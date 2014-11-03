package com.netty.push.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class SystemPrintUtil {
	private static final int MAXLINENUM = 100;

	/**
	 * 信息打印格式化方法
	 * 
	 * @param s
	 */
	public static void printServerInfo(String s) {
		StringBuffer sb = new StringBuffer();
		int len = 0;
		if (s != null && s.length() > 0) {
			len += s.getBytes().length;
		}
		len = len < MAXLINENUM ? MAXLINENUM : (len + 20);
		sb.append(formatHeadString(len));
		sb.append("\n");

		// 判断是否存在换行字符
		Pattern pat = Pattern.compile("\\n");
		String[] sarr = pat.split(s);

		if (sarr != null && sarr.length > 0) {
			for (int i = 0; i < sarr.length; i++) {
				sb.append(formatBodyString(len, sarr[i]));
				sb.append("\n");
			}
		}

		// pat = Pattern.compile("\\r");
		// sarr = pat.split(s);
		//		
		// if(sarr!=null && sarr.length>0){
		// for(int i = 0 ;i<sarr.length;i++){
		// sb.append(formatBodyString(len,sarr[i]));
		// sb.append("\n");
		// }
		// }

		sb.append(formatFootString(len));
		sb.append("\n");
		System.out.println(sb.toString());
		sb = null;
	}

	/**
	 * 格式化輸出頭部內容信息
	 * 
	 * @param len
	 * @return
	 */
	private static String formatHeadString(int len) {
		StringBuffer tmp1 = new StringBuffer();
		StringBuffer tmp2 = new StringBuffer();
		for (int i = 0; i < len; i++) {
			tmp1.append("*");
		}
		for (int i = 0; i < len - 2; i++) {
			tmp2.append(" ");
		}
		String result = tmp1.append("\n*").append(tmp2).append("*").toString();
		tmp1 = null;
		tmp2 = null;
		return result;
	}

	/**
	 * 格式化输出尾部信息内容
	 * 
	 * @param len
	 * @return
	 */
	private static String formatFootString(int len) {
		// 將頭部內容反轉即可
		String head = formatHeadString(len);
		if (head != null) {
			StringBuffer tmp = new StringBuffer(head);
			String s = tmp.reverse().toString();
			tmp = null;
			return s;
		}
		return null;
	}

	/**
	 * 格式化輸出字符串
	 * 
	 * @return
	 */
	private static String formatBodyString(int len, String s) {
		StringBuffer sHead = new StringBuffer();
		StringBuffer sEnd = new StringBuffer();
		int slen = 0;
		if (s != null && s.length() > 0) {
			slen = s.getBytes().length;
		}
		int kongbytelen = ((len - slen) - 2) / 2;
		sHead.append("*");
		for (int i = 0; i < kongbytelen; i++) {
			sHead.append(" ");
		}
		for (int i = 0; i < (len - slen - 2 - kongbytelen); i++) {
			sEnd.append(" ");
		}
		sEnd.append("*");
		String sresult = sHead.append(s).append(sEnd).toString();
		sHead = null;
		sEnd = null;
		return sresult;
	}

	public static void main(String[] args) throws ParseException {
		Calendar calender = Calendar.getInstance(Locale.US);
		System.out.println(calender.getTime().toString());
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US);
		long times = sdf.parse("Fri Oct 10 11:28:19 CST 2014").getTime();
		sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println(sdf.format(new Date(times)));
	}
}
