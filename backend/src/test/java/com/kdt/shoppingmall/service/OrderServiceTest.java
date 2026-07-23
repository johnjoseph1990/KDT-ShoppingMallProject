package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.kdt.shoppingmall.domain.cart.CartItem;
import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
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
import com.kdt.shoppingmall.exception.InsufficientStockException;
import com.kdt.shoppingmall.exception.InvalidOrderStatusException;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.CartItemRepository;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.OrderRepository;
import com.kdt.shoppingmall.repository.PaymentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;

  @Mock private CartItemRepository cartItemRepository;

  @Mock private MemberRepository memberRepository;

  @Mock private PaymentRepository paymentRepository;

  @InjectMocks private OrderService orderService;

  private Member member;
  private Product product;

  @BeforeEach
  void setUp() {
    member = new Member("test@test.com", "encoded", "테스터", MemberRole.USER);
    ReflectionTestUtils.setField(member, "id", 1L);

    product = new Product("상품A", "설명", 10000, 100, null);
    ReflectionTestUtils.setField(product, "id", 1L);
  }

  @Test
  void createOrder_성공() {
    CartItem cartItem = new CartItem(member, product, 2);
    Order savedOrder = new Order(member);
    ReflectionTestUtils.setField(savedOrder, "id", 1L);

    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(cartItemRepository.findByMemberId(1L)).willReturn(List.of(cartItem));
    given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

    // 배송지 없이(null) 생성해도 주문 자체는 성공해야 한다
    OrderResponse response =
        orderService.createOrder(
            1L, new OrderCreateRequest(null, null, null, null, null, null, null));

    assertThat(response).isNotNull();
    assertThat(product.getStockQuantity()).isEqualTo(98);
  }

  @Test
  void createOrder_빈카트_예외발생() {
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(cartItemRepository.findByMemberId(1L)).willReturn(List.of());

    assertThatThrownBy(
            () ->
                orderService.createOrder(
                    1L, new OrderCreateRequest(null, null, null, null, null, null, null)))
        .isInstanceOf(EmptyCartException.class);
  }

  @Test
  void createOrder_재고부족_예외발생() {
    // 재고(1개)보다 많은 수량(2개)을 주문하면 도메인의 InsufficientStockException이 서비스까지 전파돼야 한다.
    Product lowStock = new Product("품절임박상품", "설명", 10000, 1, null);
    ReflectionTestUtils.setField(lowStock, "id", 2L);
    CartItem cartItem = new CartItem(member, lowStock, 2);

    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(cartItemRepository.findByMemberId(1L)).willReturn(List.of(cartItem));

    assertThatThrownBy(
            () ->
                orderService.createOrder(
                    1L, new OrderCreateRequest(null, null, null, null, null, null, null)))
        .isInstanceOf(InsufficientStockException.class);
  }

  @Test
  void createOrder_빈_cartItemIds_예외발생() {
    // cartItemIds=[]는 null(전체 주문)과 달리 '선택 항목 없음'이므로 EmptyCartException이 발생해야 한다.
    // 이전에는 isEmpty() 조건에서 전체 장바구니 폴백이 실행되는 버그가 있었다.
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));

    assertThatThrownBy(
            () ->
                orderService.createOrder(
                    1L, new OrderCreateRequest(List.of(), null, null, null, null, null, null)))
        .isInstanceOf(EmptyCartException.class);
  }

  @Test
  void getOrders_성공() {
    Order order = new Order(member);
    given(orderRepository.findByMemberIdOrderByCreatedAtDesc(1L)).willReturn(List.of(order));

    List<OrderResponse> responses = orderService.getOrders(1L);

    assertThat(responses).hasSize(1);
  }

  @Test
  void getOrder_성공() {
    Order order = new Order(member);
    ReflectionTestUtils.setField(order, "id", 1L);
    given(orderRepository.findById(1L)).willReturn(Optional.of(order));

    OrderResponse response = orderService.getOrder(1L, 1L);

    assertThat(response).isNotNull();
  }

  @Test
  void getOrder_다른회원_접근금지() {
    Member other = new Member("other@test.com", "encoded", "다른회원", MemberRole.USER);
    ReflectionTestUtils.setField(other, "id", 2L);
    Order order = new Order(other);
    ReflectionTestUtils.setField(order, "id", 1L);

    given(orderRepository.findById(1L)).willReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.getOrder(1L, 1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getAllOrders_성공() {
    Order order = new Order(member);
    Pageable pageable = PageRequest.of(0, 10);
    Page<Order> page = new PageImpl<>(List.of(order));
    given(orderRepository.findAll(pageable)).willReturn(page);

    Page<OrderResponse> responses = orderService.getAllOrders(null, pageable);

    assertThat(responses.getTotalElements()).isEqualTo(1);
  }

  @Test
  void getAllOrders_상태필터_성공() {
    Order order = new Order(member);
    Pageable pageable = PageRequest.of(0, 10);
    Page<Order> page = new PageImpl<>(List.of(order));
    given(orderRepository.findByStatus(OrderStatus.PAID, pageable)).willReturn(page);

    Page<OrderResponse> responses = orderService.getAllOrders(OrderStatus.PAID, pageable);

    assertThat(responses.getTotalElements()).isEqualTo(1);
  }

  @Test
  void changeOrderStatus_성공() {
    Order order = new Order(member);
    ReflectionTestUtils.setField(order, "id", 1L);
    order.changeStatus(OrderStatus.PAID);
    given(orderRepository.findById(1L)).willReturn(Optional.of(order));

    OrderResponse response = orderService.changeOrderStatus(1L, OrderStatus.SHIPPING);

    assertThat(response.status()).isEqualTo(OrderStatus.SHIPPING);
  }

  @Test
  void changeOrderStatus_잘못된전이_예외발생() {
    Order order = new Order(member);
    ReflectionTestUtils.setField(order, "id", 1L);
    given(orderRepository.findById(1L)).willReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.changeOrderStatus(1L, OrderStatus.DELIVERED))
        .isInstanceOf(InvalidOrderStatusException.class);
  }

  @Test
  void changeOrderStatus_존재하지않는주문_예외발생() {
    given(orderRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.changeOrderStatus(99L, OrderStatus.PAID))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void changeOrderStatus_취소시_재고복구() {
    // 주문 생성 시 차감됐던 재고(2개)를 관리자 취소가 되돌리는지 확인한다.
    product.decreaseStock(2);
    Order order = new Order(member);
    order.addItem(new OrderItem(product, product.getPrice(), 2));
    ReflectionTestUtils.setField(order, "id", 1L);
    given(orderRepository.findById(1L)).willReturn(Optional.of(order));

    orderService.changeOrderStatus(1L, OrderStatus.CANCELED);

    assertThat(product.getStockQuantity()).isEqualTo(100);
  }

  @Test
  void changeOrderStatus_SHIPPING에서_취소_예외발생() {
    // SPRINT_PLAN에서 확정한 규칙: 배송 시작(SHIPPING) 후에는 취소할 수 없다.
    Order order = new Order(member);
    order.changeStatus(OrderStatus.PAID);
    order.changeStatus(OrderStatus.SHIPPING);
    ReflectionTestUtils.setField(order, "id", 1L);
    given(orderRepository.findById(1L)).willReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.changeOrderStatus(1L, OrderStatus.CANCELED))
        .isInstanceOf(InvalidOrderStatusException.class);
  }

  @RepeatedTest(5)
  void pay_결제처리됨() {
    Order order = new Order(member);
    ReflectionTestUtils.setField(order, "id", 1L);

    given(orderRepository.findById(1L)).willReturn(Optional.of(order));
    given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

    PaymentResponse response = orderService.pay(1L, 1L);

    assertThat(response.status()).isIn(PaymentStatus.SUCCESS, PaymentStatus.FAILED);
    assertThat(order.getStatus()).isIn(OrderStatus.PAID, OrderStatus.CANCELED);
  }

  // pay()의 모의 결제는 90% 성공 / 10% 실패 확률(ThreadLocalRandom)이라 결정론적이지 않다.
  // 5회 반복 중 실패 케이스가 나올 때마다 재고 복구 경로를 검증한다.
  // (결정론적 검증이 필요하면 PaymentProcessor 의존성 주입으로 리팩토링 후 Mock 처리 필요)
  @RepeatedTest(5)
  void pay_결제결과에따라_재고상태가_올바르다() {
    product.decreaseStock(2); // 주문 생성 시 이미 차감된 상태를 흉내낸다 (100 → 98)
    Order order = new Order(member);
    order.addItem(new OrderItem(product, product.getPrice(), 2));
    ReflectionTestUtils.setField(order, "id", 1L);

    given(orderRepository.findById(1L)).willReturn(Optional.of(order));
    given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

    orderService.pay(1L, 1L);

    // 결제 성공(PAID) → 차감 유지(98), 결제 실패(CANCELED) → 재고 복구(100)
    if (order.getStatus() == OrderStatus.CANCELED) {
      assertThat(product.getStockQuantity()).as("결제 실패 시 차감됐던 재고(2개)가 복구돼야 한다").isEqualTo(100);
    } else {
      assertThat(product.getStockQuantity()).as("결제 성공 시 재고가 차감된 상태로 유지돼야 한다").isEqualTo(98);
    }
  }
}
