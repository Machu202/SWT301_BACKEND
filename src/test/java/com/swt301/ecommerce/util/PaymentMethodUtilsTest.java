package com.swt301.ecommerce.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMethodUtilsTest {
    @ParameterizedTest
    @CsvSource({
            "COD,COD", "cash,COD", "cash-on-delivery,COD", "COD_PAYMENT,COD",
            "QR,QR_CODE", "qr code,QR_CODE", "QR_PAYMENT,QR_CODE", "qrcode,QR_CODE"
    })
    void canonicalizesAliases(String input, String expected) {
        assertThat(PaymentMethodUtils.canonicalName(input)).isEqualTo(expected);
    }

    @Test void handlesNullAndUnsupportedValues() {
        assertThat(PaymentMethodUtils.canonicalName(null)).isEmpty();
        assertThat(PaymentMethodUtils.isSupported("BANK_TRANSFER")).isFalse();
        assertThat(PaymentMethodUtils.isSupported("COD")).isTrue();
        assertThat(PaymentMethodUtils.isSupported("QR_CODE")).isTrue();
    }
}
