package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.kdt.shoppingmall.domain.cart.CartItem;
import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.dto.cart.CartItemRequest;
import com.kdt.shoppingmall.dto.cart.CartItemResponse;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.CartItemRepository;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

  @Mock private CartItemRepository cartItemRepository;

  @Mock private MemberRepository memberRepository;

  @Mock private ProductRepository productRepository;

  @InjectMocks private CartService cartService;

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
  void addItem_새아이템_성공() {
    CartItemRequest request = new CartItemRequest(1L, 2);
    CartItem cartItem = new CartItem(member, product, 2);
    ReflectionTestUtils.setField(cartItem, "id", 1L);

    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(productRepository.findById(1L)).willReturn(Optional.of(product));
    given(cartItemRepository.findByMemberAndProduct(member, product)).willReturn(Optional.empty());
    given(cartItemRepository.save(any(CartItem.class))).willReturn(cartItem);

    CartItemResponse response = cartService.addItem(1L, request);

    assertThat(response.quantity()).isEqualTo(2);
    assertThat(response.productName()).isEqualTo("상품A");
    verify(cartItemRepository).save(any(CartItem.class));
  }

  @Test
  void addItem_기존아이템_수량합산() {
    CartItemRequest request = new CartItemRequest(1L, 3);
    CartItem existing = new CartItem(member, product, 2);
    ReflectionTestUtils.setField(existing, "id", 1L);

    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(productRepository.findById(1L)).willReturn(Optional.of(product));
    given(cartItemRepository.findByMemberAndProduct(member, product))
        .willReturn(Optional.of(existing));

    CartItemResponse response = cartService.addItem(1L, request);

    assertThat(response.quantity()).isEqualTo(5);
  }

  @Test
  void addItem_존재하지않는상품_예외발생() {
    CartItemRequest request = new CartItemRequest(99L, 1);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(productRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> cartService.addItem(1L, request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void getCart_성공() {
    CartItem cartItem = new CartItem(member, product, 2);
    given(cartItemRepository.findByMemberId(1L)).willReturn(List.of(cartItem));

    List<CartItemResponse> responses = cartService.getCart(1L);

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).quantity()).isEqualTo(2);
  }

  @Test
  void updateQuantity_성공() {
    CartItem cartItem = new CartItem(member, product, 2);
    ReflectionTestUtils.setField(cartItem, "id", 1L);

    given(cartItemRepository.findById(1L)).willReturn(Optional.of(cartItem));

    CartItemResponse response = cartService.updateQuantity(1L, 1L, 5);

    assertThat(response.quantity()).isEqualTo(5);
  }

  @Test
  void updateQuantity_다른회원_접근금지() {
    Member other = new Member("other@test.com", "encoded", "다른회원", MemberRole.USER);
    ReflectionTestUtils.setField(other, "id", 2L);

    CartItem cartItem = new CartItem(other, product, 2);
    ReflectionTestUtils.setField(cartItem, "id", 1L);

    given(cartItemRepository.findById(1L)).willReturn(Optional.of(cartItem));

    assertThatThrownBy(() -> cartService.updateQuantity(1L, 1L, 5))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void removeItem_성공() {
    CartItem cartItem = new CartItem(member, product, 2);
    ReflectionTestUtils.setField(cartItem, "id", 1L);

    given(cartItemRepository.findById(1L)).willReturn(Optional.of(cartItem));

    cartService.removeItem(1L, 1L);

    verify(cartItemRepository).delete(cartItem);
  }

  @Test
  void addItem_없는회원_예외발생() {
    // getMemberOrThrow: memberRepository.findById().orElseThrow() 예외 경로 커버
    CartItemRequest request = new CartItemRequest(1L, 1);
    given(memberRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> cartService.addItem(99L, request))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("99");
  }

  @Test
  void updateQuantity_없는_cartItem_예외발생() {
    // getOwnedCartItemOrThrow: cartItemRepository.findById().orElseThrow() 예외 경로 커버
    given(cartItemRepository.findById(999L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> cartService.updateQuantity(1L, 999L, 3))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("999");
  }
}
