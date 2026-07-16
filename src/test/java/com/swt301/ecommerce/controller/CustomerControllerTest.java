package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.dto.request.*;
import com.swt301.ecommerce.dto.response.*;
import com.swt301.ecommerce.security.UserDetailsImpl;
import com.swt301.ecommerce.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerControllerTest {
    private final UserDetailsImpl principal = new UserDetailsImpl(7, "customer", "c@example.com", "x", List.of());

    @Test void authControllerDelegatesLoginAndRegister() {
        AuthService service = mock(AuthService.class);
        AuthController controller = new AuthController(service);
        LoginRequest login = new LoginRequest(); login.setUsername("u"); login.setPassword("p");
        JwtResponse jwt = JwtResponse.builder().token("jwt").id(7).username("u").role("ROLE_CUSTOMER").build();
        when(service.login(login)).thenReturn(jwt);
        assertThat(controller.login(login).getBody()).isSameAs(jwt);

        RegisterRequest register = new RegisterRequest();
        MessageResponse message = new MessageResponse("ok");
        when(service.register(register)).thenReturn(message);
        assertThat(controller.register(register).getBody()).isSameAs(message);
    }

    @Test void addressControllerDelegatesAllOperations() {
        AddressService service = mock(AddressService.class);
        AddressController controller = new AddressController(service);
        AddressRequest request = new AddressRequest();
        AddressResponse response = AddressResponse.builder().addressId(1).build();
        when(service.getUserAddresses(7)).thenReturn(List.of(response));
        when(service.createAddress(7, request)).thenReturn(response);
        when(service.updateAddress(1, 7, request)).thenReturn(response);
        when(service.deleteAddress(1, 7)).thenReturn(new MessageResponse("deleted"));
        assertThat((List<?>) controller.getMyAddresses(principal).getBody()).hasSize(1);
        assertThat(controller.createAddress(principal, request).getBody()).isSameAs(response);
        assertThat(controller.updateAddress(1, principal, request).getBody()).isSameAs(response);
        assertThat(controller.deleteAddress(1, principal).getBody()).isInstanceOf(MessageResponse.class);
    }

    @Test void cartControllerDelegatesAllOperations() {
        CartService service = mock(CartService.class);
        CartController controller = new CartController(service);
        CartItemRequest request = new CartItemRequest(); request.setProductId(1); request.setQuantity(2);
        CartResponse response = CartResponse.builder().cartId(1).items(List.of()).totalCartPrice(BigDecimal.ZERO).build();
        when(service.getCart(7)).thenReturn(response);
        when(service.addToCart(7, request)).thenReturn(response);
        when(service.updateCartItem(7, request)).thenReturn(response);
        when(service.removeCartItem(7, 9)).thenReturn(response);
        when(service.clearCart(7)).thenReturn(response);
        assertThat(controller.getMyCart(principal).getBody()).isSameAs(response);
        assertThat(controller.addToCart(principal, request).getBody()).isSameAs(response);
        assertThat(controller.updateCartItem(principal, request).getBody()).isSameAs(response);
        assertThat(controller.removeCartItem(9, principal).getBody()).isSameAs(response);
        assertThat(controller.clearCart(principal).getBody()).isSameAs(response);
    }

    @Test void profileVoucherAndPublicProductControllersDelegate() {
        UserProfileService profileService = mock(UserProfileService.class);
        UserProfileController profileController = new UserProfileController(profileService);
        ProfileResponse profile = ProfileResponse.builder().userId(7).build();
        ProfileUpdateRequest update = new ProfileUpdateRequest();
        when(profileService.getProfile(7)).thenReturn(profile);
        when(profileService.updateProfile(7, update)).thenReturn(profile);
        assertThat(profileController.getProfile(principal).getBody()).isSameAs(profile);
        assertThat(profileController.updateProfile(principal, update).getBody()).isSameAs(profile);

        VoucherService voucherService = mock(VoucherService.class);
        VoucherController voucherController = new VoucherController(voucherService);
        VoucherResponse voucher = VoucherResponse.builder().voucherCode("SAVE").build();
        when(voucherService.checkVoucher("SAVE", BigDecimal.TEN)).thenReturn(voucher);
        assertThat(voucherController.checkVoucher("SAVE", BigDecimal.TEN).getBody()).isSameAs(voucher);

        ProductService productService = mock(ProductService.class);
        PublicProductController productController = new PublicProductController(productService);
        ProductResponse product = ProductResponse.builder().productId(1).build();
        PagedResponse<ProductResponse> page = PagedResponse.<ProductResponse>builder().content(List.of(product)).build();
        when(productService.getAllActiveProducts()).thenReturn(List.of(product));
        when(productService.getProductPage(false, "q", 2, 0, 12)).thenReturn(page);
        when(productService.getActiveProductById(1)).thenReturn(product);
        assertThat(productController.getAllProducts().getBody()).containsExactly(product);
        assertThat(productController.getProductPage(0, 12, "q", 2).getBody()).isSameAs(page);
        assertThat(productController.getProductById(1).getBody()).isSameAs(product);
    }

    @Test void orderControllerDelegatesEveryReadAndCheckoutOperation() {
        OrderService orderService = mock(OrderService.class);
        FileUploadService fileService = mock(FileUploadService.class);
        OrderController controller = new OrderController(orderService, fileService);
        CheckoutRequest checkout = new CheckoutRequest();
        OrderSummaryResponse summary = OrderSummaryResponse.builder().subtotal(BigDecimal.TEN).build();
        OrderResponse order = OrderResponse.builder().orderId(1).build();
        PaymentQrInfoResponse qr = PaymentQrInfoResponse.builder().orderId(1).build();
        PagedResponse<OrderResponse> page = PagedResponse.<OrderResponse>builder().content(List.of(order)).build();
        when(orderService.previewOrder(7, "SAVE")).thenReturn(summary);
        when(orderService.createOrder(7, checkout, "key")).thenReturn(order);
        when(orderService.getPaymentQrInfo(1, 7)).thenReturn(qr);
        when(orderService.generatePaymentQrCode(1, 7)).thenReturn(new byte[]{1,2,3});
        when(orderService.getCustomerReceipt(1, 7)).thenReturn(new byte[]{4,5});
        when(orderService.getUserOrders(7)).thenReturn(List.of(order));
        when(orderService.getUserOrdersPage(7, 0, 10)).thenReturn(page);
        assertThat(controller.previewOrder(principal, "SAVE").getBody()).isSameAs(summary);
        assertThat(controller.createOrder(principal, "key", checkout).getBody()).isSameAs(order);
        assertThat(controller.getPaymentQrInfo(1, principal).getBody()).isSameAs(qr);
        assertThat(controller.getPaymentQrCode(1, principal).getHeaders().getContentType().toString()).isEqualTo("image/png");
        assertThat(controller.getReceipt(1, principal).getBody()).containsExactly(4,5);
        assertThat(controller.getMyOrders(principal).getBody()).containsExactly(order);
        assertThat(controller.getMyOrdersPage(principal, 0, 10).getBody()).isSameAs(page);
    }

    @Test void receiptUploadValidatesBeforeCloudinaryAndCleansOrphanOnFailure() {
        OrderService orderService = mock(OrderService.class);
        FileUploadService fileService = mock(FileUploadService.class);
        OrderController controller = new OrderController(orderService, fileService);
        MockMultipartFile file = new MockMultipartFile("file", "r.png", "image/png", new byte[]{1});
        UploadedAsset asset = UploadedAsset.builder().publicId("pid").secureUrl("url").build();
        when(fileService.uploadReceiptImage(file)).thenReturn(asset);
        doThrow(new RuntimeException("db failed")).when(orderService).updatePaymentReceipt(1, 7, asset);
        assertThatThrownBy(() -> controller.uploadQrPayment(1, principal, file)).isInstanceOf(RuntimeException.class);
        var order = inOrder(orderService, fileService);
        order.verify(orderService).validatePaymentReceiptUpload(1, 7);
        order.verify(fileService).uploadReceiptImage(file);
        verify(fileService).deleteReceiptImage("pid", "url");
    }
}
