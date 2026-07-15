package com.swt301.ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OrderStatusResponse {
    private Integer statusId;
    private String statusName;
}
