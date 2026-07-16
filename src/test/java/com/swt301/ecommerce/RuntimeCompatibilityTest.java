package com.swt301.ecommerce;

import com.swt301.ecommerce.entity.Product;
import com.swt301.ecommerce.service.impl.CartServiceImpl;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeCompatibilityTest {

    @Test
    void productImagePublicIdDoesNotRequireANewDatabaseColumn() throws Exception {
        Field field = Product.class.getDeclaredField("imagePublicId");
        assertThat(field.isAnnotationPresent(Transient.class)).isTrue();
        assertThat(field.isAnnotationPresent(Column.class)).isFalse();
    }

    @Test
    void readingAnEmptyCartMayCreateTheCustomersCart() throws Exception {
        Method method = CartServiceImpl.class.getMethod("getCart", Integer.class);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isFalse();
    }
}
