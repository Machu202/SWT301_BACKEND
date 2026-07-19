package com.swt301.ecommerce.util;

import java.util.regex.Pattern;

public final class VietnamPhoneUtils {
    public static final String REGEX = "^(?:0[35789][0-9]{8}|\\+84[35789][0-9]{8})$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private VietnamPhoneUtils() {
    }

    public static boolean isValid(String phone) {
        return phone != null && PATTERN.matcher(phone.trim()).matches();
    }

    public static String normalize(String phone) {
        if (!isValid(phone)) {
            throw new IllegalArgumentException("Số điện thoại phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx");
        }
        String trimmed = phone.trim();
        return trimmed.startsWith("0") ? "+84" + trimmed.substring(1) : trimmed;
    }

    public static String toLocal(String phone) {
        String normalized = normalize(phone);
        return "0" + normalized.substring(3);
    }
}
