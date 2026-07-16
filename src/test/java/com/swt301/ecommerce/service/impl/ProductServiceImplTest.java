package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.ProductRequest;
import com.swt301.ecommerce.dto.response.UploadedAsset;
import com.swt301.ecommerce.entity.Category;
import com.swt301.ecommerce.entity.Product;
import com.swt301.ecommerce.enums.ProductStatus;
import com.swt301.ecommerce.exception.BusinessRuleException;
import com.swt301.ecommerce.exception.ResourceNotFoundException;
import com.swt301.ecommerce.repository.CategoryRepository;
import com.swt301.ecommerce.repository.ProductRepository;
import com.swt301.ecommerce.service.FileUploadService;
import com.swt301.ecommerce.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock FileUploadService fileUploadService;
    @InjectMocks ProductServiceImpl service;

    @Test void getsOnlyActiveProducts() {
        Product product = TestFixtures.product(1, 5, new BigDecimal("100000"));
        when(productRepository.searchPublic(eq(ProductStatus.ACTIVE), eq(""), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        assertThat(service.getAllActiveProducts()).extracting("productId").containsExactly(1);
    }

    @Test void getActiveProductRejectsMissingOrInactive() {
        when(productRepository.findByProductIdAndStatus(9, ProductStatus.ACTIVE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getActiveProductById(9)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void usesAdminAndPublicPaginationQueries() {
        when(productRepository.searchAdmin(anyString(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(productRepository.searchPublic(eq(ProductStatus.ACTIVE), anyString(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.getProductPage(true, " phone ", 2, -1, 500);
        service.getProductPage(false, " phone ", 2, -1, 500);
        verify(productRepository).searchAdmin(eq("phone"), eq(2), argThat(p -> p.getPageNumber() == 0 && p.getPageSize() == 100));
        verify(productRepository).searchPublic(eq(ProductStatus.ACTIVE), eq("phone"), eq(2), argThat(p -> p.getPageSize() == 100));
    }

    @Test void createsProductWithoutImage() {
        Category category = TestFixtures.category(1);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> { Product p = inv.getArgument(0); p.setProductId(10); return p; });
        var result = service.createProduct(request(null, false));
        assertThat(result.getProductId()).isEqualTo(10);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verifyNoInteractions(fileUploadService);
    }

    @Test void createsProductWithUploadedImage() {
        Category category = TestFixtures.category(1);
        MockMultipartFile file = new MockMultipartFile("imageFile", "p.png", "image/png", new byte[]{1});
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(fileUploadService.uploadProductImage(file)).thenReturn(UploadedAsset.builder().publicId("pid").secureUrl("url").build());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        var result = service.createProduct(request(file, false));
        assertThat(result.getImage()).isEqualTo("url");
    }

    @Test void updateCannotRemoveAndReplaceImageTogether() {
        Product product = TestFixtures.product(1, 5, BigDecimal.TEN);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(TestFixtures.category(1)));
        ProductRequest request = request(new MockMultipartFile("imageFile", "p.png", "image/png", new byte[]{1}), true);
        assertThatThrownBy(() -> service.updateProduct(1, request)).isInstanceOf(BusinessRuleException.class);
    }

    @Test void replacingImageDeletesOldAsset() {
        Product product = TestFixtures.product(1, 5, BigDecimal.TEN);
        MockMultipartFile file = new MockMultipartFile("imageFile", "p.png", "image/png", new byte[]{1});
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(TestFixtures.category(1)));
        when(fileUploadService.uploadProductImage(file)).thenReturn(UploadedAsset.builder().publicId("new").secureUrl("new-url").build());
        when(productRepository.save(product)).thenReturn(product);
        service.updateProduct(1, request(file, false));
        verify(fileUploadService).deleteProductImage("products/product-1", "https://example.com/product.png");
        assertThat(product.getImage()).isEqualTo("new-url");
    }

    @Test void removeImageClearsFieldsAndDeletesAsset() {
        Product product = TestFixtures.product(1, 5, BigDecimal.TEN);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(TestFixtures.category(1)));
        when(productRepository.save(product)).thenReturn(product);
        service.updateProduct(1, request(null, true));
        assertThat(product.getImage()).isNull();
        assertThat(product.getImagePublicId()).isNull();
        verify(fileUploadService).deleteProductImage(anyString(), anyString());
    }

    @Test void deactivateIsSoftDeleteAndCleansImage() {
        Product product = TestFixtures.product(1, 5, BigDecimal.TEN);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        service.deactivateProduct(1);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        verify(productRepository).save(product);
        verify(fileUploadService).deleteProductImage("products/product-1", "https://example.com/product.png");
    }

    @Test void deactivatingAlreadyInactiveDoesNothing() {
        Product product = TestFixtures.product(1, 5, BigDecimal.TEN);
        product.setStatus(ProductStatus.INACTIVE);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        service.deactivateProduct(1);
        verify(productRepository, never()).save(any());
        verifyNoInteractions(fileUploadService);
    }

    private ProductRequest request(MockMultipartFile file, boolean removeImage) {
        ProductRequest request = new ProductRequest();
        request.setCategoryId(1);
        request.setProductName(" Product ");
        request.setDescription("Description");
        request.setPrice(new BigDecimal("100000"));
        request.setStock(5);
        request.setImageFile(file);
        request.setRemoveImage(removeImage);
        return request;
    }
}
