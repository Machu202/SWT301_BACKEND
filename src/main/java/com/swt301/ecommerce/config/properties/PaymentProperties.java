package com.swt301.ecommerce.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "ecommerce.payment")
public class PaymentProperties {
    @NotBlank
    private String bankId;

    @NotBlank
    private String bankName;

    @NotBlank
    private String accountNumber;

    @NotBlank
    private String accountName;

    private String qrTemplate = "compact2";
    private String vietQrImageBaseUrl = "https://img.vietqr.io/image";
}
