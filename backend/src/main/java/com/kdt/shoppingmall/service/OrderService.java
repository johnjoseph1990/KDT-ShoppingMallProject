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
            request.deliveryName(),
            request.deliveryPhone(),
            request.deliveryZipCode(),
            request.deliveryAddress(),
            request.deliveryAddressDetail(),
            request.deliveryNote());
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
      // ORDERED → CANCELED 전이만 허용되므로, 이미 CANCELED인 주문에 pay()를 다시 호출하면
      // changeStatus()가 InvalidOrderStatusException을 던진다. 이중 재고 복구는 구조적으로 불가.
      order.changeStatus(OrderStatus.CANCELED);
      restoreStock(order);
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
    // 관리자가 주문을 직접 취소하는 경로도 결제 실패 자동 취소(pay())와 동일하게 재고를 되돌려야
    // 판매 가능한 재고가 취소된 주문에 묶여있는 문제를 막을 수 있다.
    if (status == OrderStatus.CANCELED) {
      restoreStock(order);
    }
    return OrderResponse.from(order);
  }

  // 주문이 취소될 때 차감됐던 재고를 되돌리는 공통 로직. pay() 실패 분기와
  // changeOrderStatus()의 관리자 취소 분기, 두 취소 경로에서 함께 사용한다.
  private void restoreStock(Order order) {
    order.getOrderItems().forEach(item -> item.getProduct().increaseStock(item.getQuantity()));
  }

  private List<CartItem> resolveCartItems(Long memberId, OrderCreateRequest request) {
    // null → 전체 장바구니 주문 (프론트가 '전체 주문' 시 null로 보냄)
    if (request.cartItemIds() == null) {
      return cartItemRepository.findByMemberId(memberId);
    }
    // [] → 빈 배열, 즉 아무것도 선택 안 함. null과 다른 의도이므로 폴백 없이 빈 목록 반환.
    // createOrder()에서 isEmpty() 체크가 EmptyCartException을 던진다.
    if (request.cartItemIds().isEmpty()) {
      return List.of();
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
