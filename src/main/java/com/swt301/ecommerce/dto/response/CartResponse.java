// Vị trí: src/main/java/com/swt301/ecommerce/dto/response/CartResponse.java
package com.swt301.ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class CartResponse {
    private Integer cartId;
    private List<CartItemDto> items;
    private BigDecimal totalCartPrice; // Tổng tiền cả giỏ

    @Getter
    @Setter
    @Builder
    public static class CartItemDto {
        private Integer cartItemId;
        private Integer productId;
        private String productName;
        private String productImage;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal itemSubtotal; // Tiền của riêng món này (số lượng * giá)
    }
}