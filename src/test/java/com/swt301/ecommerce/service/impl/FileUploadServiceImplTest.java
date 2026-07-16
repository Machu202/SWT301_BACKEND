package com.swt301.ecommerce.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.swt301.ecommerce.exception.BadRequestException;
import com.swt301.ecommerce.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceImplTest {
    @Mock Cloudinary cloudinary;
    @Mock Uploader uploader;

    @Test void uploadsValidProductImage() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "public_id", "ecommerce_products/PROD_1",
                "secure_url", "https://cdn.example/p.png"));
        FileUploadServiceImpl service = new FileUploadServiceImpl(cloudinary);
        byte[] png = new byte[]{(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,1};
        MockMultipartFile file = new MockMultipartFile("file", "p.png", "image/png", png);
        var result = service.uploadProductImage(file);
        assertThat(result.getPublicId()).isEqualTo("ecommerce_products/PROD_1");
        assertThat(result.getSecureUrl()).isEqualTo("https://cdn.example/p.png");
    }

    @Test void uploadsReceiptAsAuthenticatedAsset() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "public_id", "ecommerce_receipts/RECEIPT_1",
                "secure_url", "https://cdn.example/r.png"));
        FileUploadServiceImpl service = new FileUploadServiceImpl(cloudinary);
        byte[] jpg = new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF,1};
        MockMultipartFile file = new MockMultipartFile("file", "r.jpg", "image/jpeg", jpg);
        service.uploadReceiptImage(file);
        verify(uploader).upload(any(byte[].class), argThat(options -> "authenticated".equals(options.get("type"))));
    }

    @Test void rejectsInvalidImageBeforeCloudinary() {
        FileUploadServiceImpl service = new FileUploadServiceImpl(cloudinary);
        MockMultipartFile file = new MockMultipartFile("file", "x.txt", "text/plain", "x".getBytes());
        assertThatThrownBy(() -> service.uploadProductImage(file)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(cloudinary);
    }

    @Test void wrapsCloudinaryUploadFailure() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("down"));
        FileUploadServiceImpl service = new FileUploadServiceImpl(cloudinary);
        byte[] png = new byte[]{(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,1};
        MockMultipartFile file = new MockMultipartFile("file", "p.png", "image/png", png);
        assertThatThrownBy(() -> service.uploadProductImage(file)).isInstanceOf(ExternalServiceException.class);
    }

    @Test void deletesProductUsingDerivedCloudinaryPublicId() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        FileUploadServiceImpl service = new FileUploadServiceImpl(cloudinary);
        service.deleteProductImage(null, "https://res.cloudinary.com/demo/image/upload/v123/ecommerce_products/PROD_1.png");
        verify(uploader).destroy(eq("ecommerce_products/PROD_1"), anyMap());
    }

    @Test void deleteIgnoresBlankAssetAndWrapsFailure() throws Exception {
        FileUploadServiceImpl service = new FileUploadServiceImpl(cloudinary);
        service.deleteProductImage(null, null);
        verifyNoInteractions(cloudinary);

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("pid"), anyMap())).thenThrow(new IOException("down"));
        assertThatThrownBy(() -> service.deleteReceiptImage("pid", null)).isInstanceOf(ExternalServiceException.class);
    }


    @Test void downloadsLegacyReceiptFromAuthenticatedServiceBoundary() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread worker = serveOnce(server, 200, new byte[]{9,8,7});
            FileUploadServiceImpl service = new FileUploadServiceImpl(cloudinary);
            byte[] result = service.downloadReceipt(null, "http://127.0.0.1:" + server.getLocalPort() + "/receipt");
            assertThat(result).containsExactly(9,8,7);
            worker.join(2000);
        }
    }

    @Test void nonSuccessfulReceiptDownloadIsExternalServiceFailure() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread worker = serveOnce(server, 404, new byte[0]);
            FileUploadServiceImpl service = new FileUploadServiceImpl(cloudinary);
            String url = "http://127.0.0.1:" + server.getLocalPort() + "/receipt";
            assertThatThrownBy(() -> service.downloadReceipt(null, url)).isInstanceOf(ExternalServiceException.class);
            worker.join(2000);
        }
    }

    private Thread serveOnce(ServerSocket server, int status, byte[] body) {
        Thread worker = new Thread(() -> {
            try (Socket socket = server.accept()) {
                int matched = 0;
                while (matched < 4) {
                    int value = socket.getInputStream().read();
                    if (value < 0) break;
                    byte expected = new byte[]{'\r','\n','\r','\n'}[matched];
                    matched = value == expected ? matched + 1 : (value == '\r' ? 1 : 0);
                }
                String reason = status == 200 ? "OK" : "Not Found";
                String headers = "HTTP/1.1 " + status + " " + reason + "\r\n"
                        + "Content-Length: " + body.length + "\r\n"
                        + "Connection: close\r\n\r\n";
                socket.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().write(body);
                socket.getOutputStream().flush();
            } catch (IOException ignored) {
                // The assertion in the test captures client-side failures.
            }
        });
        worker.setDaemon(true);
        worker.start();
        return worker;
    }

    @Test void downloadReceiptRejectsMissingReference() {
        FileUploadServiceImpl service = new FileUploadServiceImpl(cloudinary);
        assertThatThrownBy(() -> service.downloadReceipt(null, null)).isInstanceOf(ExternalServiceException.class);
    }
}
