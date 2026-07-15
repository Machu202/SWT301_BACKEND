package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.ProductRequest;
import com.swt301.ecommerce.dto.response.PagedResponse;
import com.swt301.ecommerce.dto.response.ProductResponse;
import com.swt301.ecommerce.dto.response.UploadedAsset;
import com.swt301.ecommerce.entity.Category;
import com.swt301.ecommerce.entity.Product;
import com.swt301.ecommerce.enums.ProductStatus;
import com.swt301.ecommerce.exception.BusinessRuleException;
import com.swt301.ecommerce.exception.ResourceNotFoundException;
import com.swt301.ecommerce.repository.CategoryRepository;
import com.swt301.ecommerce.repository.ProductRepository;
import com.swt301.ecommerce.service.FileUploadService;
import com.swt301.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FileUploadService fileUploadService;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllActiveProducts() {
        return productRepository.searchPublic(
                        ProductStatus.ACTIVE,
                        "",
                        null,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "productId")))
                .getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getActiveProductById(Integer id) {
        Product product = productRepository.findByProductIdAndStatus(id, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm đang hoạt động"));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProductPage(
            boolean admin, String search, Integer categoryId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        PageRequest pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "productId"));
        String normalizedSearch = search == null ? "" : search.trim();
        Page<Product> productPage = admin
                ? productRepository.searchAdmin(normalizedSearch, categoryId, pageable)
                : productRepository.searchPublic(ProductStatus.ACTIVE, normalizedSearch, categoryId, pageable);
        return PagedResponse.from(productPage.map(this::mapToResponse));
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        UploadedAsset uploaded = null;
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            uploaded = fileUploadService.uploadProductImage(request.getImageFile());
        }

        Product product = Product.builder()
                .category(category)
                .productName(request.getProductName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .image(uploaded == null ? null : uploaded.getSecureUrl())
                .imagePublicId(uploaded == null ? null : uploaded.getPublicId())
                .status(ProductStatus.ACTIVE)
                .build();
        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Integer id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        String oldPublicId = product.getImagePublicId();
        String oldImageUrl = product.getImage();
        boolean removeImage = Boolean.TRUE.equals(request.getRemoveImage());
        boolean replacingImage = request.getImageFile() != null && !request.getImageFile().isEmpty();
        if (removeImage && replacingImage) {
            throw new BusinessRuleException("Không thể vừa xóa ảnh vừa tải ảnh mới trong cùng một yêu cầu");
        }

        UploadedAsset uploaded = replacingImage
                ? fileUploadService.uploadProductImage(request.getImageFile())
                : null;

        product.setCategory(category);
        product.setProductName(request.getProductName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        if (uploaded != null) {
            product.setImage(uploaded.getSecureUrl());
            product.setImagePublicId(uploaded.getPublicId());
        } else if (removeImage) {
            product.setImage(null);
            product.setImagePublicId(null);
        }
        Product saved = productRepository.save(product);

        if ((uploaded != null || removeImage) && (oldPublicId != null || oldImageUrl != null)) {
            fileUploadService.deleteProductImage(oldPublicId, oldImageUrl);
        }
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        if (product.getStatus() == ProductStatus.INACTIVE) return;
        String oldPublicId = product.getImagePublicId();
        String oldImageUrl = product.getImage();
        product.setStatus(ProductStatus.INACTIVE);
        product.setImage(null);
        product.setImagePublicId(null);
        productRepository.save(product);
        fileUploadService.deleteProductImage(oldPublicId, oldImageUrl);
    }

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
                .status(product.getStatus().name())
                .build();
    }
}
