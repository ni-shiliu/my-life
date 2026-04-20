package com.mylife.util;

public class PhoneUtils {

    private PhoneUtils() {
    }

    public static String desensitize(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
