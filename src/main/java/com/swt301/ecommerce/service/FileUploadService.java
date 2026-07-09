// Vị trí: src/main/java/com/swt301/ecommerce/service/FileUploadService.java
package com.swt301.ecommerce.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String uploadImage(MultipartFile file); // Dùng cho ảnh QR
    String uploadProductImage(MultipartFile file); // Dùng cho ảnh Sản phẩm
}