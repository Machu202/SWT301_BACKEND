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
    private BigDecimal totalCartPrice;

    @Getter
    @Setter
    @Builder
    public static class CartItemDto {
        private Integer cartItemId;
        private Integer productId;
        private String productName;
        private String productImage;
        private Integer quantity;
        private Integer productStock;
        private Integer availableToAdd;
        private BigDecimal unitPrice;
        private BigDecimal itemSubtotal;
    }
}
