package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.dto.request.CheckoutRequest;
import com.swt301.ecommerce.dto.response.OrderResponse;
import com.swt301.ecommerce.dto.response.OrderSummaryResponse;
import com.swt301.ecommerce.security.UserDetailsImpl;
import com.swt301.ecommerce.service.FileUploadService;
import com.swt301.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;
    private final FileUploadService fileUploadService;

    @GetMapping("/preview")
    public ResponseEntity<OrderSummaryResponse> previewOrder(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(value = "voucherCode", required = false) String voucherCode) {
        return ResponseEntity.ok(orderService.previewOrder(currentUser.getId(), voucherCode));
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(orderService.createOrder(currentUser.getId(), request));
    }

    @GetMapping("/{orderId}/payment/qr-info")
    public ResponseEntity<?> getPaymentQrInfo(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(orderService.getPaymentQrInfo(orderId, currentUser.getId()));
    }

    @GetMapping(value = "/{orderId}/payment/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getPaymentQrCode(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG)
                .body(orderService.generatePaymentQrCode(orderId, currentUser.getId()));
    }

    @PostMapping(value = "/{orderId}/payment/qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadQrPayment(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn file ảnh");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("Kích thước ảnh không được vượt quá 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new RuntimeException("Chỉ chấp nhận file định dạng JPG hoặc PNG");
        }

        String imageUrl = fileUploadService.uploadImage(file);
        orderService.updatePaymentQrImage(orderId, currentUser.getId(), imageUrl);
        return ResponseEntity.ok(Map.of(
                "message", "Tải biên lai thành công. Đang chờ Admin xác minh",
                "qrUrl", imageUrl
        ));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(orderService.getUserOrders(currentUser.getId()));
    }
}
