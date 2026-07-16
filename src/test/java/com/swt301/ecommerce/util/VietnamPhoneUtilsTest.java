package com.swt301.ecommerce.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class VietnamPhoneUtilsTest {
    @Test void acceptsLocalVietnamPhone() { assertThat(VietnamPhoneUtils.isValid("0912345678")).isTrue(); }
    @Test void acceptsInternationalVietnamPhone() { assertThat(VietnamPhoneUtils.isValid("+84912345678")).isTrue(); }
    @Test void rejectsLettersAndWrongPrefixes() {
        assertThat(VietnamPhoneUtils.isValid("09abc45678")).isFalse();
        assertThat(VietnamPhoneUtils.isValid("0212345678")).isFalse();
    }
    @Test void normalizesLocalToInternational() { assertThat(VietnamPhoneUtils.normalize("0912345678")).isEqualTo("+84912345678"); }
    @Test void keepsInternationalNumber() { assertThat(VietnamPhoneUtils.normalize(" +84912345678 ")).isEqualTo("+84912345678"); }
    @Test void convertsToLocal() { assertThat(VietnamPhoneUtils.toLocal("+84912345678")).isEqualTo("0912345678"); }
    @Test void normalizeRejectsInvalidPhone() {
        assertThatThrownBy(() -> VietnamPhoneUtils.normalize("123"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
