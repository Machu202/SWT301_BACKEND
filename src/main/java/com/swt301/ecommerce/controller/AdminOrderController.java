// Vị trí: src/main/java/com/swt301/ecommerce/controller/AdminOrderController.java
package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    // 1. Xem tất cả đơn hàng
    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllSystemOrders());
    }

    // 2. Cập nhật trạng thái đơn hàng (Truyền ID trạng thái mới xuống)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable("id") Integer orderId,
            @RequestParam("statusId") Integer statusId) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, statusId));
    }

    // 3. Cập nhật trạng thái thanh toán (Duyệt biên lai QR)
    @PutMapping("/{id}/payment")
    public ResponseEntity<?> verifyPayment(
            @PathVariable("id") Integer orderId,
            @RequestParam("paymentStatus") String paymentStatus) {
        return ResponseEntity.ok(orderService.verifyPayment(orderId, paymentStatus));
    }
}