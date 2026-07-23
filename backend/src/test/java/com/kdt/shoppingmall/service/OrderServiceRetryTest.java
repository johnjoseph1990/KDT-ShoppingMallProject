package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.kdt.shoppingmall.domain.cart.CartItem;
import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.order.Order;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.dto.order.OrderCreateRequest;
import com.kdt.shoppingmall.dto.order.OrderResponse;
import com.kdt.shoppingmall.repository.CartItemRepository;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.OrderRepository;
import com.kdt.shoppingmall.repository.PaymentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

// @Retryable은 스프링 AOP 프록시가 있어야 동작하므로, 순수 Mockito 단위 테스트(new로 직접 생성)로는
// 재시도를 검증할 수 없다. 그래서 @EnableRetry를 켠 최소 스프링 컨텍스트만 띄워, 프록시가 적용된
// 진짜 OrderService 빈의 재시도 동작을 확인한다. (classes를 TestConfig로 한정해 DB 등 무거운
// 자동설정은 로드하지 않는다.)
@SpringBootTest(classes = OrderServiceRetryTest.TestConfig.class)
class OrderServiceRetryTest {

  // @EnableRetry: 이 설정으로 만든 OrderService 빈이 @Retryable 프록시로 감싸진다.
  @Configuration
  @EnableRetry
  static class TestConfig {
    // @MockitoBean으로 등록된 mock들이 이 @Bean 메서드의 파라미터로 주입된다.
    @Bean
    OrderService orderService(
        OrderRepository orderRepository,
        CartItemRepository cartItemRepository,
        MemberRepository memberRepository,
        PaymentRepository paymentRepository,
        PaymentProcessor paymentProcessor) {
      return new OrderService(
          orderRepository,
          cartItemRepository,
          memberRepository,
          paymentRepository,
          paymentProcessor);
    }
  }

  @MockitoBean private OrderRepository orderRepository;
  @MockitoBean private CartItemRepository cartItemRepository;
  @MockitoBean private MemberRepository memberRepository;
  @MockitoBean private PaymentRepository paymentRepository;
  @MockitoBean private PaymentProcessor paymentProcessor;

  @Autowired private OrderService orderService;

  private Member member;

  @BeforeEach
  void setUp() {
    member = new Member("test@test.com", "encoded", "테스터", MemberRole.USER);
    ReflectionTestUtils.setField(member, "id", 1L);

    Product product = new Product("상품A", "설명", 10000, 100, null);
    ReflectionTestUtils.setField(product, "id", 1L);
    CartItem cartItem = new CartItem(member, product, 2);

    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(cartItemRepository.findByMemberId(1L)).willReturn(List.of(cartItem));
  }

  private OrderCreateRequest request() {
    return new OrderCreateRequest(null, null, null, null, null, null, null);
  }

  @Test
  void createOrder_낙관적락_충돌시_재시도후_성공() {
    Order savedOrder = new Order(member);
    ReflectionTestUtils.setField(savedOrder, "id", 1L);

    // save가 첫 호출엔 낙관적 락 충돌, 두 번째 호출엔 성공하도록 설정한다.
    given(orderRepository.save(any(Order.class)))
        .willThrow(new OptimisticLockingFailureException("version conflict"))
        .willReturn(savedOrder);

    // @Retryable이 자동으로 다시 호출하므로 예외 없이 최종 성공해야 한다.
    OrderResponse response = orderService.createOrder(1L, request());

    assertThat(response).isNotNull();
    // save가 정확히 2번(1실패 + 1성공) 호출됐다면 재시도가 실제로 일어난 것이다.
    verify(orderRepository, times(2)).save(any(Order.class));
  }

  @Test
  void createOrder_낙관적락_계속충돌시_maxAttempts_소진후_예외전파() {
    // save가 매번 충돌하면 maxAttempts=3만큼 시도한 뒤 마지막 예외가 그대로 전파되고,
    // 이 예외를 GlobalExceptionHandler가 409(충돌)로 변환하게 된다.
    given(orderRepository.save(any(Order.class)))
        .willThrow(new OptimisticLockingFailureException("version conflict"));

    assertThatThrownBy(() -> orderService.createOrder(1L, request()))
        .isInstanceOf(OptimisticLockingFailureException.class);

    verify(orderRepository, times(3)).save(any(Order.class));
  }
}
