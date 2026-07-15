package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.CheckoutRequest;
import com.swt301.ecommerce.dto.response.CartResponse;
import com.swt301.ecommerce.dto.response.OrderResponse;
import com.swt301.ecommerce.dto.response.OrderSummaryResponse;
import com.swt301.ecommerce.dto.response.PaymentQrInfoResponse;
import com.swt301.ecommerce.entity.Address;
import com.swt301.ecommerce.entity.Cart;
import com.swt301.ecommerce.entity.CartItem;
import com.swt301.ecommerce.entity.Order;
import com.swt301.ecommerce.entity.OrderItem;
import com.swt301.ecommerce.entity.OrderStatus;
import com.swt301.ecommerce.entity.Payment;
import com.swt301.ecommerce.entity.PaymentMethod;
import com.swt301.ecommerce.entity.Product;
import com.swt301.ecommerce.entity.Voucher;
import com.swt301.ecommerce.exception.BadRequestException;
import com.swt301.ecommerce.exception.BusinessRuleException;
import com.swt301.ecommerce.exception.ConflictException;
import com.swt301.ecommerce.exception.ResourceNotFoundException;
import com.swt301.ecommerce.repository.AddressRepository;
import com.swt301.ecommerce.repository.CartItemRepository;
import com.swt301.ecommerce.repository.CartRepository;
import com.swt301.ecommerce.repository.OrderItemRepository;
import com.swt301.ecommerce.repository.OrderRepository;
import com.swt301.ecommerce.repository.OrderStatusRepository;
import com.swt301.ecommerce.repository.PaymentMethodRepository;
import com.swt301.ecommerce.repository.PaymentRepository;
import com.swt301.ecommerce.repository.ProductRepository;
import com.swt301.ecommerce.repository.VoucherRepository;
import com.swt301.ecommerce.service.OrderService;
import com.swt301.ecommerce.util.PaymentMethodUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Map<String, List<String>> ORDER_TRANSITIONS = Map.of(
            "PENDING", List.of("PROCESSING", "CANCELLED"),
            "PROCESSING", List.of("SHIPPED", "CANCELLED"),
            "SHIPPED", List.of("DELIVERED"),
            "DELIVERED", List.of(),
            "CANCELLED", List.of()
    );

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final VoucherRepository voucherRepository;
    private final AddressRepository addressRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public OrderSummaryResponse previewOrder(Integer userId, String voucherCode) {
        Cart cart = cartRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng không tồn tại"));
        List<CartItem> cartItems = new ArrayList<>(cart.getCartItems());
        if (cartItems.isEmpty()) {
            throw new BusinessRuleException("Checkout require at least one cart item");
        }

        Voucher voucher = findVoucherForPreview(voucherCode);
        Map<Integer, Product> products = cartItems.stream().collect(Collectors.toMap(
                item -> item.getProduct().getProductId(),
                CartItem::getProduct,
                (first, second) -> first
        ));
        return buildSummary(cartItems, products, voucher);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(Integer userId, CheckoutRequest request) {
        Cart cart = cartRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng không tồn tại"));
        List<CartItem> cartItems = new ArrayList<>(cart.getCartItems());
        if (cartItems.isEmpty()) {
            throw new BusinessRuleException("Checkout require at least one cart item");
        }

        Address address = addressRepository.findByAddressIdAndUser_UserId(request.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ giao hàng không hợp lệ"));
        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("Phương thức thanh toán không tồn tại"));
        String methodName = PaymentMethodUtils.canonicalName(paymentMethod.getMethodName());
        if (!PaymentMethodUtils.isSupported(methodName)) {
            throw new BusinessRuleException("Hệ thống chỉ hỗ trợ COD hoặc QR_CODE");
        }
        OrderStatus pendingStatus = orderStatusRepository.findByStatusName("PENDING")
                .orElseThrow(() -> new IllegalStateException("Hệ thống chưa cấu hình trạng thái PENDING"));

        Map<Integer, Product> lockedProducts = new HashMap<>();
        for (CartItem cartItem : cartItems) {
            Product lockedProduct = productRepository.findByIdForUpdate(cartItem.getProduct().getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
            if (lockedProduct.getStock() < cartItem.getQuantity()) {
                throw new ConflictException("Sản phẩm " + lockedProduct.getProductName()
                        + " chỉ còn " + lockedProduct.getStock() + " sản phẩm");
            }
            lockedProducts.put(lockedProduct.getProductId(), lockedProduct);
        }

        Voucher appliedVoucher = findVoucherForCheckout(request.getVoucherCode());
        OrderSummaryResponse summary = buildSummary(cartItems, lockedProducts, appliedVoucher);
        if (appliedVoucher != null && appliedVoucher.getQuantity() != null) {
            appliedVoucher.setQuantity(appliedVoucher.getQuantity() - 1);
            voucherRepository.save(appliedVoucher);
        }

        String orderCode = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        Order order = Order.builder()
                .orderCode(orderCode)
                .user(cart.getUser())
                .address(address)
                .voucher(appliedVoucher)
                .paymentMethod(paymentMethod)
                .status(pendingStatus)
                .subtotal(summary.getSubtotal())
                .discount(summary.getDiscount())
                .shippingFee(summary.getShippingFee())
                .total(summary.getTotal())
                .note(request.getNote())
                .build();
        order = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            Product product = lockedProducts.get(cartItem.getProduct().getProductId());
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            BigDecimal itemSubtotal = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            orderItemRepository.save(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .subtotal(itemSubtotal)
                    .build());
        }

        String paymentStatus = "QR_CODE".equals(methodName) ? "AWAITING_PAYMENT" : "PENDING";
        paymentRepository.save(Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .amount(summary.getTotal())
                .paymentStatus(paymentStatus)
                .build());

        cartItemRepository.deleteAll(cartItems);
        cart.getCartItems().clear();

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public void validatePaymentReceiptUpload(Integer orderId, Integer userId) {
        validateReceiptUploadState(orderId, userId);
    }

    @Override
    @Transactional
    public void updatePaymentQrImage(Integer orderId, Integer userId, String qrImageUrl) {
        Payment payment = validateReceiptUploadState(orderId, userId);
        payment.setQrImage(qrImageUrl);
        payment.setPaymentStatus("PENDING_VERIFICATION");
        payment.setPaymentDate(null);
        paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentQrInfoResponse getPaymentQrInfo(Integer orderId, Integer userId) {
        Order order = findOwnedOrder(orderId, userId);
        if (!"QR_CODE".equals(PaymentMethodUtils.canonicalName(order.getPaymentMethod().getMethodName()))) {
            throw new BusinessRuleException("Đơn hàng này không sử dụng thanh toán QR");
        }
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));
        String qrContent = "MB | ACCOUNT=0846511618 | NAME=CAO LE ANH KHOA"
                + " | TRANSFER_NOTE=" + order.getOrderCode()
                + " | AMOUNT=" + payment.getAmount().toPlainString() + " VND";
        return PaymentQrInfoResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .amount(payment.getAmount())
                .currency("VND")
                .paymentStatus(payment.getPaymentStatus())
                .qrContent(qrContent)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePaymentQrCode(Integer orderId, Integer userId) {
        // Keep the existing ownership and QR-payment validation before returning the configured image.
        getPaymentQrInfo(orderId, userId);
        try (InputStream input = OrderServiceImpl.class.getResourceAsStream(
                "/static/payment/vietqr-payment.png")) {
            if (input == null) {
                throw new IllegalStateException("Không tìm thấy ảnh QR thanh toán đã cấu hình");
            }
            return input.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể đọc ảnh QR thanh toán đã cấu hình", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Integer userId) {
        return orderRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllSystemOrders() {
        return orderRepository.findAll(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Integer orderId, Integer statusId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        OrderStatus newStatus = orderStatusRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("Trạng thái không hợp lệ"));

        String currentName = normalizeStatus(order.getStatus().getStatusName());
        String newName = normalizeStatus(newStatus.getStatusName());
        if (currentName.equals(newName)) {
            return mapToOrderResponse(order);
        }

        List<String> allowed = ORDER_TRANSITIONS.get(currentName);
        if (allowed == null) {
            throw new BusinessRuleException("Trạng thái hiện tại " + currentName + " chưa được cấu hình trong quy tắc chuyển trạng thái");
        }
        if (!allowed.contains(newName)) {
            throw new BusinessRuleException("Không thể chuyển đơn hàng từ " + currentName + " sang " + newName);
        }

        Payment payment = paymentRepository.findByOrder_OrderId(orderId).orElse(null);
        String paymentMethodName = PaymentMethodUtils.canonicalName(order.getPaymentMethod().getMethodName());
        String paymentStatus = payment == null ? "" : normalizeStatus(payment.getPaymentStatus());

        if ("PROCESSING".equals(newName) && "QR_CODE".equals(paymentMethodName) && !"PAID".equals(paymentStatus)) {
            throw new BusinessRuleException("Đơn thanh toán QR phải được xác minh PAID trước khi chuyển sang PROCESSING");
        }

        if ("CANCELLED".equals(newName)) {
            if ("PAID".equals(paymentStatus)) {
                throw new BusinessRuleException("Không thể hủy đơn đã thanh toán nếu chưa có quy trình hoàn tiền");
            }
            restoreStockAndVoucher(order);
            if (payment != null) {
                payment.setPaymentStatus("CANCELLED");
                payment.setPaymentDate(null);
                paymentRepository.save(payment);
            }
        }

        if ("DELIVERED".equals(newName) && "COD".equals(paymentMethodName) && payment != null) {
            payment.setPaymentStatus("PAID");
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        order.setStatus(newStatus);
        return mapToOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse verifyPayment(Integer orderId, String paymentStatus) {
        String normalizedStatus = normalizeStatus(paymentStatus);
        if (!List.of("PAID", "FAILED").contains(normalizedStatus)) {
            throw new BadRequestException("Admin chỉ có thể xác minh thanh toán thành PAID hoặc FAILED");
        }

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán của đơn hàng này"));
        Order order = payment.getOrder();
        if (!"QR_CODE".equals(PaymentMethodUtils.canonicalName(payment.getPaymentMethod().getMethodName()))) {
            throw new BusinessRuleException("Chỉ thanh toán QR mới cần Admin xác minh");
        }
        if (!"PENDING".equals(normalizeStatus(order.getStatus().getStatusName()))) {
            throw new BusinessRuleException("Chỉ có thể xác minh payment khi order đang ở trạng thái PENDING");
        }
        if (!"PENDING_VERIFICATION".equals(normalizeStatus(payment.getPaymentStatus()))) {
            throw new BusinessRuleException("Payment phải ở trạng thái PENDING_VERIFICATION trước khi Admin xác minh");
        }
        if (payment.getQrImage() == null || payment.getQrImage().isBlank()) {
            throw new BusinessRuleException("Khách hàng chưa tải biên lai thanh toán");
        }

        payment.setPaymentStatus(normalizedStatus);
        payment.setPaymentDate("PAID".equals(normalizedStatus) ? LocalDateTime.now() : null);
        paymentRepository.save(payment);
        return mapToOrderResponse(order);
    }

    private OrderSummaryResponse buildSummary(
            List<CartItem> cartItems,
            Map<Integer, Product> products,
            Voucher voucher) {
        BigDecimal subtotal = BigDecimal.ZERO;
        List<CartResponse.CartItemDto> itemDtos = new ArrayList<>();

        for (CartItem item : cartItems) {
            Product product = products.get(item.getProduct().getProductId());
            if (product == null) {
                throw new ResourceNotFoundException("Sản phẩm không tồn tại");
            }
            if (item.getQuantity() > product.getStock()) {
                throw new ConflictException("Sản phẩm " + product.getProductName()
                        + " chỉ còn " + product.getStock() + " sản phẩm");
            }
            BigDecimal itemSubtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);
            itemDtos.add(CartResponse.CartItemDto.builder()
                    .cartItemId(item.getCartItemId())
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .productImage(product.getImage())
                    .quantity(item.getQuantity())
                    .productStock(product.getStock())
                    .availableToAdd(Math.max(0, product.getStock() - item.getQuantity()))
                    .unitPrice(item.getUnitPrice())
                    .itemSubtotal(itemSubtotal)
                    .build());
        }

        BigDecimal discount = calculateDiscount(voucher, subtotal).min(subtotal);
        BigDecimal shippingFee = subtotal.compareTo(new BigDecimal("500000")) >= 0
                ? BigDecimal.ZERO : new BigDecimal("30000");
        BigDecimal total = subtotal.subtract(discount).add(shippingFee).max(BigDecimal.ZERO);

        return OrderSummaryResponse.builder()
                .items(itemDtos)
                .subtotal(subtotal)
                .discount(discount)
                .shippingFee(shippingFee)
                .total(total)
                .build();
    }

    private Voucher findVoucherForPreview(String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return null;
        }
        return voucherRepository.findByVoucherCode(voucherCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));
    }

    private Voucher findVoucherForCheckout(String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return null;
        }
        return voucherRepository.findByVoucherCodeForUpdate(voucherCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
        if (voucher == null) {
            return BigDecimal.ZERO;
        }
        if (!"ACTIVE".equalsIgnoreCase(voucher.getStatus())
                || (voucher.getQuantity() != null && voucher.getQuantity() <= 0)
                || (voucher.getExpiredDate() != null && voucher.getExpiredDate().isBefore(LocalDateTime.now()))
                || (voucher.getMinOrder() != null && subtotal.compareTo(voucher.getMinOrder()) < 0)) {
            throw new BusinessRuleException("Mã giảm giá không hợp lệ hoặc không đủ điều kiện");
        }
        if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) {
            return voucher.getDiscountValue() == null ? BigDecimal.ZERO : voucher.getDiscountValue();
        }
        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            BigDecimal value = voucher.getDiscountValue() == null ? BigDecimal.ZERO : voucher.getDiscountValue();
            return subtotal.multiply(value).divide(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }

    private Payment validateReceiptUploadState(Integer orderId, Integer userId) {
        Order order = findOwnedOrder(orderId, userId);
        if (!"QR_CODE".equals(PaymentMethodUtils.canonicalName(order.getPaymentMethod().getMethodName()))) {
            throw new BusinessRuleException("Đơn COD không sử dụng biên lai QR");
        }
        if (!"PENDING".equals(normalizeStatus(order.getStatus().getStatusName()))) {
            throw new BusinessRuleException("Chỉ có thể tải biên lai khi order đang ở trạng thái PENDING");
        }
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));
        String currentPaymentStatus = normalizeStatus(payment.getPaymentStatus());
        if (!List.of("AWAITING_PAYMENT", "FAILED").contains(currentPaymentStatus)) {
            throw new BusinessRuleException("Không thể tải biên lai khi payment đang ở trạng thái " + currentPaymentStatus);
        }
        return payment;
    }

    private void restoreStockAndVoucher(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrder_OrderId(order.getOrderId());
        for (OrderItem item : orderItems) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm để hoàn tồn kho"));
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        if (order.getVoucher() != null && order.getVoucher().getQuantity() != null) {
            Voucher voucher = voucherRepository.findByIdForUpdate(order.getVoucher().getVoucherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher để hoàn lượt sử dụng"));
            voucher.setQuantity(voucher.getQuantity() + 1);
            voucherRepository.save(voucher);
        }
    }

    private Order findOwnedOrder(Integer orderId, Integer userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        if (!order.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên đơn hàng này");
        }
        return order;
    }

    private List<String> allowedNextStatuses(String currentStatus) {
        return ORDER_TRANSITIONS.getOrDefault(normalizeStatus(currentStatus), List.of());
    }

    private String normalizeStatus(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        Payment payment = paymentRepository.findByOrder_OrderId(order.getOrderId()).orElse(null);
        String paymentStatus = payment != null ? payment.getPaymentStatus() : "N/A";
        String receiptUrl = payment != null ? payment.getQrImage() : null;
        Address address = order.getAddress();
        String fullAddress = address.getStreet() + ", " + address.getWard()
                + ", " + address.getDistrict() + ", " + address.getProvince();
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus().getStatusName())
                .allowedNextStatuses(allowedNextStatuses(order.getStatus().getStatusName()))
                .paymentMethod(PaymentMethodUtils.canonicalName(order.getPaymentMethod().getMethodName()))
                .paymentStatus(paymentStatus)
                .voucherCode(order.getVoucher() != null ? order.getVoucher().getVoucherCode() : null)
                .receiptUrl(receiptUrl)
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .shippingAddress(fullAddress)
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
