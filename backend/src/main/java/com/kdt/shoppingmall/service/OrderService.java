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
import com.kdt.shoppingmall.dto.payment.PaymentConfirmRequest;
import com.kdt.shoppingmall.dto.payment.PaymentResponse;
import com.kdt.shoppingmall.exception.EmptyCartException;
import com.kdt.shoppingmall.exception.PaymentAmountMismatchException;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.CartItemRepository;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.OrderRepository;
import com.kdt.shoppingmall.repository.PaymentRepository;
import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

  private final OrderRepository orderRepository;
  private final CartItemRepository cartItemRepository;
  private final MemberRepository memberRepository;
  private final PaymentRepository paymentRepository;
  // 결제 성공 여부를 결정하는 전략 — 테스트에서 Mock으로 교체해 결과를 고정할 수 있다.
  private final PaymentProcessor paymentProcessor;

  public OrderService(
      OrderRepository orderRepository,
      CartItemRepository cartItemRepository,
      MemberRepository memberRepository,
      PaymentRepository paymentRepository,
      PaymentProcessor paymentProcessor) {
    this.orderRepository = orderRepository;
    this.cartItemRepository = cartItemRepository;
    this.memberRepository = memberRepository;
    this.paymentRepository = paymentRepository;
    this.paymentProcessor = paymentProcessor;
  }

  // @Retryable: 동시 주문으로 재고 차감이 겹쳐 낙관적 락 충돌
  // (OptimisticLockingFailureException)이 나면 최대 3번까지 자동으로 재호출한다.
  // backoff delay=50ms: 재시도 사이에 잠깐 쉬어, 충돌한 두 요청이 동시에 재돌진하는 것을 피한다.
  // @Retryable 어드바이스가 @Transactional보다 바깥에 위치하므로, 재시도마다 새 트랜잭션이
  // 열려 이전 시도에서 롤백된 상태를 이어받지 않는다. 3번 모두 실패하면 마지막 예외가 그대로
  // 전파되어 GlobalExceptionHandler가 409(충돌)로 응답한다.
  @Retryable(
      retryFor = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 50))
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
  public PaymentResponse pay(Long memberId, Long orderId, PaymentConfirmRequest request) {
    Order order = getOwnedOrderOrThrow(memberId, orderId);

    // 프론트가 보낸 amount는 브라우저 개발자도구로 조작 가능하므로 절대 신뢰하지 않는다.
    // 서버가 order.getTotalPrice()로 직접 계산한 금액과 다르면 토스 승인 API 호출 자체를 막는다.
    if (order.getTotalPrice() != request.amount()) {
      throw new PaymentAmountMismatchException("결제 금액이 일치하지 않습니다.");
    }

    boolean success =
        paymentProcessor.confirm(request.paymentKey(), request.orderId(), order.getTotalPrice());

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
                order.getTotalPrice(),
                request.paymentKey()));
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
