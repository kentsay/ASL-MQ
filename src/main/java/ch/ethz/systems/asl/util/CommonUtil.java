package main.java.ch.ethz.systems.asl.util;

import java.util.UUID;

public class CommonUtil {

    public static String genUUID() {
        UUID uid = UUID.randomUUID();
        return uid.toString();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println(CommonUtil.genUUID());
        }

    }

}
