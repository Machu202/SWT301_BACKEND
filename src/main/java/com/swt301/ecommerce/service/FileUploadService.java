package com.swt301.ecommerce.service;

import com.swt301.ecommerce.dto.response.UploadedAsset;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    UploadedAsset uploadReceiptImage(MultipartFile file);
    UploadedAsset uploadProductImage(MultipartFile file);
    byte[] downloadReceipt(String receiptPublicId, String legacyUrl);
    void deleteProductImage(String publicId, String imageUrl);
    void deleteReceiptImage(String publicId, String legacyUrl);
}
