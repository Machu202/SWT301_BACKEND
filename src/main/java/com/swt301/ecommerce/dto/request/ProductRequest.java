// Vị trí: src/main/java/com/swt301/ecommerce/dto/request/ProductRequest.java
package com.swt301.ecommerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotNull(message = "ID danh mục không được để trống")
    private Integer categoryId;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String productName;

    private String description;

    @NotNull(message = "Giá không được để trống")
    @Min(value = 0, message = "Giá sản phẩm phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho phải lớn hơn hoặc bằng 0")
    private Integer stock;

    private org.springframework.web.multipart.MultipartFile imageFile;
}