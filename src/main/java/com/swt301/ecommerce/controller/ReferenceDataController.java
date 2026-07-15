package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.dto.response.OrderStatusResponse;
import com.swt301.ecommerce.dto.response.PaymentMethodResponse;
import com.swt301.ecommerce.repository.OrderStatusRepository;
import com.swt301.ecommerce.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/reference")
@RequiredArgsConstructor
public class ReferenceDataController {

    private static final Set<String> SUPPORTED_PAYMENT_METHODS = Set.of("COD", "QR_CODE");

    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderStatusRepository orderStatusRepository;

    @GetMapping("/payment-methods")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<List<PaymentMethodResponse>> getPaymentMethods() {
        List<PaymentMethodResponse> methods = paymentMethodRepository
                .findAll(Sort.by(Sort.Direction.ASC, "methodName"))
                .stream()
                .filter(method -> method.getMethodName() != null)
                .filter(method -> SUPPORTED_PAYMENT_METHODS.contains(
                        method.getMethodName().trim().toUpperCase(Locale.ROOT)))
                .map(method -> PaymentMethodResponse.builder()
                        .paymentMethodId(method.getPaymentMethodId())
                        .methodName(method.getMethodName())
                        .build())
                .toList();
        return ResponseEntity.ok(methods);
    }

    @GetMapping("/order-statuses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderStatusResponse>> getOrderStatuses() {
        List<OrderStatusResponse> statuses = orderStatusRepository
                .findAll(Sort.by(Sort.Direction.ASC, "statusId"))
                .stream()
                .map(status -> OrderStatusResponse.builder()
                        .statusId(status.getStatusId())
                        .statusName(status.getStatusName())
                        .build())
                .toList();
        return ResponseEntity.ok(statuses);
    }
}
