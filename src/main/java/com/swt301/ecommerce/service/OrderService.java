package com.swt301.ecommerce.service;

import com.swt301.ecommerce.dto.request.CheckoutRequest;
import com.swt301.ecommerce.dto.response.OrderResponse;
import com.swt301.ecommerce.dto.response.OrderSummaryResponse;
import com.swt301.ecommerce.dto.response.PaymentQrInfoResponse;

import java.util.List;

public interface OrderService {
    OrderSummaryResponse previewOrder(Integer userId, String voucherCode);
    OrderResponse createOrder(Integer userId, CheckoutRequest request);
    void validatePaymentReceiptUpload(Integer orderId, Integer userId);
    void updatePaymentQrImage(Integer orderId, Integer userId, String qrImageUrl);
    PaymentQrInfoResponse getPaymentQrInfo(Integer orderId, Integer userId);
    byte[] generatePaymentQrCode(Integer orderId, Integer userId);
    List<OrderResponse> getUserOrders(Integer userId);
    List<OrderResponse> getAllSystemOrders();
    OrderResponse updateOrderStatus(Integer orderId, Integer statusId);
    OrderResponse verifyPayment(Integer orderId, String paymentStatus);
}
