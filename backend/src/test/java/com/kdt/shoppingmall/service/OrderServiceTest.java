package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.kdt.shoppingmall.domain.cart.CartItem;
import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.order.Order;
import com.kdt.shoppingmall.domain.order.OrderItem;
import com.kdt.shoppingmall.domain.order.OrderStatus;
import com.kdt.shoppingmall.domain.payment.Payment;
import com.kdt.shoppingmall.domain.payment.PaymentMethod;
import com.kdt.shoppingmall.domain.payment.PaymentStatus;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.dto.order.OrderCreateRequest;
import com.kdt.shoppingmall.dto.order.OrderResponse;
import com.kdt.shoppingmall.dto.payment.PaymentConfirmRequest;
import com.kdt.shoppingmall.dto.payment.PaymentResponse;
import com.kdt.shoppingmall.exception.EmptyCartException;
import com.kdt.shoppingmall.exception.InsufficientStockException;
import com.kdt.shoppingmall.exception.InvalidOrderStatusException;
import com.kdt.shoppingmall.exception.PaymentAmountMismatchException;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.CartItemRepository;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.OrderRepository;
import com.kdt.shoppingmall.repository.PaymentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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

  // 결제 결과를 고정하기 위해 Mock으로 주입한다 — 성공/실패를 willReturn으로 제어
  @Mock private PaymentProcessor paymentProcessor;

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

  @Test
  void pay_카드결제성공시_PAID_상태_유지() {
    // paymentProcessor.confirm(...)이 DONE을 반환하도록 고정 → 카드결제 즉시완료 경로를 검증
    given(paymentProcessor.confirm(any(), any(), anyInt()))
        .willReturn(
            new PaymentProcessor.ConfirmResult(
                PaymentProcessor.ConfirmStatus.DONE, PaymentMethod.CARD, null, null, null));
    product.decreaseStock(2); // 주문 생성 시 이미 차감된 상태 (100 → 98)
    Order order = new Order(member);
    order.addItem(new OrderItem(product, product.getPrice(), 2)); // totalPrice = 20000
    ReflectionTestUtils.setField(order, "id", 1L);

    given(orderRepository.findById(1L)).willReturn(Optional.of(order));
    given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

    PaymentConfirmRequest request = new PaymentConfirmRequest("test_payment_key", "ORDER-1", 20000);
    PaymentResponse response = orderService.pay(1L, 1L, request);

    assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    assertThat(product.getStockQuantity()).as("결제 성공 시 재고가 차감된 상태로 유지돼야 한다").isEqualTo(98);
  }

  @Test
  void pay_가상계좌_발급성공시_WAITING_FOR_DEPOSIT_상태() {
    // 가상계좌는 발급만 되고 실제 입금은 아직이므로, confirm 성공이어도 PAID가 아니라
    // WAITING_FOR_DEPOSIT으로 가야 한다. 재고는 카드결제와 마찬가지로 이미 차감된 채 유지된다.
    given(paymentProcessor.confirm(any(), any(), anyInt()))
        .willReturn(
            new PaymentProcessor.ConfirmResult(
                PaymentProcessor.ConfirmStatus.WAITING_FOR_DEPOSIT,
                PaymentMethod.VIRTUAL_ACCOUNT,
                "20",
                "1234567890",
                null));
    product.decreaseStock(2);
    Order order = new Order(member);
    order.addItem(new OrderItem(product, product.getPrice(), 2)); // totalPrice = 20000
    ReflectionTestUtils.setField(order, "id", 1L);

    given(orderRepository.findById(1L)).willReturn(Optional.of(order));
    given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

    PaymentConfirmRequest request = new PaymentConfirmRequest("test_payment_key", "ORDER-1", 20000);
    PaymentResponse response = orderService.pay(1L, 1L, request);

    assertThat(response.status()).isEqualTo(PaymentStatus.WAITING_FOR_DEPOSIT);
    assertThat(response.virtualAccountNumber()).isEqualTo("1234567890");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.WAITING_FOR_DEPOSIT);
    assertThat(product.getStockQuantity()).as("입금 대기 중에는 재고를 복구하지 않는다").isEqualTo(98);
  }

  @Test
  void pay_결제실패시_CANCELED_재고복구() {
    // paymentProcessor.confirm(...)이 실패를 반환하도록 고정 → 항상 실패 경로를 검증
    given(paymentProcessor.confirm(any(), any(), anyInt()))
        .willReturn(PaymentProcessor.ConfirmResult.failed());
    product.decreaseStock(2); // 주문 생성 시 이미 차감된 상태 (100 → 98)
    Order order = new Order(member);
    order.addItem(new OrderItem(product, product.getPrice(), 2)); // totalPrice = 20000
    ReflectionTestUtils.setField(order, "id", 1L);

    given(orderRepository.findById(1L)).willReturn(Optional.of(order));
    given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

    PaymentConfirmRequest request = new PaymentConfirmRequest("test_payment_key", "ORDER-1", 20000);
    PaymentResponse response = orderService.pay(1L, 1L, request);

    assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    assertThat(product.getStockQuantity()).as("결제 실패 시 차감됐던 재고(2개)가 복구돼야 한다").isEqualTo(100);
  }

  @Test
  void checkDepositStatus_입금완료시_PAID_전환() {
    Order order = new Order(member);
    ReflectionTestUtils.setField(order, "id", 1L);
    order.changeStatus(OrderStatus.WAITING_FOR_DEPOSIT);
    Payment payment =
        new Payment(
            order,
            PaymentStatus.WAITING_FOR_DEPOSIT,
            PaymentMethod.VIRTUAL_ACCOUNT,
            20000,
            "test_payment_key");

    given(orderRepository.findById(1L)).willReturn(Optional.of(order));
    given(paymentRepository.findByOrder(order)).willReturn(Optional.of(payment));
    given(paymentProcessor.checkStatus("test_payment_key"))
        .willReturn(
            new PaymentProcessor.ConfirmResult(
                PaymentProcessor.ConfirmStatus.DONE,
                PaymentMethod.VIRTUAL_ACCOUNT,
                null,
                null,
                null));

    OrderResponse response = orderService.checkDepositStatus(1L, 1L);

    assertThat(response.status()).isEqualTo(OrderStatus.PAID);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
  }

  @Test
  void checkDepositStatus_아직_입금안됨_상태유지() {
    Order order = new Order(member);
    ReflectionTestUtils.setField(order, "id", 1L);
    order.changeStatus(OrderStatus.WAITING_FOR_DEPOSIT);
    Payment payment =
        new Payment(
            order,
            PaymentStatus.WAITING_FOR_DEPOSIT,
            PaymentMethod.VIRTUAL_ACCOUNT,
            20000,
            "test_payment_key");

    given(orderRepository.findById(1L)).willReturn(Optional.of(order));
    given(paymentRepository.findByOrder(order)).willReturn(Optional.of(payment));
    given(paymentProcessor.checkStatus("test_payment_key"))
        .willReturn(
            new PaymentProcessor.ConfirmResult(
                PaymentProcessor.ConfirmStatus.WAITING_FOR_DEPOSIT,
                PaymentMethod.VIRTUAL_ACCOUNT,
                null,
                null,
                null));

    OrderResponse response = orderService.checkDepositStatus(1L, 1L);

    assertThat(response.status()).isEqualTo(OrderStatus.WAITING_FOR_DEPOSIT);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.WAITING_FOR_DEPOSIT);
  }

  @Test
  void checkDepositStatus_입금대기상태가_아니면_토스에_묻지않는다() {
    Order order = new Order(member);
    ReflectionTestUtils.setField(order, "id", 1L);
    order.changeStatus(OrderStatus.PAID);
    given(orderRepository.findById(1L)).willReturn(Optional.of(order));

    OrderResponse response = orderService.checkDepositStatus(1L, 1L);

    assertThat(response.status()).isEqualTo(OrderStatus.PAID);
    verify(paymentProcessor, never()).checkStatus(any());
  }

  @Test
  void pay_금액불일치시_예외발생() {
    // 클라이언트(프론트)가 보낸 amount는 브라우저 개발자도구로 조작 가능하므로 절대 신뢰하지 않는다.
    // 서버가 order.getTotalPrice()로 직접 계산한 금액과 다르면 토스 승인 API를 호출하지도 않고 즉시 거부한다.
    product.decreaseStock(2);
    Order order = new Order(member);
    order.addItem(new OrderItem(product, product.getPrice(), 2)); // totalPrice = 20000
    ReflectionTestUtils.setField(order, "id", 1L);
    given(orderRepository.findById(1L)).willReturn(Optional.of(order));

    PaymentConfirmRequest request = new PaymentConfirmRequest("test_payment_key", "ORDER-1", 9999);

    assertThatThrownBy(() -> orderService.pay(1L, 1L, request))
        .isInstanceOf(PaymentAmountMismatchException.class);
    // 금액이 안 맞으면 외부 API(토스)를 호출할 필요가 없어야 한다 — 불필요한 외부 호출 방지 검증
    verify(paymentProcessor, never()).confirm(any(), any(), anyInt());
  }

  @Test
  void createOrder_없는회원_예외발생() {
    // memberRepository.findById().orElseThrow() 예외 경로 커버
    given(memberRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                orderService.createOrder(
                    99L, new OrderCreateRequest(null, null, null, null, null, null, null)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("99");
  }

  @Test
  void createOrder_특정_cartItemIds_선택주문_성공() {
    // cartItemIds가 [1L]이면 전체 장바구니가 아닌 선택한 항목만 주문해야 한다.
    CartItem cartItem = new CartItem(member, product, 1);
    Order savedOrder = new Order(member);
    ReflectionTestUtils.setField(savedOrder, "id", 1L);

    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(cartItemRepository.findById(1L)).willReturn(Optional.of(cartItem));
    given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

    OrderResponse response =
        orderService.createOrder(
            1L, new OrderCreateRequest(List.of(1L), null, null, null, null, null, null));

    assertThat(response).isNotNull();
    assertThat(product.getStockQuantity()).isEqualTo(99); // 1개 차감
  }

  @Test
  void createOrder_없는_cartItem_예외발생() {
    // resolveCartItems: cartItemRepository.findById().orElseThrow() 예외 경로 커버
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(cartItemRepository.findById(999L)).willReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                orderService.createOrder(
                    1L, new OrderCreateRequest(List.of(999L), null, null, null, null, null, null)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("999");
  }

  @Test
  void createOrder_다른회원_cartItem_접근금지() {
    // resolveCartItems: peek 람다에서 소유자 검증 실패 → AccessDeniedException 경로 커버
    Member other = new Member("other@test.com", "encoded", "다른회원", MemberRole.USER);
    ReflectionTestUtils.setField(other, "id", 2L);
    CartItem otherCartItem = new CartItem(other, product, 1);

    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(cartItemRepository.findById(1L)).willReturn(Optional.of(otherCartItem));

    assertThatThrownBy(
            () ->
                orderService.createOrder(
                    1L, new OrderCreateRequest(List.of(1L), null, null, null, null, null, null)))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  }

  @Test
  void getOrder_없는주문_예외발생() {
    // getOwnedOrderOrThrow: orderRepository.findById().orElseThrow() 예외 경로 커버
    given(orderRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.getOrder(1L, 99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("99");
  }
}
