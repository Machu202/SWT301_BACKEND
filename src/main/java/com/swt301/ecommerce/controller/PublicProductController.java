package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.dto.response.PagedResponse;
import com.swt301.ecommerce.dto.response.ProductResponse;
import com.swt301.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class PublicProductController {
    private final ProductService productService;

    /** Backward-compatible active product list capped at 100. Prefer /page for UI rendering. */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllActiveProducts());
    }

    @GetMapping("/page")
    public ResponseEntity<PagedResponse<ProductResponse>> getProductPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Integer categoryId) {
        return ResponseEntity.ok(productService.getProductPage(false, search, categoryId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getActiveProductById(id));
    }
}
