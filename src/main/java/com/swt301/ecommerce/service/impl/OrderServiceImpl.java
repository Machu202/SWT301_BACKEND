// Vị trí: src/main/java/com/swt301/ecommerce/service/impl/OrderServiceImpl.java
package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.CheckoutRequest;
import com.swt301.ecommerce.dto.response.CartResponse;
import com.swt301.ecommerce.dto.response.OrderResponse;
import com.swt301.ecommerce.dto.response.OrderSummaryResponse;
import com.swt301.ecommerce.entity.*;
import com.swt301.ecommerce.repository.*;
import com.swt301.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

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

    // ... (Giữ nguyên hàm previewOrder và calculateDiscount từ bước trước ở đây) ...
    @Override
    public OrderSummaryResponse previewOrder(Integer userId, String voucherCode) {
        // ... (Code của FE-11 đã viết) ...
        Cart cart = cartRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        
        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Không có sản phẩm nào trong giỏ hàng để thanh toán");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<CartResponse.CartItemDto> itemDtos = cart.getCartItems().stream().map(item -> {
            BigDecimal itemSub = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
            return CartResponse.CartItemDto.builder()
                    .cartItemId(item.getCartItemId())
                    .productId(item.getProduct().getProductId())
                    .productName(item.getProduct().getProductName())
                    .productImage(item.getProduct().getImage())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .itemSubtotal(itemSub)
                    .build();
        }).collect(Collectors.toList());

        for (CartResponse.CartItemDto dto : itemDtos) {
            subtotal = subtotal.add(dto.getItemSubtotal());
        }

        BigDecimal discount = calculateDiscount(voucherCode, subtotal);
        BigDecimal shippingFee = subtotal.compareTo(new BigDecimal("500000")) >= 0 
                ? BigDecimal.ZERO : new BigDecimal("30000");
        BigDecimal total = subtotal.subtract(discount).add(shippingFee);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        return OrderSummaryResponse.builder()
                .items(itemDtos).subtotal(subtotal).discount(discount).shippingFee(shippingFee).total(total).build();
    }

    private BigDecimal calculateDiscount(String voucherCode, BigDecimal subtotal) {
        // ... (Code của FE-11 đã viết) ...
        if (voucherCode == null || voucherCode.isBlank()) return BigDecimal.ZERO;
        Voucher voucher = voucherRepository.findByVoucherCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));
        if (!"ACTIVE".equalsIgnoreCase(voucher.getStatus()) || 
            (voucher.getQuantity() != null && voucher.getQuantity() <= 0) ||
            (voucher.getExpiredDate() != null && voucher.getExpiredDate().isBefore(LocalDateTime.now())) ||
            (voucher.getMinOrder() != null && subtotal.compareTo(voucher.getMinOrder()) < 0)) {
            throw new RuntimeException("Mã giảm giá không hợp lệ hoặc không đủ điều kiện");
        }
        if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) return voucher.getDiscountValue();
        else if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) 
            return subtotal.multiply(voucher.getDiscountValue()).divide(new BigDecimal("100"));
        return BigDecimal.ZERO;
    }


    // ==========================================
    // FE-13 & FE-17: CHỐT ĐƠN VÀ LƯU DB
    // ==========================================
    @Override
    @Transactional // Nếu có bất kỳ Exception nào, toàn bộ dữ liệu (trừ tồn kho, lưu đơn) sẽ tự động quay xe!
    public OrderResponse createOrder(Integer userId, CheckoutRequest request) {
        
        // 1. Validate nền tảng
        Cart cart = cartRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        if (cart.getCartItems().isEmpty()) throw new RuntimeException("Giỏ hàng đang trống");

        Address address = addressRepository.findByAddressIdAndUser_UserId(request.getAddressId(), userId)
                .orElseThrow(() -> new RuntimeException("Địa chỉ giao hàng không hợp lệ"));
        
        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new RuntimeException("Phương thức thanh toán không hợp lệ"));
        
        OrderStatus pendingStatus = orderStatusRepository.findByStatusName("PENDING")
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Chưa cấu hình trạng thái PENDING"));

        // 2. Chạy lại thuật toán tính tiền để chốt số (Bảo mật tuyệt đối, không tin giá trị từ FE gửi lên)
        OrderSummaryResponse summary = previewOrder(userId, request.getVoucherCode());
        
        // Trừ lượt sử dụng Voucher
        Voucher appliedVoucher = null;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            appliedVoucher = voucherRepository.findByVoucherCode(request.getVoucherCode()).get();
            appliedVoucher.setQuantity(appliedVoucher.getQuantity() - 1);
            voucherRepository.save(appliedVoucher);
        }

        // 3. Khởi tạo và Lưu bảng Order
        String orderCode = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = Order.builder()
                .orderCode(orderCode).user(cart.getUser()).address(address).voucher(appliedVoucher)
                .paymentMethod(paymentMethod).status(pendingStatus)
                .subtotal(summary.getSubtotal()).discount(summary.getDiscount())
                .shippingFee(summary.getShippingFee()).total(summary.getTotal())
                .note(request.getNote())
                .build();
        order = orderRepository.save(order);

        // 4. Lưu bảng OrderItem và Trừ Stock
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + product.getProductName() + " đã hết hoặc không đủ số lượng");
            }
            
            // Trừ số lượng tồn kho
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            BigDecimal itemSubtotal = cartItem.getUnitPrice().multiply(new BigDecimal(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order).product(product)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .subtotal(itemSubtotal) // THÊM DÒNG NÀY
                    .build();
            orderItemRepository.save(orderItem);
        }

        // 5. FE-13: Rẽ nhánh luồng Thanh Toán
        String paymentStatus = "PENDING";
        if ("QR_CODE".equalsIgnoreCase(paymentMethod.getMethodName())) {
            paymentStatus = "AWAITING_PAYMENT"; // Nếu là QR thì chờ khách tải bill lên
        }

        Payment payment = Payment.builder()
                .order(order).paymentMethod(paymentMethod)
                .amount(summary.getTotal()).paymentStatus(paymentStatus)
                .build();
        paymentRepository.save(payment);

        // 6. Dọn dẹp giỏ hàng
        cartItemRepository.deleteAll(cart.getCartItems()); // Xóa trong DB
        cart.getCartItems().clear();

        // 7. Trả kết quả
        String fullAddress = address.getStreet() + ", " + address.getWard() + ", " + address.getDistrict() + ", " + address.getProvince();
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .status(pendingStatus.getStatusName())
                .paymentMethod(paymentMethod.getMethodName())
                .paymentStatus(paymentStatus)
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
    // ==========================================
    // FE-15: CẬP NHẬT ẢNH QR VÀO DB
    // ==========================================
    @Override
    @Transactional
    public void updatePaymentQrImage(Integer orderId, Integer userId, String qrImageUrl) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
                
        // Chỉ chủ đơn hàng mới được upload ảnh
        if (!order.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thao tác trên đơn hàng này");
        }

        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thanh toán"));

        payment.setQrImage(qrImageUrl);
        payment.setPaymentStatus("PENDING_VERIFICATION"); // Đổi trạng thái chờ Admin duyệt bill
        paymentRepository.save(payment);
    }

    // ==========================================
    // FE-19: LẤY DANH SÁCH LỊCH SỬ ĐƠN HÀNG
    // ==========================================
    @Override
    public List<OrderResponse> getUserOrders(Integer userId) {
        List<Order> orders = orderRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
        
        return orders.stream().map(order -> {
            String paymentStatus = "N/A";
            var paymentOpt = paymentRepository.findByOrder_OrderId(order.getOrderId());
            if (paymentOpt.isPresent()) {
                paymentStatus = paymentOpt.get().getPaymentStatus();
            }

            Address address = order.getAddress();
            String fullAddress = address.getStreet() + ", " + address.getWard() + 
                                 ", " + address.getDistrict() + ", " + address.getProvince();

            return OrderResponse.builder()
                    .orderId(order.getOrderId())
                    .orderCode(order.getOrderCode())
                    .status(order.getStatus().getStatusName())
                    .paymentMethod(order.getPaymentMethod().getMethodName())
                    .paymentStatus(paymentStatus)
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
        }).collect(Collectors.toList());
    }
    // ==========================================
    // LUỒNG DÀNH CHO ADMIN (QUẢN LÝ ĐƠN HÀNG)
    // ==========================================
    @Override
    public List<OrderResponse> getAllSystemOrders() {
        // Lấy tất cả đơn hàng, xếp mới nhất lên đầu
        List<Order> orders = orderRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Integer orderId, Integer statusId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
                
        OrderStatus newStatus = orderStatusRepository.findById(statusId)
                .orElseThrow(() -> new RuntimeException("Trạng thái không hợp lệ"));
                
        order.setStatus(newStatus);
        orderRepository.save(order);
        
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse verifyPayment(Integer orderId, String paymentStatus) {
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thanh toán của đơn hàng này"));
                
        payment.setPaymentStatus(paymentStatus); // Cập nhật thành PAID hoặc FAILED
        paymentRepository.save(payment);
        
        return mapToOrderResponse(payment.getOrder());
    }

    // Hàm hỗ trợ map Order sang OrderResponse (Tái sử dụng cho code gọn)
    private OrderResponse mapToOrderResponse(Order order) {
        String paymentStatus = "N/A";
        var paymentOpt = paymentRepository.findByOrder_OrderId(order.getOrderId());
        if (paymentOpt.isPresent()) {
            paymentStatus = paymentOpt.get().getPaymentStatus();
        }

        Address address = order.getAddress();
        String fullAddress = address.getStreet() + ", " + address.getWard() + 
                             ", " + address.getDistrict() + ", " + address.getProvince();

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus().getStatusName())
                .paymentMethod(order.getPaymentMethod().getMethodName())
                .paymentStatus(paymentStatus)
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