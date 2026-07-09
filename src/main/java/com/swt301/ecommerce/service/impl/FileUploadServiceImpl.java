// Vị trí: src/main/java/com/swt301/ecommerce/service/impl/FileUploadServiceImpl.java
package com.swt301.ecommerce.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.swt301.ecommerce.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) {
        try {
            // Tạo tên file ngẫu nhiên (Public ID)
            String publicId = "QR_" + UUID.randomUUID().toString();
            
            // Upload lên thư mục "ecommerce_qr" trên Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("public_id", publicId, "folder", "ecommerce_qr"));
            
            // Trả về đường link ảnh bảo mật (https)
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tải ảnh lên server: " + e.getMessage());
        }
    }
    @Override
    public String uploadProductImage(MultipartFile file) {
        try {
            // Đặt tên file bắt đầu bằng PROD_
            String publicId = "PROD_" + UUID.randomUUID().toString();
            
            // Upload vào thư mục "ecommerce_products"
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("public_id", publicId, "folder", "ecommerce_products"));
            
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tải ảnh sản phẩm: " + e.getMessage());
        }
    }
}