package com.kdt.shoppingmall.domain.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 리뷰 한 건에서 추출된 키워드를 저장하는 엔티티.
// 리뷰 1개 → 키워드 N개(1:N). 상품 단위로 집계하면 자주 언급된 키워드를 알 수 있다.
// "신선하고 맛있어요" → Review 1개, ReviewKeyword 2개("신선", "맛있")
@Entity
@Table(name = "review_keyword")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewKeyword {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 이 키워드가 속한 리뷰. 리뷰가 삭제될 때 ReviewService에서 먼저 삭제한다.
  @ManyToOne
  @JoinColumn(name = "review_id", nullable = false)
  private Review review;

  @Column(nullable = false)
  private String keyword;

  public ReviewKeyword(Review review, String keyword) {
    this.review = review;
    this.keyword = keyword;
  }
}
