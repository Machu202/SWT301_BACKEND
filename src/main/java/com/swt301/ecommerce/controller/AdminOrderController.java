package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.dto.response.AdminOrderMetricsResponse;
import com.swt301.ecommerce.dto.response.OrderResponse;
import com.swt301.ecommerce.dto.response.PagedResponse;
import com.swt301.ecommerce.dto.response.PaymentQrInfoResponse;
import com.swt301.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderService orderService;

    @GetMapping("/metrics")
    public ResponseEntity<AdminOrderMetricsResponse> getMetrics() {
        return ResponseEntity.ok(orderService.getAdminOrderMetrics());
    }

    /** Backward-compatible capped list (maximum 100). Prefer /page. */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllSystemOrders());
    }

    @GetMapping("/page")
    public ResponseEntity<PagedResponse<OrderResponse>> getAllOrdersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getAllSystemOrdersPage(page, size));
    }

    @GetMapping(value = "/{id}/payment/receipt", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getReceipt(@PathVariable("id") Integer orderId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Content-Security-Policy", "default-src 'none'")
                .body(orderService.getAdminReceipt(orderId));
    }

    @GetMapping("/{id}/payment/qr-info")
    public ResponseEntity<PaymentQrInfoResponse> getPaymentQrInfo(@PathVariable("id") Integer orderId) {
        return ResponseEntity.ok(orderService.getAdminPaymentQrInfo(orderId));
    }

    @GetMapping(value = "/{id}/payment/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getPaymentQr(@PathVariable("id") Integer orderId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG)
                .header("Content-Security-Policy", "default-src 'none'")
                .body(orderService.generateAdminPaymentQrCode(orderId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable("id") Integer orderId,
            @RequestParam("statusId") Integer statusId) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, statusId));
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<OrderResponse> verifyPayment(
            @PathVariable("id") Integer orderId,
            @RequestParam("paymentStatus") String paymentStatus) {
        return ResponseEntity.ok(orderService.verifyPayment(orderId, paymentStatus));
    }
}
