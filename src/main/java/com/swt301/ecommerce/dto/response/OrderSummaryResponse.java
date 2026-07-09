// Vị trí: src/main/java/com/swt301/ecommerce/dto/response/OrderSummaryResponse.java
package com.swt301.ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class OrderSummaryResponse {
    // Tái sử dụng lại CartItemDto từ CartResponse để hiển thị danh sách món hàng
    private List<CartResponse.CartItemDto> items; 
    
    private BigDecimal subtotal;    // Tổng tiền hàng
    private BigDecimal discount;    // Tiền giảm giá (từ Voucher)
    private BigDecimal shippingFee; // Phí ship
    private BigDecimal total;       // Tổng thanh toán cuối cùng
}