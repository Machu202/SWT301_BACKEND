package com.swt301.ecommerce;

import com.swt301.ecommerce.dto.request.*;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {
    private static Validator validator;

    @BeforeAll static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test void registerAcceptsOnlyCustomerAndVietnamPhone() {
        RegisterRequest valid = register();
        assertThat(validator.validate(valid)).isEmpty();
        valid.setRole("ADMIN");
        assertThat(validator.validate(valid)).extracting(v -> v.getPropertyPath().toString()).contains("role");
        valid.setRole("CUSTOMER"); valid.setPhone("09ABC45678");
        assertThat(validator.validate(valid)).extracting(v -> v.getPropertyPath().toString()).contains("phone");
    }

    @Test void addressAndProfileValidatePhoneAndRequiredFields() {
        AddressRequest address = new AddressRequest();
        address.setReceiverName("Receiver"); address.setReceiverPhone("0912345678");
        address.setProvince("P"); address.setDistrict("D"); address.setWard("W"); address.setStreet("S");
        assertThat(validator.validate(address)).isEmpty();
        address.setReceiverPhone("123");
        assertThat(validator.validate(address)).extracting(v -> v.getPropertyPath().toString()).contains("receiverPhone");

        ProfileUpdateRequest profile = new ProfileUpdateRequest(); profile.setFullName("Name"); profile.setPhone("+84912345678");
        assertThat(validator.validate(profile)).isEmpty();
    }

    @Test void cartProductAndCheckoutBoundaryRulesWork() {
        CartItemRequest cart = new CartItemRequest(); cart.setProductId(1); cart.setQuantity(0);
        assertThat(validator.validate(cart)).extracting(v -> v.getPropertyPath().toString()).contains("quantity");

        ProductRequest product = new ProductRequest();
        product.setCategoryId(1); product.setProductName("Product"); product.setPrice(new BigDecimal("-1")); product.setStock(-1);
        assertThat(validator.validate(product)).extracting(v -> v.getPropertyPath().toString()).contains("price", "stock");

        CheckoutRequest checkout = new CheckoutRequest(); checkout.setPaymentMethodId(null); checkout.setNote("x".repeat(1001));
        assertThat(validator.validate(checkout)).extracting(v -> v.getPropertyPath().toString()).contains("paymentMethodId", "note");
    }

    @Test void manualAddressValidatesPhoneFormat() {
        ManualAddressRequest manual = new ManualAddressRequest();
        manual.setReceiverName("Receiver"); manual.setReceiverPhone("123");
        manual.setProvince("P"); manual.setDistrict("D"); manual.setWard("W"); manual.setStreet("S");
        assertThat(validator.validate(manual)).extracting(v -> v.getPropertyPath().toString()).contains("receiverPhone");
    }

    private RegisterRequest register() {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("customer"); r.setPassword("secret1"); r.setFullName("Customer");
        r.setEmail("customer@example.com"); r.setPhone("0912345678"); r.setRole("CUSTOMER");
        return r;
    }
}
