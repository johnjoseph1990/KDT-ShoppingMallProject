package com.kdt.shoppingmall.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.review.Review;
import com.kdt.shoppingmall.domain.review.ReviewKeyword;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

// @DataJpaTest: 실제 H2 DB로 집계 쿼리(GROUP BY + COUNT)가 올바로 동작하는지 검증한다.
// 키워드 빈도 집계는 "진짜 DB 쿼리"가 의도대로 동작해야만 의미가 있어 Mock 테스트로는 대체 불가하다.
@DataJpaTest
class ReviewKeywordRepositoryTest {

  @Autowired private TestEntityManager em;
  @Autowired private ReviewKeywordRepository reviewKeywordRepository;
  @Autowired private MemberRepository memberRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private ReviewRepository reviewRepository;

  private Product product;

  @BeforeEach
  void setUp() {
    product = em.persistAndFlush(new Product("상품A", "설명", 10000, 10, null));

    // 3명의 회원이 각각 리뷰를 작성하고 키워드를 남긴다:
    //   회원1·2 → "신선" (2회), 회원3 → "맛있" (1회)
    for (int i = 1; i <= 3; i++) {
      Member m =
          memberRepository.save(
              new Member("user" + i + "@test.com", "pw", "이름" + i, MemberRole.USER));
      Review r = reviewRepository.save(new Review(m, product, 5, "리뷰"));
      String keyword = (i <= 2) ? "신선" : "맛있";
      reviewKeywordRepository.save(new ReviewKeyword(r, keyword));
    }
    em.flush();
    em.clear();
  }

  // 키워드 빈도가 내림차순으로 정렬되어야 한다 — 가장 많이 나온 키워드가 먼저.
  @Test
  void 상품별_키워드를_빈도_내림차순으로_조회한다() {
    List<Object[]> result =
        reviewKeywordRepository.findTopKeywordsByProductId(product.getId(), PageRequest.of(0, 10));

    assertThat(result).hasSize(2);
    assertThat((String) result.get(0)[0]).isEqualTo("신선"); // 2회 → 1위
    assertThat((Long) result.get(0)[1]).isEqualTo(2L);
    assertThat((String) result.get(1)[0]).isEqualTo("맛있"); // 1회 → 2위
    assertThat((Long) result.get(1)[1]).isEqualTo(1L);
  }

  // limit 파라미터로 결과 개수를 제한할 수 있어야 한다.
  @Test
  void limit_적용시_상위_N개만_반환한다() {
    List<Object[]> result =
        reviewKeywordRepository.findTopKeywordsByProductId(product.getId(), PageRequest.of(0, 1));

    assertThat(result).hasSize(1);
    assertThat((String) result.get(0)[0]).isEqualTo("신선");
  }

  // 리뷰가 없는 상품은 빈 리스트를 반환해야 한다.
  @Test
  void 리뷰없는_상품은_빈_리스트를_반환한다() {
    Product newProduct = em.persistAndFlush(new Product("신상품", "설명", 5000, 5, null));
    em.clear();

    List<Object[]> result =
        reviewKeywordRepository.findTopKeywordsByProductId(
            newProduct.getId(), PageRequest.of(0, 10));

    assertThat(result).isEmpty();
  }

  // 리뷰 삭제 시 연결된 키워드도 함께 삭제되어야 한다 (deleteByReviewId 검증).
  @Test
  void 리뷰_삭제_시_해당_리뷰의_키워드도_삭제된다() {
    // 새 리뷰 + 키워드 생성
    Member m = memberRepository.save(new Member("del@test.com", "pw", "삭제자", MemberRole.USER));
    Review review = reviewRepository.save(new Review(m, product, 3, "삭제할 리뷰"));
    reviewKeywordRepository.save(new ReviewKeyword(review, "신선"));
    em.flush();
    em.clear();

    long before = reviewKeywordRepository.count();
    reviewKeywordRepository.deleteByReviewId(review.getId());
    em.flush();

    assertThat(reviewKeywordRepository.count()).isEqualTo(before - 1);
  }
}
