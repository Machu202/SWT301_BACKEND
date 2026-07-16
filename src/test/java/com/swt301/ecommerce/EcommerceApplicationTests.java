package com.swt301.ecommerce;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EcommerceApplicationTests {
    @Test void applicationEntryPointExists() {
        assertThat(EcommerceApplication.class).isNotNull();
    }
}
