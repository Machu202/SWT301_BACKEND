// Vị trí: src/main/java/com/swt301/ecommerce/service/impl/ProductServiceImpl.java
package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.ProductRequest;
import com.swt301.ecommerce.dto.response.ProductResponse;
import com.swt301.ecommerce.entity.Category;
import com.swt301.ecommerce.entity.Product;
import com.swt301.ecommerce.repository.CategoryRepository;
import com.swt301.ecommerce.repository.ProductRepository;
import com.swt301.ecommerce.service.FileUploadService;
import com.swt301.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FileUploadService fileUploadService;

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductResponse getProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        return mapToResponse(product);
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        // Gọi hàm up ảnh lên Cloudinary
        String imageUrl = null;
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            imageUrl = fileUploadService.uploadProductImage(request.getImageFile());
        }

        Product product = Product.builder()
                .category(category)
                .productName(request.getProductName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .image(imageUrl) // LƯU LINK ẢNH CLOUDINARY
                .build();

        return mapToResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse updateProduct(Integer id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        // Nếu admin có chọn file ảnh mới thì up lên, không thì giữ nguyên ảnh cũ
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            String imageUrl = fileUploadService.uploadProductImage(request.getImageFile());
            product.setImage(imageUrl);
        }

        product.setCategory(category);
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        return mapToResponse(productRepository.save(product));
    }
    @Override
    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        productRepository.delete(product);
    }

    // Hàm hỗ trợ chuyển đổi Entity -> Response
    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .productId(product.getProductId())
                .categoryId(product.getCategory().getCategoryId())
                .categoryName(product.getCategory().getCategoryName())
                .productName(product.getProductName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .image(product.getImage())
                .build();
    }
}