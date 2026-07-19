package com.swt301.ecommerce.service;

import com.swt301.ecommerce.dto.request.ProductRequest;
import com.swt301.ecommerce.dto.response.PagedResponse;
import com.swt301.ecommerce.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {
    List<ProductResponse> getAllActiveProducts();
    ProductResponse getActiveProductById(Integer id);
    PagedResponse<ProductResponse> getProductPage(boolean admin, String search, Integer categoryId, int page, int size);
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(Integer id, ProductRequest request);
    void deactivateProduct(Integer id);
}
