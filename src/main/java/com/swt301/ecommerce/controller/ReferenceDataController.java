package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.dto.response.OrderStatusResponse;
import com.swt301.ecommerce.dto.response.PaymentMethodResponse;
import com.swt301.ecommerce.repository.OrderStatusRepository;
import com.swt301.ecommerce.repository.PaymentMethodRepository;
import com.swt301.ecommerce.util.PaymentMethodUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reference")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderStatusRepository orderStatusRepository;

    @GetMapping("/payment-methods")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<List<PaymentMethodResponse>> getPaymentMethods() {
        Map<String, PaymentMethodResponse> uniqueMethods = new LinkedHashMap<>();
        paymentMethodRepository.findAll(Sort.by(Sort.Direction.ASC, "paymentMethodId"))
                .stream()
                .filter(method -> PaymentMethodUtils.isSupported(method.getMethodName()))
                .forEach(method -> {
                    String canonicalName = PaymentMethodUtils.canonicalName(method.getMethodName());
                    uniqueMethods.putIfAbsent(canonicalName, PaymentMethodResponse.builder()
                            .paymentMethodId(method.getPaymentMethodId())
                            .methodName(canonicalName)
                            .build());
                });
        return ResponseEntity.ok(List.copyOf(uniqueMethods.values()));
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
