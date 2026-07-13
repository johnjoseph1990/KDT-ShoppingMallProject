package com.kdt.shoppingmall.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kdt.shoppingmall.domain.cart.CartItem;
import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.product.Product;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class CartItemRepositoryTest {

  @Autowired private TestEntityManager em;

  @Autowired private CartItemRepository cartItemRepository;

  private Member member;
  private Product product;

  @BeforeEach
  void setUp() {
    member = em.persistAndFlush(new Member("test@test.com", "encoded", "테스터", MemberRole.USER));
    product = em.persistAndFlush(new Product("상품A", "설명", 10000, 100, null));
  }

  @Test
  void findByMemberId_성공() {
    em.persistAndFlush(new CartItem(member, product, 3));

    List<CartItem> result = cartItemRepository.findByMemberId(member.getId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getQuantity()).isEqualTo(3);
  }

  @Test
  void findByMemberAndProduct_존재() {
    em.persistAndFlush(new CartItem(member, product, 2));

    Optional<CartItem> result = cartItemRepository.findByMemberAndProduct(member, product);

    assertThat(result).isPresent();
    assertThat(result.get().getQuantity()).isEqualTo(2);
  }

  @Test
  void findByMemberAndProduct_미존재() {
    Optional<CartItem> result = cartItemRepository.findByMemberAndProduct(member, product);

    assertThat(result).isEmpty();
  }
}
