package com.kdt.shoppingmall.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.review.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class ProductRepositoryBestProductsTest {

  @Autowired private TestEntityManager em;

  @Autowired private ProductRepository productRepository;

  private Product highRated;
  private Product lowRated;
  private Product noReview;

  @BeforeEach
  void setUp() {
    highRated = em.persist(new Product("높은평점상품", "설명", 10000, 100, null));
    lowRated = em.persist(new Product("낮은평점상품", "설명", 10000, 100, null));
    noReview = em.persist(new Product("리뷰없는상품", "설명", 10000, 100, null));

    Member member = em.persist(new Member("test@test.com", "encoded", "테스터", MemberRole.USER));
    em.persist(new Review(member, highRated, 5, "최고"));
    em.persist(new Review(member, lowRated, 1, "별로"));

    em.flush();
    em.clear();
  }

  @Test
  void 평균평점_높은순으로_정렬() {
    Page<Product> result = productRepository.findAllOrderByAverageRatingDesc(PageRequest.of(0, 10));

    assertThat(result.getContent())
        .extracting("name")
        .containsExactly("높은평점상품", "낮은평점상품", "리뷰없는상품");
  }

  @Test
  void 전체상품수는_유지된다() {
    Page<Product> result = productRepository.findAllOrderByAverageRatingDesc(PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(3);
  }
}
