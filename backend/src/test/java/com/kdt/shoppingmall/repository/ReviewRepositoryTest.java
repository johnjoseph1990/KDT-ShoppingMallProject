package com.kdt.shoppingmall.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.review.Review;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class ReviewRepositoryTest {

  @Autowired private TestEntityManager em;

  @Autowired private ReviewRepository reviewRepository;

  private Member member;
  private Product product;

  @BeforeEach
  void setUp() {
    member = em.persistAndFlush(new Member("test@test.com", "encoded", "테스터", MemberRole.USER));
    product = em.persistAndFlush(new Product("상품A", "설명", 10000, 100, null));
  }

  @Test
  void findByProductIdOrderByCreatedAtDesc_성공() {
    em.persistAndFlush(new Review(member, product, 5, "최고!"));
    em.persistAndFlush(
        new Review(
            em.persistAndFlush(new Member("other@test.com", "encoded", "다른회원", MemberRole.USER)),
            product,
            3,
            "보통"));

    List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId());

    assertThat(reviews).hasSize(2);
  }

  @Test
  void existsByMemberIdAndProductId_존재하면_true() {
    em.persistAndFlush(new Review(member, product, 4, "좋아요"));

    assertThat(reviewRepository.existsByMemberIdAndProductId(member.getId(), product.getId()))
        .isTrue();
  }

  @Test
  void existsByMemberIdAndProductId_없으면_false() {
    assertThat(reviewRepository.existsByMemberIdAndProductId(member.getId(), product.getId()))
        .isFalse();
  }
}
