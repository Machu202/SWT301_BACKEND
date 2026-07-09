// Vị trí: src/main/java/com/swt301/ecommerce/controller/OrderController.java
package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.dto.response.OrderSummaryResponse;

import com.swt301.ecommerce.security.UserDetailsImpl;
import com.swt301.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;
    private final com.swt301.ecommerce.service.FileUploadService fileUploadService;

    // FE-11: Xem trước hóa đơn tính tiền
    @GetMapping("/preview")
    public ResponseEntity<OrderSummaryResponse> previewOrder(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(value = "voucherCode", required = false) String voucherCode) {
        
        OrderSummaryResponse response = orderService.previewOrder(currentUser.getId(), voucherCode);
        return ResponseEntity.ok(response);
    }
    // FE-13 & FE-17: Chốt đơn hàng
    @PostMapping("/checkout")
    public ResponseEntity<com.swt301.ecommerce.dto.response.OrderResponse> createOrder(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @jakarta.validation.Valid @RequestBody com.swt301.ecommerce.dto.request.CheckoutRequest request) {
        
        return ResponseEntity.ok(orderService.createOrder(currentUser.getId(), request));
    }
    // NHỚ THÊM DEPENDENCY NÀY VÀO ĐẦU CONTROLLER:
    // private final com.teamproject.ecommerceapi.service.FileUploadService fileUploadService;

    // 3. FE-15: Tải ảnh bill QR
    @PostMapping(value = "/{orderId}/payment/qr", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadQrPayment(
            @PathVariable("orderId") Integer orderId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        // Validate File
        if (file.isEmpty()) throw new RuntimeException("Vui lòng chọn file ảnh");
        if (file.getSize() > 5 * 1024 * 1024) throw new RuntimeException("Kích thước ảnh không được vượt quá 5MB");
        
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new RuntimeException("Chỉ chấp nhận file định dạng JPG hoặc PNG");
        }

        // Upload và lưu DB
        String imageUrl = fileUploadService.uploadImage(file);
        orderService.updatePaymentQrImage(orderId, currentUser.getId(), imageUrl);

        return ResponseEntity.ok(java.util.Map.of(
                "message", "Tải ảnh hóa đơn thành công",
                "qrUrl", imageUrl
        ));
    }

    // 4. FE-19: Xem lịch sử đơn hàng
    @GetMapping
    public ResponseEntity<java.util.List<com.swt301.ecommerce.dto.response.OrderResponse>> getMyOrders(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        return ResponseEntity.ok(orderService.getUserOrders(currentUser.getId()));
    }
}