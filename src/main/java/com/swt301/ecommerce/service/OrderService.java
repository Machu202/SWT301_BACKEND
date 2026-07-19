package com.swt301.ecommerce.service;

import com.swt301.ecommerce.dto.request.CheckoutRequest;
import com.swt301.ecommerce.dto.response.AdminOrderMetricsResponse;
import com.swt301.ecommerce.dto.response.OrderResponse;
import com.swt301.ecommerce.dto.response.OrderSummaryResponse;
import com.swt301.ecommerce.dto.response.PagedResponse;
import com.swt301.ecommerce.dto.response.PaymentQrInfoResponse;
import com.swt301.ecommerce.dto.response.UploadedAsset;

import java.util.List;

public interface OrderService {
    OrderSummaryResponse previewOrder(Integer userId, String voucherCode);
    OrderResponse createOrder(Integer userId, CheckoutRequest request, String idempotencyHeader);
    void validatePaymentReceiptUpload(Integer orderId, Integer userId);
    void updatePaymentReceipt(Integer orderId, Integer userId, UploadedAsset asset);
    PaymentQrInfoResponse getPaymentQrInfo(Integer orderId, Integer userId);
    byte[] generatePaymentQrCode(Integer orderId, Integer userId);
    PaymentQrInfoResponse getAdminPaymentQrInfo(Integer orderId);
    byte[] generateAdminPaymentQrCode(Integer orderId);
    byte[] getCustomerReceipt(Integer orderId, Integer userId);
    byte[] getAdminReceipt(Integer orderId);
    List<OrderResponse> getUserOrders(Integer userId);
    PagedResponse<OrderResponse> getUserOrdersPage(Integer userId, int page, int size);
    AdminOrderMetricsResponse getAdminOrderMetrics();
    List<OrderResponse> getAllSystemOrders();
    PagedResponse<OrderResponse> getAllSystemOrdersPage(int page, int size);
    OrderResponse updateOrderStatus(Integer orderId, Integer statusId);
    OrderResponse verifyPayment(Integer orderId, String paymentStatus);
}
