package main.java.ch.ethz.systems.asl.util;

import java.util.UUID;

public class CommonUtil {
	
	public static String genUUID() {
		UUID uid = UUID.randomUUID();
		return Long.toHexString(System.currentTimeMillis()) + "_" + uid.toString().subSequence(0, 8);
	}
	
	public static void main(String[] args) {
		for (int i = 0; i < 10; i++) {
			System.out.println(CommonUtil.genUUID());	
		}
		
	}
	
}
