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
import org.springframework.data.domain.Pageable;

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

    // [P1-5] 페이징 API로 변경 — Page<Review>를 반환한다.
    Page<Review> reviews =
        reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId(), Pageable.unpaged());

    assertThat(reviews.getTotalElements()).isEqualTo(2);
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

  @Test
  void findAverageRatingByProductId_리뷰여러개_평균반환() {
    em.persistAndFlush(new Review(member, product, 5, "최고!"));
    em.persistAndFlush(
        new Review(
            em.persistAndFlush(new Member("other@test.com", "encoded", "다른회원", MemberRole.USER)),
            product,
            3,
            "보통"));

    Double average = reviewRepository.findAverageRatingByProductId(product.getId());

    assertThat(average).isEqualTo(4.0);
  }

  @Test
  void findAverageRatingByProductId_리뷰없으면_null반환() {
    Double average = reviewRepository.findAverageRatingByProductId(product.getId());

    assertThat(average).isNull();
  }

  // 평균 별점은 컬럼에 저장된 값이 아니라 조회 시점에 AVG로 계산되는 "파생값"이다.
  // 따라서 리뷰를 삭제하면 별도의 재계산 로직 없이도 평균이 자동으로 갱신되어야 한다.
  // 이 테스트는 그 설계 의도를 명세로 고정한다 — 나중에 평균을 컬럼에 캐싱하는 식으로
  // 바꾸면 이 테스트가 깨지면서 "삭제 시 재계산이 빠졌다"는 사실을 잡아준다.
  @Test
  void findAverageRatingByProductId_리뷰삭제후_평균재계산() {
    // given: 5점, 3점, 1점 → 평균 3.0
    Review five = em.persistAndFlush(new Review(member, product, 5, "최고!"));
    em.persistAndFlush(
        new Review(
            em.persistAndFlush(new Member("other@test.com", "encoded", "다른회원", MemberRole.USER)),
            product,
            3,
            "보통"));
    em.persistAndFlush(
        new Review(
            em.persistAndFlush(new Member("third@test.com", "encoded", "세번째", MemberRole.USER)),
            product,
            1,
            "아쉬움"));
    assertThat(reviewRepository.findAverageRatingByProductId(product.getId())).isEqualTo(3.0);

    // when: 5점 리뷰를 삭제한다 (남은 건 3점, 1점 → 평균 2.0)
    reviewRepository.delete(five);
    // flush: 삭제 SQL을 DB로 즉시 내보낸다. 안 하면 영속성 컨텍스트에만 남아
    // 이어지는 AVG 쿼리가 삭제 전 데이터를 읽을 수 있다.
    em.flush();
    // clear: 1차 캐시를 비워 다음 조회가 확실히 DB를 다시 읽게 만든다.
    em.clear();

    // then
    assertThat(reviewRepository.findAverageRatingByProductId(product.getId())).isEqualTo(2.0);
  }

  @Test
  void findAverageRatingByProductId_마지막리뷰삭제후_null복귀() {
    // given: 리뷰 1건뿐
    Review only = em.persistAndFlush(new Review(member, product, 4, "좋아요"));
    assertThat(reviewRepository.findAverageRatingByProductId(product.getId())).isEqualTo(4.0);

    // when: 유일한 리뷰를 삭제한다
    reviewRepository.delete(only);
    em.flush();
    em.clear();

    // then: 집계 대상이 없으므로 AVG는 0.0이 아니라 null이다.
    // 호출부(ProductService)가 null → 0.0 변환을 반드시 해야 하는 근거가 여기에 있다.
    assertThat(reviewRepository.findAverageRatingByProductId(product.getId())).isNull();
  }
}
