// Vị trí: src/main/java/com/swt301/ecommerce/service/ProductService.java
package com.swt301.ecommerce.service;

import com.swt301.ecommerce.dto.request.ProductRequest;
import com.swt301.ecommerce.dto.response.ProductResponse;
import java.util.List;

public interface ProductService {
    List<ProductResponse> getAllProducts();
    ProductResponse getProductById(Integer id);
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(Integer id, ProductRequest request);
    void deleteProduct(Integer id);
}