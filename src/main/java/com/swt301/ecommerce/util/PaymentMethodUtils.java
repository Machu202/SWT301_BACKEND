package com.swt301.ecommerce.util;

import java.util.Locale;
import java.util.Set;

public final class PaymentMethodUtils {

    private static final Set<String> COD_ALIASES = Set.of(
            "COD", "CASH_ON_DELIVERY", "CASH_DELIVERY", "CASH"
    );
    private static final Set<String> QR_ALIASES = Set.of(
            "QR", "QR_CODE", "QRCODE", "QR_PAYMENT", "ONLINE_QR"
    );

    private PaymentMethodUtils() {
    }

    public static String canonicalName(String rawName) {
        if (rawName == null) {
            return "";
        }
        String normalized = rawName.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (COD_ALIASES.contains(normalized) || normalized.startsWith("COD_")) {
            return "COD";
        }
        if (QR_ALIASES.contains(normalized) || normalized.startsWith("QR_") || normalized.contains("QRCODE")) {
            return "QR_CODE";
        }
        return normalized;
    }

    public static boolean isSupported(String rawName) {
        String canonical = canonicalName(rawName);
        return "COD".equals(canonical) || "QR_CODE".equals(canonical);
    }
}
