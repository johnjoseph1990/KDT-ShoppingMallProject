package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.kdt.shoppingmall.domain.cart.CartItem;
import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.order.Order;
import com.kdt.shoppingmall.domain.order.OrderStatus;
import com.kdt.shoppingmall.domain.payment.Payment;
import com.kdt.shoppingmall.domain.payment.PaymentStatus;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.dto.order.OrderCreateRequest;
import com.kdt.shoppingmall.dto.order.OrderResponse;
import com.kdt.shoppingmall.dto.payment.PaymentResponse;
import com.kdt.shoppingmall.exception.EmptyCartException;
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

    OrderResponse response = orderService.createOrder(1L, new OrderCreateRequest(null));

    assertThat(response).isNotNull();
    assertThat(product.getStockQuantity()).isEqualTo(98);
  }

  @Test
  void createOrder_빈카트_예외발생() {
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(cartItemRepository.findByMemberId(1L)).willReturn(List.of());

    assertThatThrownBy(() -> orderService.createOrder(1L, new OrderCreateRequest(null)))
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
}
