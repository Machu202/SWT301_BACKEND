// Vị trí: src/main/java/com/swt301/ecommerce/service/OrderService.java
package com.swt301.ecommerce.service;

import com.swt301.ecommerce.dto.response.OrderResponse;
import com.swt301.ecommerce.dto.response.OrderSummaryResponse;

public interface OrderService {
    // FE-11: Tính toán hóa đơn tạm tính
    OrderSummaryResponse previewOrder(Integer userId, String voucherCode);
    // FE-13 & FE-17: Chốt đơn hàng
    OrderResponse createOrder(Integer userId, com.swt301.ecommerce.dto.request.CheckoutRequest request);
    // FE-15: Cập nhật link ảnh QR vào đơn hàng
    void updatePaymentQrImage(Integer orderId, Integer userId, String qrImageUrl);

    // FE-19: Lấy danh sách lịch sử đơn hàng của User
    java.util.List<com.swt301.ecommerce.dto.response.OrderResponse> getUserOrders(Integer userId);
    // --- LUỒNG DÀNH CHO ADMIN ---
    // 1. Xem toàn bộ đơn hàng trong hệ thống
    java.util.List<com.swt301.ecommerce.dto.response.OrderResponse> getAllSystemOrders();
    
    // 2. Admin cập nhật trạng thái đơn hàng (VD: PENDING -> SHIPPED)
    com.swt301.ecommerce.dto.response.OrderResponse updateOrderStatus(Integer orderId, Integer statusId);
    
    // 3. Admin duyệt biên lai chuyển khoản QR (VD: PENDING_VERIFICATION -> PAID)
    com.swt301.ecommerce.dto.response.OrderResponse verifyPayment(Integer orderId, String paymentStatus);
}
