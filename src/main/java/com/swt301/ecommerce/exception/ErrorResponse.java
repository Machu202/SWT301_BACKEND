// Vị trí: src/main/java/com/swt301/ecommerce/exception/ErrorResponse.java
package com.swt301.ecommerce.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private Map<String, String> errors; // Dùng để trả về tên trường bị lỗi (vd: { "receiverPhone": "Sai định dạng" })
}