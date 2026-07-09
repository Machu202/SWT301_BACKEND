// Vị trí: src/main/java/com/swt301/ecommerce/dto/request/CartItemRequest.java
package com.swt301.ecommerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemRequest {
    
    @NotNull(message = "ID sản phẩm không được để trống")
    private Integer productId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
}