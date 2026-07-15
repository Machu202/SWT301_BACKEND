package com.swt301.ecommerce.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Set<String> SUPPORTED_PAYMENT_METHODS = Set.of("COD", "QR_CODE");
    private static final Set<String> ALLOWED_PAYMENT_STATUSES = Set.of("PAID", "FAILED", "PENDING_VERIFICATION");

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
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Không có sản phẩm nào trong giỏ hàng để thanh toán");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<CartResponse.CartItemDto> itemDtos = cart.getCartItems().stream().map(item -> {
            Product product = item.getProduct();
            if (item.getQuantity() > product.getStock()) {
                throw new RuntimeException("Sản phẩm " + product.getProductName() + " chỉ còn " + product.getStock() + " sản phẩm");
            }
            BigDecimal itemSub = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return CartResponse.CartItemDto.builder()
                    .cartItemId(item.getCartItemId())
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .productImage(product.getImage())
                    .quantity(item.getQuantity())
                    .productStock(product.getStock())
                    .availableToAdd(Math.max(0, product.getStock() - item.getQuantity()))
                    .unitPrice(item.getUnitPrice())
                    .itemSubtotal(itemSub)
                    .build();
        }).collect(Collectors.toList());

        for (CartResponse.CartItemDto dto : itemDtos) {
            subtotal = subtotal.add(dto.getItemSubtotal());
        }

        BigDecimal discount = calculateDiscount(voucherCode, subtotal).min(subtotal);
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

    private BigDecimal calculateDiscount(String voucherCode, BigDecimal subtotal) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return BigDecimal.ZERO;
        }
        Voucher voucher = voucherRepository.findByVoucherCode(voucherCode.trim())
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));
        if (!"ACTIVE".equalsIgnoreCase(voucher.getStatus())
                || (voucher.getQuantity() != null && voucher.getQuantity() <= 0)
                || (voucher.getExpiredDate() != null && voucher.getExpiredDate().isBefore(LocalDateTime.now()))
                || (voucher.getMinOrder() != null && subtotal.compareTo(voucher.getMinOrder()) < 0)) {
            throw new RuntimeException("Mã giảm giá không hợp lệ hoặc không đủ điều kiện");
        }
        if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) {
            return voucher.getDiscountValue();
        }
        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            return subtotal.multiply(voucher.getDiscountValue()).divide(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(Integer userId, CheckoutRequest request) {
        Cart cart = cartRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        List<CartItem> cartItems = new ArrayList<>(cart.getCartItems());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống");
        }

        Address address = addressRepository.findByAddressIdAndUser_UserId(request.getAddressId(), userId)
                .orElseThrow(() -> new RuntimeException("Địa chỉ giao hàng không hợp lệ"));
        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new RuntimeException("Phương thức thanh toán không hợp lệ"));
        String methodName = paymentMethod.getMethodName().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_PAYMENT_METHODS.contains(methodName)) {
            throw new RuntimeException("Hệ thống chỉ hỗ trợ COD hoặc QR_CODE");
        }
        OrderStatus pendingStatus = orderStatusRepository.findByStatusName("PENDING")
                .orElseThrow(() -> new RuntimeException("Hệ thống chưa cấu hình trạng thái PENDING"));

        Map<Integer, Product> lockedProducts = new HashMap<>();
        for (CartItem cartItem : cartItems) {
            Product lockedProduct = productRepository.findByIdForUpdate(cartItem.getProduct().getProductId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
            if (lockedProduct.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + lockedProduct.getProductName()
                        + " chỉ còn " + lockedProduct.getStock() + " sản phẩm");
            }
            lockedProducts.put(lockedProduct.getProductId(), lockedProduct);
        }

        OrderSummaryResponse summary = previewOrder(userId, request.getVoucherCode());
        Voucher appliedVoucher = null;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            appliedVoucher = voucherRepository.findByVoucherCode(request.getVoucherCode().trim())
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));
            if (appliedVoucher.getQuantity() != null) {
                appliedVoucher.setQuantity(appliedVoucher.getQuantity() - 1);
                voucherRepository.save(appliedVoucher);
            }
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
    @Transactional
    public void updatePaymentQrImage(Integer orderId, Integer userId, String qrImageUrl) {
        Order order = findOwnedOrder(orderId, userId);
        if (!"QR_CODE".equalsIgnoreCase(order.getPaymentMethod().getMethodName())) {
            throw new RuntimeException("Đơn COD không sử dụng biên lai QR");
        }
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thanh toán"));
        if ("PAID".equalsIgnoreCase(payment.getPaymentStatus())) {
            throw new RuntimeException("Đơn hàng đã thanh toán thành công");
        }
        payment.setQrImage(qrImageUrl);
        payment.setPaymentStatus("PENDING_VERIFICATION");
        paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentQrInfoResponse getPaymentQrInfo(Integer orderId, Integer userId) {
        Order order = findOwnedOrder(orderId, userId);
        if (!"QR_CODE".equalsIgnoreCase(order.getPaymentMethod().getMethodName())) {
            throw new RuntimeException("Đơn hàng này không sử dụng thanh toán QR");
        }
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thanh toán"));
        String qrContent = "SWT301_PAYMENT|ORDER=" + order.getOrderCode()
                + "|AMOUNT=" + payment.getAmount().toPlainString() + "|CURRENCY=VND";
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
        PaymentQrInfoResponse info = getPaymentQrInfo(orderId, userId);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BitMatrix matrix = new QRCodeWriter().encode(info.getQrContent(), BarcodeFormat.QR_CODE, 360, 360);
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new RuntimeException("Không thể tạo mã QR thanh toán", ex);
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        OrderStatus newStatus = orderStatusRepository.findById(statusId)
                .orElseThrow(() -> new RuntimeException("Trạng thái không hợp lệ"));
        order.setStatus(newStatus);
        return mapToOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse verifyPayment(Integer orderId, String paymentStatus) {
        String normalizedStatus = paymentStatus == null ? "" : paymentStatus.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PAYMENT_STATUSES.contains(normalizedStatus)) {
            throw new RuntimeException("Trạng thái thanh toán chỉ được là PAID, FAILED hoặc PENDING_VERIFICATION");
        }
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thanh toán của đơn hàng này"));
        if (!"QR_CODE".equalsIgnoreCase(payment.getPaymentMethod().getMethodName())) {
            throw new RuntimeException("Chỉ thanh toán QR mới cần Admin xác minh");
        }
        if ("PAID".equals(normalizedStatus) && (payment.getQrImage() == null || payment.getQrImage().isBlank())) {
            throw new RuntimeException("Khách hàng chưa tải biên lai thanh toán");
        }
        payment.setPaymentStatus(normalizedStatus);
        payment.setPaymentDate("PAID".equals(normalizedStatus) ? LocalDateTime.now() : null);
        paymentRepository.save(payment);
        return mapToOrderResponse(payment.getOrder());
    }

    private Order findOwnedOrder(Integer orderId, Integer userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        if (!order.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thao tác trên đơn hàng này");
        }
        return order;
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
                .paymentMethod(order.getPaymentMethod().getMethodName())
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
