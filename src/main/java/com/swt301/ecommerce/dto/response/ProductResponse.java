// Vị trí: src/main/java/com/swt301/ecommerce/dto/response/ProductResponse.java
package com.swt301.ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class ProductResponse {
    private Integer productId;
    private Integer categoryId;
    private String categoryName;
    private String productName;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String image;
}