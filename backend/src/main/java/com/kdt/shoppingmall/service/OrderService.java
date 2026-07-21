package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.domain.cart.CartItem;
import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.order.Order;
import com.kdt.shoppingmall.domain.order.OrderItem;
import com.kdt.shoppingmall.domain.order.OrderStatus;
import com.kdt.shoppingmall.domain.payment.Payment;
import com.kdt.shoppingmall.domain.payment.PaymentStatus;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.dto.order.OrderCreateRequest;
import com.kdt.shoppingmall.dto.order.OrderResponse;
import com.kdt.shoppingmall.dto.payment.PaymentResponse;
import com.kdt.shoppingmall.exception.EmptyCartException;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.CartItemRepository;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.OrderRepository;
import com.kdt.shoppingmall.repository.PaymentRepository;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

  private static final int MOCK_PAYMENT_SUCCESS_RATE = 90;

  private final OrderRepository orderRepository;
  private final CartItemRepository cartItemRepository;
  private final MemberRepository memberRepository;
  private final PaymentRepository paymentRepository;

  public OrderService(
      OrderRepository orderRepository,
      CartItemRepository cartItemRepository,
      MemberRepository memberRepository,
      PaymentRepository paymentRepository) {
    this.orderRepository = orderRepository;
    this.cartItemRepository = cartItemRepository;
    this.memberRepository = memberRepository;
    this.paymentRepository = paymentRepository;
  }

  @Transactional
  public OrderResponse createOrder(Long memberId, OrderCreateRequest request) {
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("회원을 찾을 수 없습니다. id=" + memberId));

    List<CartItem> cartItems = resolveCartItems(memberId, request);
    if (cartItems.isEmpty()) {
      throw new EmptyCartException("주문할 장바구니 항목이 없습니다.");
    }

    // 주문 시점의 배송지를 Order에 함께 저장 (이후 주소 변경과 무관하게 원본 보존)
    Order order =
        new Order(
            member,
            request != null ? request.deliveryName() : null,
            request != null ? request.deliveryPhone() : null,
            request != null ? request.deliveryZipCode() : null,
            request != null ? request.deliveryAddress() : null,
            request != null ? request.deliveryAddressDetail() : null,
            request != null ? request.deliveryNote() : null);
    for (CartItem cartItem : cartItems) {
      Product product = cartItem.getProduct();
      product.decreaseStock(cartItem.getQuantity());
      order.addItem(new OrderItem(product, product.getPrice(), cartItem.getQuantity()));
    }

    Order saved = orderRepository.save(order);
    cartItemRepository.deleteAll(cartItems);
    return OrderResponse.from(saved);
  }

  public List<OrderResponse> getOrders(Long memberId) {
    return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
        .map(OrderResponse::from)
        .toList();
  }

  public OrderResponse getOrder(Long memberId, Long orderId) {
    return OrderResponse.from(getOwnedOrderOrThrow(memberId, orderId));
  }

  @Transactional
  public PaymentResponse pay(Long memberId, Long orderId) {
    Order order = getOwnedOrderOrThrow(memberId, orderId);
    boolean success = ThreadLocalRandom.current().nextInt(100) < MOCK_PAYMENT_SUCCESS_RATE;

    if (success) {
      order.changeStatus(OrderStatus.PAID);
    } else {
      order.changeStatus(OrderStatus.CANCELED);
      order.getOrderItems().forEach(item -> item.getProduct().increaseStock(item.getQuantity()));
    }

    Payment payment =
        paymentRepository.save(
            new Payment(
                order,
                success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
                order.getTotalPrice()));
    return PaymentResponse.from(payment);
  }

  // status가 없으면(null) 전체 조회, 있으면 상태별 필터링해서 조회한다.
  public Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
    Page<Order> orders =
        status == null
            ? orderRepository.findAll(pageable)
            : orderRepository.findByStatus(status, pageable);
    return orders.map(OrderResponse::from);
  }

  @Transactional
  public OrderResponse changeOrderStatus(Long orderId, OrderStatus status) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("주문을 찾을 수 없습니다. id=" + orderId));
    order.changeStatus(status);
    return OrderResponse.from(order);
  }

  private List<CartItem> resolveCartItems(Long memberId, OrderCreateRequest request) {
    if (request == null || request.cartItemIds() == null || request.cartItemIds().isEmpty()) {
      return cartItemRepository.findByMemberId(memberId);
    }
    return request.cartItemIds().stream()
        .map(
            id ->
                cartItemRepository
                    .findById(id)
                    .orElseThrow(
                        () -> new ResourceNotFoundException("장바구니 항목을 찾을 수 없습니다. id=" + id)))
        .peek(
            cartItem -> {
              if (!cartItem.getMember().getId().equals(memberId)) {
                throw new AccessDeniedException("본인의 장바구니만 주문할 수 있습니다.");
              }
            })
        .toList();
  }

  private Order getOwnedOrderOrThrow(Long memberId, Long orderId) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("주문을 찾을 수 없습니다. id=" + orderId));
    if (!order.getMember().getId().equals(memberId)) {
      throw new AccessDeniedException("본인의 주문만 조회할 수 있습니다.");
    }
    return order;
  }
}
