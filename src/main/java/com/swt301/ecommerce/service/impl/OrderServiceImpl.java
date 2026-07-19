package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.config.properties.PaymentProperties;
import com.swt301.ecommerce.dto.request.CheckoutRequest;
import com.swt301.ecommerce.dto.request.ManualAddressRequest;
import com.swt301.ecommerce.dto.response.AdminOrderMetricsResponse;
import com.swt301.ecommerce.dto.response.CartResponse;
import com.swt301.ecommerce.dto.response.OrderItemResponse;
import com.swt301.ecommerce.dto.response.OrderResponse;
import com.swt301.ecommerce.dto.response.OrderSummaryResponse;
import com.swt301.ecommerce.dto.response.PagedResponse;
import com.swt301.ecommerce.dto.response.PaymentQrInfoResponse;
import com.swt301.ecommerce.dto.response.UploadedAsset;
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
import com.swt301.ecommerce.enums.PaymentStatus;
import com.swt301.ecommerce.enums.ProductStatus;
import com.swt301.ecommerce.exception.BadRequestException;
import com.swt301.ecommerce.exception.BusinessRuleException;
import com.swt301.ecommerce.exception.ConflictException;
import com.swt301.ecommerce.exception.ExternalServiceException;
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
import com.swt301.ecommerce.service.FileUploadService;
import com.swt301.ecommerce.service.OrderService;
import com.swt301.ecommerce.util.PaymentMethodUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int ORDER_CODE_RETRIES = 10;
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
    private final FileUploadService fileUploadService;
    private final PaymentProperties paymentProperties;
    private final ResourceLoader resourceLoader;

    @Override
    @Transactional(readOnly = true)
    public OrderSummaryResponse previewOrder(Integer userId, String voucherCode) {
        Cart cart = cartRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng không tồn tại"));
        List<CartItem> cartItems = new ArrayList<>(cart.getCartItems());
        requireCartItems(cartItems);
        Voucher voucher = findVoucherForPreview(voucherCode);
        Map<Integer, Product> products = loadCurrentProducts(cartItems, false);
        return buildSummary(cartItems, products, voucher);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(Integer userId, CheckoutRequest request, String idempotencyHeader) {
        String idempotencyKey = resolveIdempotencyKey(idempotencyHeader, request.getIdempotencyKey());
        Order existing = orderRepository.findByUser_UserIdAndIdempotencyKey(userId, idempotencyKey).orElse(null);
        if (existing != null) return mapSingleOrder(existing);

        Cart cart = cartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng không tồn tại"));
        // Re-check after the per-user cart lock so concurrent retries with the same key return one order.
        existing = orderRepository.findByUser_UserIdAndIdempotencyKey(userId, idempotencyKey).orElse(null);
        if (existing != null) return mapSingleOrder(existing);
        List<CartItem> cartItems = new ArrayList<>(cart.getCartItems());
        requireCartItems(cartItems);

        DeliverySnapshot delivery = resolveDeliverySnapshot(userId, request);
        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("Phương thức thanh toán không tồn tại"));
        String methodName = PaymentMethodUtils.canonicalName(paymentMethod.getMethodName());
        if (!PaymentMethodUtils.isSupported(methodName)) {
            throw new BusinessRuleException("Hệ thống chỉ hỗ trợ COD hoặc QR_CODE");
        }
        OrderStatus pendingStatus = orderStatusRepository.findByStatusName("PENDING")
                .orElseThrow(() -> new IllegalStateException("Hệ thống chưa cấu hình trạng thái PENDING"));

        Map<Integer, Product> lockedProducts = loadCurrentProducts(cartItems, true);
        Voucher appliedVoucher = findVoucherForCheckout(request.getVoucherCode());
        OrderSummaryResponse summary = buildSummary(cartItems, lockedProducts, appliedVoucher);
        if (appliedVoucher != null && appliedVoucher.getQuantity() != null) {
            appliedVoucher.setQuantity(appliedVoucher.getQuantity() - 1);
            voucherRepository.save(appliedVoucher);
        }

        Order order = Order.builder()
                .orderCode(generateUniqueOrderCode())
                .idempotencyKey(idempotencyKey)
                .user(cart.getUser())
                .address(delivery.sourceAddress())
                .receiverNameSnapshot(delivery.receiverName())
                .receiverPhoneSnapshot(delivery.receiverPhone())
                .provinceSnapshot(delivery.province())
                .districtSnapshot(delivery.district())
                .wardSnapshot(delivery.ward())
                .streetSnapshot(delivery.street())
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

        List<OrderItem> createdItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = lockedProducts.get(cartItem.getProduct().getProductId());
            BigDecimal currentPrice = product.getPrice();
            BigDecimal itemSubtotal = currentPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
            createdItems.add(orderItemRepository.save(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productNameSnapshot(product.getProductName())
                    .productImageSnapshot(product.getImage())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(currentPrice)
                    .subtotal(itemSubtotal)
                    .build()));
        }

        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .amount(summary.getTotal())
                .paymentStatus("QR_CODE".equals(methodName)
                        ? PaymentStatus.AWAITING_PAYMENT : PaymentStatus.PENDING)
                .build());

        cartItemRepository.deleteAll(cartItems);
        cart.getCartItems().clear();
        return mapToOrderResponse(order, payment, createdItems);
    }

    @Override
    @Transactional(readOnly = true)
    public void validatePaymentReceiptUpload(Integer orderId, Integer userId) {
        // Preflight runs before Cloudinary upload and deliberately avoids a write lock.
        // The state is checked again under a pessimistic lock when the asset is attached.
        validateReceiptUploadState(orderId, userId, false);
    }

    @Override
    @Transactional
    public void updatePaymentReceipt(Integer orderId, Integer userId, UploadedAsset asset) {
        Payment payment = validateReceiptUploadState(orderId, userId, true);
        String oldPublicId = payment.getReceiptPublicId();
        String oldUrl = payment.getQrImage();
        payment.setReceiptPublicId(asset.getPublicId());
        payment.setQrImage(null);
        payment.setPaymentStatus(PaymentStatus.PENDING_VERIFICATION);
        payment.setReceiptUploadedAt(LocalDateTime.now());
        payment.setVerifiedAt(null);
        payment.setPaymentDate(null);
        paymentRepository.save(payment);
        if (oldPublicId != null || oldUrl != null) {
            fileUploadService.deleteReceiptImage(oldPublicId, oldUrl);
        }
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
        return PaymentQrInfoResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .amount(payment.getAmount())
                .currency("VND")
                .paymentStatus(payment.getPaymentStatus().name())
                .transferNote(order.getOrderCode())
                .bankId(paymentProperties.getBankId())
                .bankName(paymentProperties.getBankName())
                .accountName(paymentProperties.getAccountName())
                .accountNumber(paymentProperties.getAccountNumber())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePaymentQrCode(Integer orderId, Integer userId) {
        PaymentQrInfoResponse info = getPaymentQrInfo(orderId, userId);
        return loadConfiguredQrBytes(info);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentQrInfoResponse getAdminPaymentQrInfo(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        if (!"QR_CODE".equals(PaymentMethodUtils.canonicalName(order.getPaymentMethod().getMethodName()))) {
            throw new BusinessRuleException("Đơn hàng này không sử dụng thanh toán QR");
        }
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));
        return PaymentQrInfoResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .amount(payment.getAmount())
                .currency("VND")
                .paymentStatus(payment.getPaymentStatus().name())
                .transferNote(order.getOrderCode())
                .bankId(paymentProperties.getBankId())
                .bankName(paymentProperties.getBankName())
                .accountName(paymentProperties.getAccountName())
                .accountNumber(paymentProperties.getAccountNumber())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateAdminPaymentQrCode(Integer orderId) {
        return loadConfiguredQrBytes(getAdminPaymentQrInfo(orderId));
    }

    private byte[] loadConfiguredQrBytes(PaymentQrInfoResponse info) {
        // Authorization and QR-payment validation already happened while building info.
        // Always return the exact project-owned MB Bank QR image configured in application.yml.
        Resource resource = resourceLoader.getResource(paymentProperties.getQrImageResource());
        if (!resource.exists() || !resource.isReadable()) {
            throw new ExternalServiceException("Không tìm thấy ảnh QR thanh toán đã cấu hình");
        }
        try (var input = resource.getInputStream()) {
            byte[] bytes = input.readAllBytes();
            if (bytes.length < 100) {
                throw new ExternalServiceException("Ảnh QR thanh toán đã cấu hình không hợp lệ");
            }
            return bytes;
        } catch (IOException ex) {
            throw new ExternalServiceException("Không thể đọc ảnh QR thanh toán đã cấu hình", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getCustomerReceipt(Integer orderId, Integer userId) {
        findOwnedOrder(orderId, userId);
        return getReceiptBytes(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getAdminReceipt(Integer orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng");
        }
        return getReceiptBytes(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Integer userId) {
        return getUserOrdersPage(userId, 0, MAX_PAGE_SIZE).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getUserOrdersPage(Integer userId, int page, int size) {
        Page<Order> orderPage = orderRepository.findByUser_UserId(userId, pageable(page, size));
        return pageResponse(orderPage, mapOrders(orderPage.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderMetricsResponse getAdminOrderMetrics() {
        return AdminOrderMetricsResponse.builder()
                .totalOrders(orderRepository.count())
                .paymentsPendingVerification(paymentRepository.countByPaymentStatus(PaymentStatus.PENDING_VERIFICATION))
                .paidRevenue(Objects.requireNonNullElse(paymentRepository.sumAmountByPaymentStatus(PaymentStatus.PAID), BigDecimal.ZERO))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllSystemOrders() {
        return getAllSystemOrdersPage(0, MAX_PAGE_SIZE).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllSystemOrdersPage(int page, int size) {
        Page<Order> orderPage = orderRepository.findAll(pageable(page, size));
        return pageResponse(orderPage, mapOrders(orderPage.getContent()));
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
        if (currentName.equals(newName)) return mapSingleOrder(order);
        List<String> allowed = ORDER_TRANSITIONS.get(currentName);
        if (allowed == null || !allowed.contains(newName)) {
            throw new BusinessRuleException("Không thể chuyển đơn hàng từ " + currentName + " sang " + newName);
        }

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
        String method = PaymentMethodUtils.canonicalName(order.getPaymentMethod().getMethodName());
        PaymentStatus paymentStatus = payment == null ? null : payment.getPaymentStatus();
        if ("PROCESSING".equals(newName) && "QR_CODE".equals(method) && paymentStatus != PaymentStatus.PAID) {
            throw new BusinessRuleException("Đơn thanh toán QR phải được xác minh PAID trước khi chuyển sang PROCESSING");
        }
        if ("CANCELLED".equals(newName)) {
            if (paymentStatus == PaymentStatus.PAID) {
                throw new BusinessRuleException("Không thể hủy đơn đã thanh toán nếu chưa có quy trình hoàn tiền");
            }
            restoreStockAndVoucher(order);
            if (payment != null) {
                payment.setPaymentStatus(PaymentStatus.CANCELLED);
                payment.setPaymentDate(null);
                payment.setVerifiedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
        }
        if ("DELIVERED".equals(newName) && "COD".equals(method) && payment != null) {
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setVerifiedAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }
        order.setStatus(newStatus);
        orderRepository.save(order);
        return mapSingleOrder(order);
    }

    @Override
    @Transactional
    public OrderResponse verifyPayment(Integer orderId, String paymentStatus) {
        PaymentStatus target;
        try {
            target = PaymentStatus.valueOf(normalizeStatus(paymentStatus));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Admin chỉ có thể xác minh thanh toán thành PAID hoặc FAILED");
        }
        if (target != PaymentStatus.PAID && target != PaymentStatus.FAILED) {
            throw new BadRequestException("Admin chỉ có thể xác minh thanh toán thành PAID hoặc FAILED");
        }
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));
        Order order = payment.getOrder();
        if (!"QR_CODE".equals(PaymentMethodUtils.canonicalName(payment.getPaymentMethod().getMethodName()))) {
            throw new BusinessRuleException("Chỉ thanh toán QR mới cần Admin xác minh");
        }
        if (!"PENDING".equals(normalizeStatus(order.getStatus().getStatusName()))) {
            throw new BusinessRuleException("Chỉ có thể xác minh payment khi order đang ở trạng thái PENDING");
        }
        if (payment.getPaymentStatus() != PaymentStatus.PENDING_VERIFICATION) {
            throw new BusinessRuleException("Payment phải ở trạng thái PENDING_VERIFICATION trước khi Admin xác minh");
        }
        if (!hasReceipt(payment)) {
            throw new BusinessRuleException("Khách hàng chưa tải biên lai thanh toán");
        }
        LocalDateTime now = LocalDateTime.now();
        payment.setPaymentStatus(target);
        payment.setVerifiedAt(now);
        payment.setPaymentDate(target == PaymentStatus.PAID ? now : null);
        paymentRepository.save(payment);
        return mapSingleOrder(order);
    }

    private Map<Integer, Product> loadCurrentProducts(List<CartItem> cartItems, boolean lock) {
        Map<Integer, Product> result = new HashMap<>();
        for (CartItem cartItem : cartItems.stream()
                .sorted(java.util.Comparator.comparing(item -> item.getProduct().getProductId()))
                .toList()) {
            Integer id = cartItem.getProduct().getProductId();
            Product product = lock
                    ? productRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"))
                    : productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BusinessRuleException("Sản phẩm " + product.getProductName() + " hiện không còn được bán");
            }
            if (product.getStock() < cartItem.getQuantity()) {
                throw new ConflictException("Sản phẩm " + product.getProductName() + " chỉ còn " + product.getStock() + " sản phẩm");
            }
            result.put(id, product);
        }
        return result;
    }

    private OrderSummaryResponse buildSummary(List<CartItem> cartItems, Map<Integer, Product> products, Voucher voucher) {
        BigDecimal subtotal = BigDecimal.ZERO;
        List<CartResponse.CartItemDto> itemDtos = new ArrayList<>();
        for (CartItem item : cartItems) {
            Product product = products.get(item.getProduct().getProductId());
            BigDecimal currentPrice = product.getPrice();
            BigDecimal itemSubtotal = currentPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);
            itemDtos.add(CartResponse.CartItemDto.builder()
                    .cartItemId(item.getCartItemId())
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .productImage(product.getImage())
                    .quantity(item.getQuantity())
                    .productStock(product.getStock())
                    .availableToAdd(Math.max(0, product.getStock() - item.getQuantity()))
                    .unitPrice(currentPrice)
                    .itemSubtotal(itemSubtotal)
                    .build());
        }
        BigDecimal discount = calculateDiscount(voucher, subtotal).min(subtotal);
        BigDecimal shippingFee = subtotal.compareTo(new BigDecimal("500000")) >= 0
                ? BigDecimal.ZERO : new BigDecimal("30000");
        return OrderSummaryResponse.builder()
                .items(itemDtos)
                .subtotal(subtotal)
                .discount(discount)
                .shippingFee(shippingFee)
                .total(subtotal.subtract(discount).add(shippingFee).max(BigDecimal.ZERO))
                .build();
    }

    private DeliverySnapshot resolveDeliverySnapshot(Integer userId, CheckoutRequest request) {
        boolean hasSaved = request.getAddressId() != null;
        boolean hasManual = request.getManualAddress() != null;
        if (hasSaved == hasManual) {
            throw new BadRequestException("Chọn đúng một địa chỉ đã lưu hoặc nhập địa chỉ thủ công");
        }
        if (hasSaved) {
            Address address = addressRepository.findByAddressIdAndUser_UserId(request.getAddressId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ giao hàng không hợp lệ"));
            return new DeliverySnapshot(address, address.getReceiverName(), address.getReceiverPhone(),
                    address.getProvince(), address.getDistrict(), address.getWard(), address.getStreet());
        }
        ManualAddressRequest manual = request.getManualAddress();
        List<String> values = java.util.Arrays.asList(manual.getReceiverName(), manual.getReceiverPhone(), manual.getProvince(),
                manual.getDistrict(), manual.getWard(), manual.getStreet());
        if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new BadRequestException("Vui lòng nhập đầy đủ địa chỉ giao hàng thủ công");
        }
        return new DeliverySnapshot(null, manual.getReceiverName().trim(), manual.getReceiverPhone().trim(),
                manual.getProvince().trim(), manual.getDistrict().trim(), manual.getWard().trim(), manual.getStreet().trim());
    }

    private Payment validateReceiptUploadState(Integer orderId, Integer userId, boolean lockPayment) {
        Order order = findOwnedOrder(orderId, userId);
        if (!"QR_CODE".equals(PaymentMethodUtils.canonicalName(order.getPaymentMethod().getMethodName()))) {
            throw new BusinessRuleException("Đơn COD không sử dụng biên lai QR");
        }
        if (!"PENDING".equals(normalizeStatus(order.getStatus().getStatusName()))) {
            throw new BusinessRuleException("Chỉ có thể tải biên lai khi order đang ở trạng thái PENDING");
        }
        Payment payment = (lockPayment
                ? paymentRepository.findByOrderIdForUpdate(orderId)
                : paymentRepository.findByOrder_OrderId(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));
        if (payment.getPaymentStatus() != PaymentStatus.AWAITING_PAYMENT
                && payment.getPaymentStatus() != PaymentStatus.FAILED) {
            throw new BusinessRuleException("Không thể tải biên lai khi payment đang ở trạng thái " + payment.getPaymentStatus());
        }
        return payment;
    }

    private byte[] getReceiptBytes(Integer orderId) {
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));
        if (!hasReceipt(payment)) throw new ResourceNotFoundException("Đơn hàng chưa có biên lai");
        return fileUploadService.downloadReceipt(payment.getReceiptPublicId(), payment.getQrImage());
    }

    private List<OrderResponse> mapOrders(List<Order> orders) {
        if (orders.isEmpty()) return List.of();
        List<Integer> ids = orders.stream().map(Order::getOrderId).toList();
        Map<Integer, Payment> payments = paymentRepository.findByOrder_OrderIdIn(ids).stream()
                .collect(Collectors.toMap(payment -> payment.getOrder().getOrderId(), Function.identity()));
        Map<Integer, List<OrderItem>> items = orderItemRepository.findByOrder_OrderIdIn(ids).stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getOrderId()));
        return orders.stream()
                .map(order -> mapToOrderResponse(order, payments.get(order.getOrderId()), items.getOrDefault(order.getOrderId(), List.of())))
                .toList();
    }

    private OrderResponse mapSingleOrder(Order order) {
        Payment payment = paymentRepository.findByOrder_OrderId(order.getOrderId()).orElse(null);
        List<OrderItem> items = orderItemRepository.findByOrder_OrderId(order.getOrderId());
        return mapToOrderResponse(order, payment, items);
    }

    private OrderResponse mapToOrderResponse(Order order, Payment payment, List<OrderItem> orderItems) {
        DeliverySnapshot delivery = snapshotFromOrder(order);
        List<OrderItemResponse> itemResponses = orderItems.stream().map(item -> OrderItemResponse.builder()
                .productId(item.getProduct() == null ? null : item.getProduct().getProductId())
                .productName(firstNonBlank(item.getProductNameSnapshot(), item.getProduct() == null ? null : item.getProduct().getProductName()))
                .productImage(firstNonBlank(item.getProductImageSnapshot(), item.getProduct() == null ? null : item.getProduct().getImage()))
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build()).toList();
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus().getStatusName())
                .allowedNextStatuses(allowedNextStatuses(order.getStatus().getStatusName()))
                .paymentMethod(PaymentMethodUtils.canonicalName(order.getPaymentMethod().getMethodName()))
                .paymentStatus(payment == null ? "N/A" : payment.getPaymentStatus().name())
                .voucherCode(order.getVoucher() == null ? null : order.getVoucher().getVoucherCode())
                .hasReceipt(payment != null && hasReceipt(payment))
                .receiptUploadedAt(payment == null ? null : payment.getReceiptUploadedAt())
                .paymentVerifiedAt(payment == null ? null : payment.getVerifiedAt())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .receiverName(delivery.receiverName())
                .receiverPhone(delivery.receiverPhone())
                .shippingAddress(String.join(", ", delivery.street(), delivery.ward(), delivery.district(), delivery.province()))
                .items(itemResponses)
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private DeliverySnapshot snapshotFromOrder(Order order) {
        if (order.getReceiverNameSnapshot() != null) {
            return new DeliverySnapshot(order.getAddress(), order.getReceiverNameSnapshot(), order.getReceiverPhoneSnapshot(),
                    order.getProvinceSnapshot(), order.getDistrictSnapshot(), order.getWardSnapshot(), order.getStreetSnapshot());
        }
        Address address = order.getAddress();
        if (address == null) return new DeliverySnapshot(null, "N/A", "N/A", "N/A", "N/A", "N/A", "N/A");
        return new DeliverySnapshot(address, address.getReceiverName(), address.getReceiverPhone(),
                address.getProvince(), address.getDistrict(), address.getWard(), address.getStreet());
    }

    private void restoreStockAndVoucher(Order order) {
        for (OrderItem item : orderItemRepository.findByOrder_OrderId(order.getOrderId())) {
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

    private Voucher findVoucherForPreview(String code) {
        if (code == null || code.isBlank()) return null;
        return voucherRepository.findByVoucherCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));
    }

    private Voucher findVoucherForCheckout(String code) {
        if (code == null || code.isBlank()) return null;
        return voucherRepository.findByVoucherCodeForUpdate(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
        if (voucher == null) return BigDecimal.ZERO;
        if (!"ACTIVE".equalsIgnoreCase(voucher.getStatus())
                || (voucher.getQuantity() != null && voucher.getQuantity() <= 0)
                || (voucher.getExpiredDate() != null && voucher.getExpiredDate().isBefore(LocalDateTime.now()))
                || (voucher.getMinOrder() != null && subtotal.compareTo(voucher.getMinOrder()) < 0)) {
            throw new BusinessRuleException("Mã giảm giá không hợp lệ hoặc không đủ điều kiện");
        }
        BigDecimal value = voucher.getDiscountValue() == null ? BigDecimal.ZERO : voucher.getDiscountValue();
        if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) return value;
        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            return subtotal.multiply(value).divide(new BigDecimal("100"));
        }
        throw new BusinessRuleException("Loại voucher không hợp lệ");
    }

    private String generateUniqueOrderCode() {
        for (int attempt = 0; attempt < ORDER_CODE_RETRIES; attempt++) {
            int numericPart = ThreadLocalRandom.current().nextInt(1_000_000);
            String code = String.format(Locale.ROOT, "ORD-%06d", numericPart);
            if (!orderRepository.existsByOrderCode(code)) return code;
        }
        throw new ConflictException("Không thể sinh mã đơn hàng duy nhất. Vui lòng thử lại");
    }

    private String resolveIdempotencyKey(String header, String body) {
        String key = firstNonBlank(header, body);
        if (key == null || key.isBlank()) throw new BadRequestException("Idempotency-Key is required for checkout");
        if (key.length() > 100) throw new BadRequestException("Idempotency-Key is too long");
        return key.trim();
    }

    private void requireCartItems(Collection<CartItem> items) {
        if (items.isEmpty()) throw new BusinessRuleException("Checkout require at least one cart item");
    }

    private Order findOwnedOrder(Integer orderId, Integer userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        if (!order.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên đơn hàng này");
        }
        return order;
    }

    private boolean hasReceipt(Payment payment) {
        return (payment.getReceiptPublicId() != null && !payment.getReceiptPublicId().isBlank())
                || (payment.getQrImage() != null && !payment.getQrImage().isBlank());
    }

    private List<String> allowedNextStatuses(String currentStatus) {
        return ORDER_TRANSITIONS.getOrDefault(normalizeStatus(currentStatus), List.of());
    }

    private String normalizeStatus(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private PageRequest pageable(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(MAX_PAGE_SIZE, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PagedResponse<OrderResponse> pageResponse(Page<Order> source, List<OrderResponse> content) {
        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .page(source.getNumber())
                .size(source.getSize())
                .totalElements(source.getTotalElements())
                .totalPages(source.getTotalPages())
                .first(source.isFirst())
                .last(source.isLast())
                .build();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private record DeliverySnapshot(
            Address sourceAddress,
            String receiverName,
            String receiverPhone,
            String province,
            String district,
            String ward,
            String street) {
    }
}
