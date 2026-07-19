package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.dto.request.CheckoutRequest;
import com.swt301.ecommerce.dto.response.OrderResponse;
import com.swt301.ecommerce.dto.response.OrderSummaryResponse;
import com.swt301.ecommerce.dto.response.PagedResponse;
import com.swt301.ecommerce.dto.response.UploadedAsset;
import com.swt301.ecommerce.security.UserDetailsImpl;
import com.swt301.ecommerce.service.FileUploadService;
import com.swt301.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
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
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(orderService.createOrder(currentUser.getId(), request, idempotencyKey));
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
        return imageResponse(orderService.generatePaymentQrCode(orderId, currentUser.getId()));
    }

    @PostMapping(value = "/{orderId}/payment/qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadQrPayment(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam("file") MultipartFile file) {
        // Ownership and workflow checks are deliberately performed before Cloudinary receives the file.
        orderService.validatePaymentReceiptUpload(orderId, currentUser.getId());
        UploadedAsset asset = fileUploadService.uploadReceiptImage(file);
        try {
            orderService.updatePaymentReceipt(orderId, currentUser.getId(), asset);
        } catch (RuntimeException ex) {
            fileUploadService.deleteReceiptImage(asset.getPublicId(), asset.getSecureUrl());
            throw ex;
        }
        return ResponseEntity.ok(Map.of(
                "message", "Tải biên lai thành công. Đang chờ Admin xác minh",
                "hasReceipt", true
        ));
    }

    @GetMapping(value = "/{orderId}/payment/receipt", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getReceipt(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Content-Security-Policy", "default-src 'none'")
                .body(orderService.getCustomerReceipt(orderId, currentUser.getId()));
    }

    /** Backward-compatible capped list (maximum 100). Prefer /page for UI rendering. */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(orderService.getUserOrders(currentUser.getId()));
    }

    @GetMapping("/page")
    public ResponseEntity<PagedResponse<OrderResponse>> getMyOrdersPage(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getUserOrdersPage(currentUser.getId(), page, size));
    }

    private ResponseEntity<byte[]> imageResponse(byte[] bytes) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG)
                .header("Content-Security-Policy", "default-src 'none'")
                .body(bytes);
    }
}
