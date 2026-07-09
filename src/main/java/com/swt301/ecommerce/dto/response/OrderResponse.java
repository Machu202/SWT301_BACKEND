// Vị trí: src/main/java/com/swt301/ecommerce/dto/response/OrderResponse.java
package com.swt301.ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class OrderResponse {
    private Integer orderId;
    private String orderCode;
    private String status;
    private String paymentMethod;
    private String paymentStatus; 
    
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shippingFee;
    private BigDecimal total;
    
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress; 
    
    private String note;
    private LocalDateTime createdAt;
}