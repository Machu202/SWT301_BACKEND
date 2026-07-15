package com.swt301.ecommerce.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.swt301.ecommerce.dto.response.UploadedAsset;
import com.swt301.ecommerce.exception.ExternalServiceException;
import com.swt301.ecommerce.service.FileUploadService;
import com.swt301.ecommerce.util.ImageFileValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public UploadedAsset uploadReceiptImage(MultipartFile file) {
        ImageFileValidator.validate(file, MAX_IMAGE_BYTES);
        return upload(file, "ecommerce_receipts", "RECEIPT_", true);
    }

    @Override
    public UploadedAsset uploadProductImage(MultipartFile file) {
        ImageFileValidator.validate(file, MAX_IMAGE_BYTES);
        return upload(file, "ecommerce_products", "PROD_", false);
    }

    @Override
    public byte[] downloadReceipt(String receiptPublicId, String legacyUrl) {
        String url;
        if (receiptPublicId != null && !receiptPublicId.isBlank()) {
            url = cloudinary.url()
                    .resourceType("image")
                    .type("authenticated")
                    .signed(true)
                    .secure(true)
                    .generate(receiptPublicId);
        } else if (legacyUrl != null && !legacyUrl.isBlank()) {
            url = legacyUrl;
        } else {
            throw new ExternalServiceException("Không tìm thấy ảnh biên lai", null);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ExternalServiceException("Không thể tải ảnh biên lai", null);
            }
            return response.body();
        } catch (IOException ex) {
            throw new ExternalServiceException("Không thể tải ảnh biên lai", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Tải ảnh biên lai bị gián đoạn", ex);
        }
    }

    @Override
    public void deleteProductImage(String publicId, String imageUrl) {
        deleteAsset(publicId != null && !publicId.isBlank() ? publicId : derivePublicId(imageUrl), "upload");
    }

    @Override
    public void deleteReceiptImage(String publicId, String legacyUrl) {
        String resolved = publicId != null && !publicId.isBlank() ? publicId : derivePublicId(legacyUrl);
        deleteAsset(resolved, publicId != null && !publicId.isBlank() ? "authenticated" : "upload");
    }

    private UploadedAsset upload(MultipartFile file, String folder, String prefix, boolean authenticated) {
        try {
            String publicId = folder + "/" + prefix + UUID.randomUUID();
            Map<String, Object> options = authenticated
                    ? ObjectUtils.asMap("public_id", publicId, "type", "authenticated", "resource_type", "image")
                    : ObjectUtils.asMap("public_id", publicId, "resource_type", "image");
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            return UploadedAsset.builder()
                    .publicId(String.valueOf(uploadResult.get("public_id")))
                    .secureUrl(uploadResult.get("secure_url") == null ? null : String.valueOf(uploadResult.get("secure_url")))
                    .build();
        } catch (IOException ex) {
            throw new ExternalServiceException("Lỗi khi tải ảnh lên Cloudinary", ex);
        }
    }

    private void deleteAsset(String publicId, String type) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", "image", "type", type, "invalidate", true));
        } catch (IOException ex) {
            throw new ExternalServiceException("Không thể xóa ảnh cũ trên Cloudinary", ex);
        }
    }

    private String derivePublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        try {
            String path = URI.create(imageUrl).getPath();
            int uploadIndex = path.indexOf("/upload/");
            if (uploadIndex < 0) return null;
            String afterUpload = path.substring(uploadIndex + "/upload/".length());
            afterUpload = afterUpload.replaceFirst("^v\\d+/", "");
            int dot = afterUpload.lastIndexOf('.');
            return dot > 0 ? afterUpload.substring(0, dot) : afterUpload;
        } catch (Exception ignored) {
            return null;
        }
    }
}
