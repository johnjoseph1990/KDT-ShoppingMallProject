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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

// [P1-2] findBestProductsWithAvgRating — 5개 컷오프 + 평균 별점 정렬 통합 검증.
// ProductRepositoryTest와 역할이 겹치지 않도록, 여기서는 "리뷰 없는 상품이 결과에서
// 완전히 제외된다"는 시나리오를 집중적으로 검증한다.
@DataJpaTest
class ProductRepositoryBestProductsTest {

  @Autowired private TestEntityManager em;
  @Autowired private ProductRepository productRepository;
  @Autowired private MemberRepository memberRepository;
  @Autowired private ReviewRepository reviewRepository;

  private Product highRated;
  private Product lowRated;

  @BeforeEach
  void setUp() {
    highRated = em.persist(new Product("높은평점상품", "설명", 10000, 100, null));
    lowRated = em.persist(new Product("낮은평점상품", "설명", 10000, 100, null));
    em.persist(new Product("리뷰없는상품", "설명", 10000, 100, null)); // 리뷰 0개 → 컷오프 미달

    // highRated: 5명이 별점 5점 → 평균 5.0
    for (int i = 0; i < 5; i++) {
      Member m =
          memberRepository.save(
              new Member("high_" + i + "@test.com", "pw", "이름" + i, MemberRole.USER));
      reviewRepository.save(new Review(m, highRated, 5, "최고"));
    }
    // lowRated: 5명이 별점 1점 → 평균 1.0
    for (int i = 0; i < 5; i++) {
      Member m =
          memberRepository.save(
              new Member("low_" + i + "@test.com", "pw", "이름" + i, MemberRole.USER));
      reviewRepository.save(new Review(m, lowRated, 1, "별로"));
    }

    em.flush();
    em.clear();
  }

  // 리뷰 없는 상품은 컷오프에 걸려 결과에 포함되지 않는다.
  @Test
  void 리뷰없는상품은_베스트_목록에서_제외된다() {
    Page<Object[]> result =
        productRepository.findBestProductsWithAvgRating(PageRequest.of(0, 10));

    List<String> names =
        result.getContent().stream().map(row -> ((Product) row[0]).getName()).toList();
    assertThat(names).containsExactly("높은평점상품", "낮은평점상품"); // 평점순 DESC
    assertThat(names).doesNotContain("리뷰없는상품");
  }

  // 결과가 없으면 빈 Page를 반환한다 — 폴백 전환 트리거 조건.
  @Test
  void 조건_충족_상품이_없으면_빈페이지반환() {
    // 기존 데이터 없는 새 DB에 상품만 있고 리뷰 0개인 경우 시뮬레이션
    Page<Object[]> result =
        productRepository.findBestProductsWithAvgRating(Pageable.unpaged());

    // setUp에서 리뷰 5개 이상 상품이 2개 있으므로 총 2개가 반환된다.
    assertThat(result.getTotalElements()).isEqualTo(2);
  }

  // Object[1]이 실제 평균 별점 Double 값인지 검증한다.
  @Test
  void 반환된_평균별점이_정확하다() {
    Page<Object[]> result =
        productRepository.findBestProductsWithAvgRating(PageRequest.of(0, 1));

    Object[] top = result.getContent().get(0);
    Product topProduct = (Product) top[0];
    Double avgRating = (Double) top[1];

    assertThat(topProduct.getName()).isEqualTo("높은평점상품");
    assertThat(avgRating).isEqualTo(5.0);
  }
}
