package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.dto.request.ProductRequest;
import com.swt301.ecommerce.dto.response.*;
import com.swt301.ecommerce.entity.Category;
import com.swt301.ecommerce.entity.OrderStatus;
import com.swt301.ecommerce.entity.PaymentMethod;
import com.swt301.ecommerce.repository.CategoryRepository;
import com.swt301.ecommerce.repository.OrderStatusRepository;
import com.swt301.ecommerce.repository.PaymentMethodRepository;
import com.swt301.ecommerce.service.OrderService;
import com.swt301.ecommerce.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminControllerTest {
    @Test void adminProductControllerDelegatesCrud() {
        ProductService service = mock(ProductService.class);
        ProductController controller = new ProductController(service);
        ProductRequest request = new ProductRequest();
        ProductResponse product = ProductResponse.builder().productId(1).build();
        PagedResponse<ProductResponse> page = PagedResponse.<ProductResponse>builder().content(List.of(product)).build();
        when(service.getProductPage(true, "q", 2, 0, 12)).thenReturn(page);
        when(service.createProduct(request)).thenReturn(product);
        when(service.updateProduct(1, request)).thenReturn(product);
        assertThat(controller.getProductPage(0, 12, "q", 2).getBody()).isSameAs(page);
        assertThat(controller.createProduct(request).getBody()).isSameAs(product);
        assertThat(controller.updateProduct(1, request).getBody()).isSameAs(product);
        assertThat(controller.deleteProduct(1).getBody()).containsEntry("message", "Sản phẩm đã được chuyển sang INACTIVE");
        verify(service).deactivateProduct(1);
    }

    @Test void adminOrderControllerDelegatesAllOperationsAndSetsSecureHeaders() {
        OrderService service = mock(OrderService.class);
        AdminOrderController controller = new AdminOrderController(service);
        OrderResponse order = OrderResponse.builder().orderId(1).build();
        PagedResponse<OrderResponse> page = PagedResponse.<OrderResponse>builder().content(List.of(order)).build();
        AdminOrderMetricsResponse metrics = AdminOrderMetricsResponse.builder().totalOrders(1).paidRevenue(BigDecimal.TEN).build();
        PaymentQrInfoResponse qr = PaymentQrInfoResponse.builder().orderId(1).build();
        when(service.getAdminOrderMetrics()).thenReturn(metrics);
        when(service.getAllSystemOrders()).thenReturn(List.of(order));
        when(service.getAllSystemOrdersPage(0, 10)).thenReturn(page);
        when(service.getAdminReceipt(1)).thenReturn(new byte[]{1});
        when(service.getAdminPaymentQrInfo(1)).thenReturn(qr);
        when(service.generateAdminPaymentQrCode(1)).thenReturn(new byte[]{2});
        when(service.updateOrderStatus(1, 2)).thenReturn(order);
        when(service.verifyPayment(1, "PAID")).thenReturn(order);
        assertThat(controller.getMetrics().getBody()).isSameAs(metrics);
        assertThat(controller.getAllOrders().getBody()).containsExactly(order);
        assertThat(controller.getAllOrdersPage(0, 10).getBody()).isSameAs(page);
        assertThat(controller.getReceipt(1).getHeaders().getCacheControl()).contains("no-store");
        assertThat(controller.getPaymentQrInfo(1).getBody()).isSameAs(qr);
        assertThat(controller.getPaymentQr(1).getHeaders().getContentType().toString()).isEqualTo("image/png");
        assertThat(controller.updateOrderStatus(1, 2).getBody()).isSameAs(order);
        assertThat(controller.verifyPayment(1, "PAID").getBody()).isSameAs(order);
    }

    @Test void referenceControllerReturnsIndependentCategoryPaymentAndStatusLists() {
        PaymentMethodRepository paymentRepo = mock(PaymentMethodRepository.class);
        CategoryRepository categoryRepo = mock(CategoryRepository.class);
        OrderStatusRepository statusRepo = mock(OrderStatusRepository.class);
        ReferenceDataController controller = new ReferenceDataController(paymentRepo, categoryRepo, statusRepo);
        when(categoryRepo.findAll(any(Sort.class))).thenReturn(List.of(
                Category.builder().categoryId(1).categoryName("Phones").build()));
        when(paymentRepo.findAll(any(Sort.class))).thenReturn(List.of(
                PaymentMethod.builder().paymentMethodId(4).methodName("QR").build(),
                PaymentMethod.builder().paymentMethodId(5).methodName("QR_CODE").build(),
                PaymentMethod.builder().paymentMethodId(6).methodName("COD").build(),
                PaymentMethod.builder().paymentMethodId(7).methodName("CRYPTO").build()));
        when(statusRepo.findAll(any(Sort.class))).thenReturn(List.of(
                OrderStatus.builder().statusId(9).statusName("PENDING").build()));
        assertThat(controller.getCategories().getBody()).extracting(CategoryResponse::getCategoryName).containsExactly("Phones");
        assertThat(controller.getPaymentMethods().getBody()).extracting(PaymentMethodResponse::getMethodName)
                .containsExactly("QR_CODE", "COD");
        assertThat(controller.getOrderStatuses().getBody()).extracting(OrderStatusResponse::getStatusName).containsExactly("PENDING");
    }
}
