package com.swt301.ecommerce.util;

import com.swt301.ecommerce.exception.BadRequestException;
import com.swt301.ecommerce.exception.PayloadTooLargeException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

public final class ImageFileValidator {
    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private ImageFileValidator() {
    }

    public static void validate(MultipartFile file, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn file ảnh");
        }
        if (file.getSize() > maxBytes) {
            throw new PayloadTooLargeException("Kích thước ảnh không được vượt quá " + (maxBytes / 1024 / 1024) + "MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        if (!ALLOWED_MIME.contains(contentType) || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Chỉ chấp nhận file JPG hoặc PNG hợp lệ");
        }
        try {
            byte[] header = file.getBytes();
            boolean jpeg = header.length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
            boolean png = header.length >= 8
                    && (header[0] & 0xFF) == 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47
                    && header[4] == 0x0D
                    && header[5] == 0x0A
                    && header[6] == 0x1A
                    && header[7] == 0x0A;
            if (("image/jpeg".equals(contentType) && !jpeg) || ("image/png".equals(contentType) && !png)) {
                throw new BadRequestException("Nội dung file không khớp với định dạng ảnh đã khai báo");
            }
        } catch (IOException ex) {
            throw new BadRequestException("Không thể đọc file ảnh");
        }
    }
}
