package com.swt301.ecommerce.util;

import com.swt301.ecommerce.exception.BadRequestException;
import com.swt301.ecommerce.exception.PayloadTooLargeException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;

class ImageFileValidatorTest {
    private static final byte[] PNG = new byte[]{(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,1};
    private static final byte[] JPG = new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF,1};

    @Test void acceptsValidPng() {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", PNG);
        assertThatCode(() -> ImageFileValidator.validate(file, 100)).doesNotThrowAnyException();
    }

    @Test void acceptsValidJpeg() {
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", JPG);
        assertThatCode(() -> ImageFileValidator.validate(file, 100)).doesNotThrowAnyException();
    }

    @Test void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> ImageFileValidator.validate(file, 100)).isInstanceOf(BadRequestException.class);
    }

    @Test void rejectsOversizedFile() {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", PNG);
        assertThatThrownBy(() -> ImageFileValidator.validate(file, 4)).isInstanceOf(PayloadTooLargeException.class);
    }

    @Test void rejectsBadMimeOrExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "image.exe", "application/octet-stream", PNG);
        assertThatThrownBy(() -> ImageFileValidator.validate(file, 100)).isInstanceOf(BadRequestException.class);
    }

    @Test void rejectsSignatureMismatch() {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", JPG);
        assertThatThrownBy(() -> ImageFileValidator.validate(file, 100)).isInstanceOf(BadRequestException.class);
    }
}
