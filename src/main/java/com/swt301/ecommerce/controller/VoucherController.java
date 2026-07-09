// Vị trí: src/main/java/com/swt301/ecommerce/controller/VoucherController.java
package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/check")
    public ResponseEntity<?> checkVoucher(
            @RequestParam("code") String code,
            @RequestParam("orderSubtotal") BigDecimal orderSubtotal) {
        return ResponseEntity.ok(voucherService.checkVoucher(code, orderSubtotal));
    }
}