package com.swt301.ecommerce.support;

import com.swt301.ecommerce.entity.*;
import com.swt301.ecommerce.enums.PaymentStatus;
import com.swt301.ecommerce.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

public final class TestFixtures {
    private TestFixtures() {}

    public static Role role(String name) {
        return Role.builder().roleId("ADMIN".equals(name) ? 2 : 1).roleName(name).build();
    }

    public static User user(int id, String role) {
        return User.builder()
                .userId(id)
                .role(role(role))
                .username("user" + id)
                .passwordHash("encoded")
                .fullName("User " + id)
                .email("user" + id + "@example.com")
                .phone("+84912345678")
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Category category(int id) {
        return Category.builder().categoryId(id).categoryName("Category " + id).build();
    }

    public static Product product(int id, int stock, BigDecimal price) {
        return Product.builder()
                .productId(id)
                .category(category(1))
                .productName("Product " + id)
                .description("Description")
                .price(price)
                .stock(stock)
                .image("https://example.com/product.png")
                .imagePublicId("products/product-" + id)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    public static Cart cart(int id, User user) {
        return Cart.builder().cartId(id).user(user).cartItems(new ArrayList<>()).build();
    }

    public static CartItem cartItem(int id, Cart cart, Product product, int quantity) {
        CartItem item = CartItem.builder()
                .cartItemId(id)
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .build();
        cart.getCartItems().add(item);
        return item;
    }

    public static Address address(int id, User user, boolean isDefault) {
        return Address.builder()
                .addressId(id)
                .user(user)
                .receiverName("Receiver")
                .receiverPhone("+84912345678")
                .province("Tien Giang")
                .district("My Tho")
                .ward("Ward 1")
                .street("2D Street")
                .isDefault(isDefault)
                .build();
    }

    public static PaymentMethod paymentMethod(int id, String name) {
        return PaymentMethod.builder().paymentMethodId(id).methodName(name).build();
    }

    public static OrderStatus orderStatus(int id, String name) {
        return OrderStatus.builder().statusId(id).statusName(name).build();
    }

    public static Voucher voucher(int id, String type, BigDecimal value) {
        return Voucher.builder()
                .voucherId(id)
                .voucherCode("SAVE" + id)
                .voucherName("Voucher " + id)
                .discountType(type)
                .discountValue(value)
                .minOrder(BigDecimal.ZERO)
                .expiredDate(LocalDateTime.now().plusDays(3))
                .quantity(10)
                .status("ACTIVE")
                .build();
    }

    public static Order order(int id, User user, String status, String paymentMethod) {
        return Order.builder()
                .orderId(id)
                .orderCode(String.format("ORD-%06d", id))
                .idempotencyKey("key-" + id)
                .user(user)
                .receiverNameSnapshot("Receiver")
                .receiverPhoneSnapshot("+84912345678")
                .provinceSnapshot("Tien Giang")
                .districtSnapshot("My Tho")
                .wardSnapshot("Ward 1")
                .streetSnapshot("2D Street")
                .paymentMethod(paymentMethod(1, paymentMethod))
                .status(orderStatus(1, status))
                .subtotal(new BigDecimal("100000"))
                .discount(BigDecimal.ZERO)
                .shippingFee(new BigDecimal("30000"))
                .total(new BigDecimal("130000"))
                .createdAt(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();
    }

    public static OrderItem orderItem(int id, Order order, Product product, int quantity) {
        OrderItem item = OrderItem.builder()
                .orderItemId(id)
                .order(order)
                .product(product)
                .productNameSnapshot(product.getProductName())
                .productImageSnapshot(product.getImage())
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .subtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .build();
        order.getOrderItems().add(item);
        return item;
    }

    public static Payment payment(int id, Order order, PaymentStatus status) {
        Payment payment = Payment.builder()
                .paymentId(id)
                .order(order)
                .paymentMethod(order.getPaymentMethod())
                .amount(order.getTotal())
                .paymentStatus(status)
                .build();
        order.setPayment(payment);
        return payment;
    }
}
