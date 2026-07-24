package com.kdt.shoppingmall.domain.review;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "review",
    uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "product_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @ManyToOne
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  // [P2-3] 리뷰 수정 API가 추가될 때 동시 수정 충돌을 DB 레벨에서 감지하기 위한 준비.
  // 지금은 수정 엔드포인트가 없어 실질 효과는 없지만, 나중에 추가할 때 @Version 없이 만들면
  // 충돌을 놓치는 버그가 생기므로 미리 선언해둔다. Product와 동일한 패턴.
  @Version private Long version;

  @Column(nullable = false)
  private int rating;

  @Column(columnDefinition = "TEXT")
  private String content;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public Review(Member member, Product product, int rating, String content) {
    this.member = member;
    this.product = product;
    this.rating = rating;
    this.content = content;
  }

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }
}
