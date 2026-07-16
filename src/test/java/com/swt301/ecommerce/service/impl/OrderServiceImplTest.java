package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.config.properties.PaymentProperties;
import com.swt301.ecommerce.dto.request.CheckoutRequest;
import com.swt301.ecommerce.dto.request.ManualAddressRequest;
import com.swt301.ecommerce.dto.response.UploadedAsset;
import com.swt301.ecommerce.entity.*;
import com.swt301.ecommerce.enums.PaymentStatus;
import com.swt301.ecommerce.enums.ProductStatus;
import com.swt301.ecommerce.exception.*;
import com.swt301.ecommerce.repository.*;
import com.swt301.ecommerce.service.FileUploadService;
import com.swt301.ecommerce.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplTest {
    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock VoucherRepository voucherRepository;
    @Mock AddressRepository addressRepository;
    @Mock PaymentMethodRepository paymentMethodRepository;
    @Mock OrderStatusRepository orderStatusRepository;
    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock ProductRepository productRepository;
    @Mock FileUploadService fileUploadService;
    @Mock PaymentProperties paymentProperties;
    @Mock ResourceLoader resourceLoader;
    @InjectMocks OrderServiceImpl service;

    private User user;
    private Product product;
    private Cart cart;
    private CartItem cartItem;
    private Address address;

    @BeforeEach void setUp() {
        user = TestFixtures.user(1, "CUSTOMER");
        product = TestFixtures.product(10, 12, new BigDecimal("300000"));
        cart = TestFixtures.cart(20, user);
        cartItem = TestFixtures.cartItem(30, cart, product, 2);
        address = TestFixtures.address(40, user, true);
    }

    @Test void previewUsesCurrentProductPriceAndShippingRule() {
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        var result = service.previewOrder(1, null);
        assertThat(result.getSubtotal()).isEqualByComparingTo("600000");
        assertThat(result.getShippingFee()).isZero();
        assertThat(result.getItems()).hasSize(1);
    }

    @Test void previewRejectsEmptyCartInactiveProductAndInsufficientStock() {
        Cart empty = TestFixtures.cart(21, user);
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(empty));
        assertThatThrownBy(() -> service.previewOrder(1, null)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least one cart item");

        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        product.setStatus(ProductStatus.INACTIVE);
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        assertThatThrownBy(() -> service.previewOrder(1, null)).isInstanceOf(BusinessRuleException.class);
        product.setStatus(ProductStatus.ACTIVE); product.setStock(1);
        assertThatThrownBy(() -> service.previewOrder(1, null)).isInstanceOf(ConflictException.class);
    }

    @Test void previewAppliesPercentVoucher() {
        Voucher voucher = TestFixtures.voucher(1, "PERCENT", new BigDecimal("10"));
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(voucherRepository.findByVoucherCode("SAVE1")).thenReturn(Optional.of(voucher));
        var result = service.previewOrder(1, " SAVE1 ");
        assertThat(result.getDiscount()).isEqualByComparingTo("60000");
        assertThat(result.getTotal()).isEqualByComparingTo("540000");
    }

    @Test void createsCodOrderWithSixDigitCodeAndDecrementsStock() {
        CheckoutRequest request = savedAddressRequest(1, null);
        stubCreateBase("COD");
        var result = service.createOrder(1, request, "checkout-key");
        assertThat(result.getOrderCode()).matches("ORD-\\d{6}");
        assertThat(result.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(product.getStock()).isEqualTo(10);
        assertThat(cart.getCartItems()).isEmpty();
        verify(cartItemRepository).deleteAll(anyList());
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test void createsQrOrderWithManualAddressAndAwaitingPayment() {
        CheckoutRequest request = manualAddressRequest(2);
        stubCreateBase("QR_CODE");
        var result = service.createOrder(1, request, null);
        assertThat(result.getPaymentStatus()).isEqualTo("AWAITING_PAYMENT");
        assertThat(result.getShippingAddress()).contains("Manual Street", "Manual Ward");
    }

    @Test void createOrderReturnsExistingOrderForSameIdempotencyKey() {
        Order existing = TestFixtures.order(5, user, "PENDING", "COD");
        Payment payment = TestFixtures.payment(1, existing, PaymentStatus.PENDING);
        when(orderRepository.findByUser_UserIdAndIdempotencyKey(1, "same-key")).thenReturn(Optional.of(existing));
        when(paymentRepository.findByOrder_OrderId(5)).thenReturn(Optional.of(payment));
        when(orderItemRepository.findByOrder_OrderId(5)).thenReturn(List.of());
        var result = service.createOrder(1, savedAddressRequest(1, null), "same-key");
        assertThat(result.getOrderId()).isEqualTo(5);
        verifyNoInteractions(cartRepository);
    }

    @Test void createOrderRequiresIdempotencyAndExactlyOneAddressMode() {
        assertThatThrownBy(() -> service.createOrder(1, savedAddressRequest(1, null), null))
                .isInstanceOf(BadRequestException.class);

        CheckoutRequest both = savedAddressRequest(1, "key");
        both.setManualAddress(manual());
        when(orderRepository.findByUser_UserIdAndIdempotencyKey(1, "key")).thenReturn(Optional.empty());
        when(cartRepository.findByUserIdForUpdate(1)).thenReturn(Optional.of(cart));
        assertThatThrownBy(() -> service.createOrder(1, both, "key")).isInstanceOf(BadRequestException.class);
    }

    @Test void createOrderRejectsUnsupportedPaymentMethod() {
        CheckoutRequest request = savedAddressRequest(99, "key");
        when(orderRepository.findByUser_UserIdAndIdempotencyKey(1, "key")).thenReturn(Optional.empty());
        when(cartRepository.findByUserIdForUpdate(1)).thenReturn(Optional.of(cart));
        when(addressRepository.findByAddressIdAndUser_UserId(40, 1)).thenReturn(Optional.of(address));
        when(paymentMethodRepository.findById(99)).thenReturn(Optional.of(TestFixtures.paymentMethod(99, "CRYPTO")));
        assertThatThrownBy(() -> service.createOrder(1, request, "key")).isInstanceOf(BusinessRuleException.class);
    }

    @Test void receiptPreflightChecksOwnershipMethodOrderAndPaymentState() {
        Order order = TestFixtures.order(5, user, "PENDING", "QR_CODE");
        Payment payment = TestFixtures.payment(1, order, PaymentStatus.AWAITING_PAYMENT);
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_OrderId(5)).thenReturn(Optional.of(payment));
        assertThatCode(() -> service.validatePaymentReceiptUpload(5, 1)).doesNotThrowAnyException();

        assertThatThrownBy(() -> service.validatePaymentReceiptUpload(5, 2)).isInstanceOf(AccessDeniedException.class);
        order.getPaymentMethod().setMethodName("COD");
        assertThatThrownBy(() -> service.validatePaymentReceiptUpload(5, 1)).isInstanceOf(BusinessRuleException.class);
    }

    @Test void replacingReceiptDeletesOldReceiptAfterSave() {
        Order order = TestFixtures.order(5, user, "PENDING", "QR_CODE");
        Payment payment = TestFixtures.payment(1, order, PaymentStatus.FAILED);
        payment.setReceiptPublicId("old-id"); payment.setQrImage("old-url");
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderIdForUpdate(5)).thenReturn(Optional.of(payment));
        UploadedAsset asset = UploadedAsset.builder().publicId("new-id").secureUrl("new-url").build();
        service.updatePaymentReceipt(5, 1, asset);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING_VERIFICATION);
        assertThat(payment.getReceiptPublicId()).isEqualTo("new-id");
        verify(fileUploadService).deleteReceiptImage("old-id", "old-url");
    }

    @Test void getsQrInfoFromBackendConfiguration() {
        Order order = TestFixtures.order(5, user, "PENDING", "QR_CODE");
        Payment payment = TestFixtures.payment(1, order, PaymentStatus.AWAITING_PAYMENT);
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_OrderId(5)).thenReturn(Optional.of(payment));
        when(paymentProperties.getBankId()).thenReturn("970422");
        when(paymentProperties.getBankName()).thenReturn("MB");
        when(paymentProperties.getAccountNumber()).thenReturn("0123456789");
        when(paymentProperties.getAccountName()).thenReturn("TEST ACCOUNT");
        var info = service.getPaymentQrInfo(5, 1);
        assertThat(info.getAmount()).isEqualByComparingTo("130000");
        assertThat(info.getTransferNote()).isEqualTo(order.getOrderCode());
        assertThat(info.getBankName()).isEqualTo("MB");
    }

    @Test void receiptDownloadChecksCustomerOwnershipAndAdminExistence() {
        Order order = TestFixtures.order(5, user, "PENDING", "QR_CODE");
        Payment payment = TestFixtures.payment(1, order, PaymentStatus.PENDING_VERIFICATION);
        payment.setReceiptPublicId("receipt-id");
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));
        when(orderRepository.existsById(5)).thenReturn(true);
        when(paymentRepository.findByOrder_OrderId(5)).thenReturn(Optional.of(payment));
        when(fileUploadService.downloadReceipt("receipt-id", null)).thenReturn(new byte[]{1,2});
        assertThat(service.getCustomerReceipt(5, 1)).containsExactly(1,2);
        assertThat(service.getAdminReceipt(5)).containsExactly(1,2);
    }

    @Test void mapsPagedOrdersAndItemsWithoutNPlusOneCalls() {
        Order order = TestFixtures.order(5, user, "PENDING", "COD");
        OrderItem item = TestFixtures.orderItem(1, order, product, 2);
        Payment payment = TestFixtures.payment(1, order, PaymentStatus.PENDING);
        when(orderRepository.findByUser_UserId(eq(1), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(order)));
        when(paymentRepository.findByOrder_OrderIdIn(List.of(5))).thenReturn(List.of(payment));
        when(orderItemRepository.findByOrder_OrderIdIn(List.of(5))).thenReturn(List.of(item));
        var page = service.getUserOrdersPage(1, 0, 10);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getItems()).hasSize(1);
        verify(paymentRepository, never()).findByOrder_OrderId(5);
    }

    @Test void adminMetricsCountsOnlyPaidRevenue() {
        when(orderRepository.count()).thenReturn(10L);
        when(paymentRepository.countByPaymentStatus(PaymentStatus.PENDING_VERIFICATION)).thenReturn(2L);
        when(paymentRepository.sumAmountByPaymentStatus(PaymentStatus.PAID)).thenReturn(new BigDecimal("900000"));
        var result = service.getAdminOrderMetrics();
        assertThat(result.getTotalOrders()).isEqualTo(10);
        assertThat(result.getPaidRevenue()).isEqualByComparingTo("900000");
    }

    @Test void enforcesOrderTransitionsAndQrPaymentRule() {
        Order order = TestFixtures.order(5, user, "PENDING", "QR_CODE");
        Payment payment = TestFixtures.payment(1, order, PaymentStatus.AWAITING_PAYMENT);
        when(orderRepository.findByOrderId(5)).thenReturn(Optional.of(order));
        when(orderStatusRepository.findById(2)).thenReturn(Optional.of(TestFixtures.orderStatus(2, "PROCESSING")));
        when(paymentRepository.findByOrderIdForUpdate(5)).thenReturn(Optional.of(payment));
        assertThatThrownBy(() -> service.updateOrderStatus(5, 2)).isInstanceOf(BusinessRuleException.class);

        when(orderStatusRepository.findById(3)).thenReturn(Optional.of(TestFixtures.orderStatus(3, "DELIVERED")));
        assertThatThrownBy(() -> service.updateOrderStatus(5, 3)).isInstanceOf(BusinessRuleException.class);
    }

    @Test void cancellingOrderRestoresStockVoucherAndPayment() {
        Order order = TestFixtures.order(5, user, "PENDING", "COD");
        Voucher voucher = TestFixtures.voucher(1, "FIXED", BigDecimal.TEN); voucher.setQuantity(4); order.setVoucher(voucher);
        OrderItem item = TestFixtures.orderItem(1, order, product, 2);
        Payment payment = TestFixtures.payment(1, order, PaymentStatus.PENDING);
        when(orderRepository.findByOrderId(5)).thenReturn(Optional.of(order));
        when(orderStatusRepository.findById(9)).thenReturn(Optional.of(TestFixtures.orderStatus(9, "CANCELLED")));
        when(paymentRepository.findByOrderIdForUpdate(5)).thenReturn(Optional.of(payment));
        when(orderItemRepository.findByOrder_OrderId(5)).thenReturn(List.of(item));
        when(productRepository.findByIdForUpdate(10)).thenReturn(Optional.of(product));
        when(voucherRepository.findByIdForUpdate(1)).thenReturn(Optional.of(voucher));
        when(paymentRepository.findByOrder_OrderId(5)).thenReturn(Optional.of(payment));
        service.updateOrderStatus(5, 9);
        assertThat(product.getStock()).isEqualTo(14);
        assertThat(voucher.getQuantity()).isEqualTo(5);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(order.getStatus().getStatusName()).isEqualTo("CANCELLED");
    }

    @Test void deliveredCodMarksPaymentPaid() {
        Order order = TestFixtures.order(5, user, "SHIPPED", "COD");
        Payment payment = TestFixtures.payment(1, order, PaymentStatus.PENDING);
        when(orderRepository.findByOrderId(5)).thenReturn(Optional.of(order));
        when(orderStatusRepository.findById(4)).thenReturn(Optional.of(TestFixtures.orderStatus(4, "DELIVERED")));
        when(paymentRepository.findByOrderIdForUpdate(5)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByOrder_OrderId(5)).thenReturn(Optional.of(payment));
        when(orderItemRepository.findByOrder_OrderId(5)).thenReturn(List.of());
        service.updateOrderStatus(5, 4);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaymentDate()).isNotNull();
    }

    @Test void verifyPaymentRequiresQrPendingReceiptAndValidTarget() {
        Order order = TestFixtures.order(5, user, "PENDING", "QR_CODE");
        Payment payment = TestFixtures.payment(1, order, PaymentStatus.PENDING_VERIFICATION);
        payment.setReceiptPublicId("receipt");
        when(paymentRepository.findByOrderIdForUpdate(5)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByOrder_OrderId(5)).thenReturn(Optional.of(payment));
        when(orderItemRepository.findByOrder_OrderId(5)).thenReturn(List.of());
        service.verifyPayment(5, "PAID");
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getVerifiedAt()).isNotNull();

        assertThatThrownBy(() -> service.verifyPayment(5, "UNKNOWN")).isInstanceOf(BadRequestException.class);
    }


    @Test void returnsConfiguredProjectQrForCustomerAndAdmin() {
        Order order = TestFixtures.order(5, user, "PENDING", "QR_CODE");
        Payment payment = TestFixtures.payment(1, order, PaymentStatus.AWAITING_PAYMENT);
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_OrderId(5)).thenReturn(Optional.of(payment));
        when(paymentProperties.getBankId()).thenReturn("OMS_ID");
        when(paymentProperties.getBankName()).thenReturn("OMS_Bank");
        when(paymentProperties.getAccountNumber()).thenReturn("1234567890");
        when(paymentProperties.getAccountName()).thenReturn("OMS_Account");
        when(paymentProperties.getQrImageResource()).thenReturn("classpath:/static/payment/vietqr-payment.png");
        byte[] configuredPng = new byte[128];
        configuredPng[0] = (byte) 0x89; configuredPng[1] = 0x50; configuredPng[2] = 0x4E; configuredPng[3] = 0x47;
        when(resourceLoader.getResource("classpath:/static/payment/vietqr-payment.png"))
                .thenReturn(new ByteArrayResource(configuredPng));

        byte[] customerQr = service.generatePaymentQrCode(5, 1);
        byte[] adminQr = service.generateAdminPaymentQrCode(5);
        assertThat(customerQr).isEqualTo(configuredPng);
        assertThat(adminQr).isEqualTo(configuredPng);
    }

    @Test void returnsCappedCustomerAndAdminListsThroughPagedQueries() {
        Order order = TestFixtures.order(5, user, "PENDING", "COD");
        Payment payment = TestFixtures.payment(1, order, PaymentStatus.PENDING);
        when(orderRepository.findByUser_UserId(eq(1), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(order)));
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(order)));
        when(paymentRepository.findByOrder_OrderIdIn(List.of(5))).thenReturn(List.of(payment));
        when(orderItemRepository.findByOrder_OrderIdIn(List.of(5))).thenReturn(List.of());
        assertThat(service.getUserOrders(1)).hasSize(1);
        assertThat(service.getAllSystemOrders()).hasSize(1);
        assertThat(service.getAllSystemOrdersPage(0, 10).getContent()).hasSize(1);
    }

    private void stubCreateBase(String method) {
        when(orderRepository.findByUser_UserIdAndIdempotencyKey(eq(1), anyString())).thenReturn(Optional.empty());
        when(cartRepository.findByUserIdForUpdate(1)).thenReturn(Optional.of(cart));
        when(addressRepository.findByAddressIdAndUser_UserId(40, 1)).thenReturn(Optional.of(address));
        when(paymentMethodRepository.findById(anyInt())).thenReturn(Optional.of(TestFixtures.paymentMethod(1, method)));
        when(orderStatusRepository.findByStatusName("PENDING")).thenReturn(Optional.of(TestFixtures.orderStatus(1, "PENDING")));
        when(productRepository.findByIdForUpdate(10)).thenReturn(Optional.of(product));
        when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> { Order o = inv.getArgument(0); o.setOrderId(5); return o; });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> { OrderItem i = inv.getArgument(0); i.setOrderItemId(1); return i; });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> { Payment p = inv.getArgument(0); p.setPaymentId(1); return p; });
    }

    private CheckoutRequest savedAddressRequest(int paymentMethodId, String bodyKey) {
        CheckoutRequest request = new CheckoutRequest();
        request.setAddressId(40); request.setPaymentMethodId(paymentMethodId); request.setIdempotencyKey(bodyKey); return request;
    }

    private CheckoutRequest manualAddressRequest(int paymentMethodId) {
        CheckoutRequest request = new CheckoutRequest();
        request.setManualAddress(manual()); request.setPaymentMethodId(paymentMethodId); request.setIdempotencyKey("manual-key"); return request;
    }

    private ManualAddressRequest manual() {
        ManualAddressRequest manual = new ManualAddressRequest();
        manual.setReceiverName("Manual Receiver"); manual.setReceiverPhone("0912345678");
        manual.setProvince("Manual Province"); manual.setDistrict("Manual District");
        manual.setWard("Manual Ward"); manual.setStreet("Manual Street"); return manual;
    }
}
