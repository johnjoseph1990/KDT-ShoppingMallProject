package com.kdt.shoppingmall.domain.product;

import com.kdt.shoppingmall.exception.InsufficientStockException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private int price;

  @Column(nullable = false)
  private int stockQuantity;

  private String imageUrl;

  // 상품 상세 페이지 조회 횟수. findById() 호출마다 1씩 증가한다.
  // 실무에서는 Redis 카운터로 처리하지만, 학습 규모에서는 DB UPDATE로 단순 구현한다.
  // columnDefinition에 DEFAULT 0을 줘야 기존 row가 있는 테이블에 컬럼을 추가할 때
  // (ddl-auto=update가 ALTER TABLE ADD COLUMN을 실행) 기존 row도 0으로 채워지며 NOT NULL 제약을 통과한다.
  @Column(nullable = false, columnDefinition = "integer default 0")
  private int viewCount = 0;

  @Version private Long version;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  // [P1-1] @BatchSize: 상품 목록을 불러올 때 각 상품의 tags를 N번 개별 SELECT하는 대신,
  // 최대 50개씩 IN 절로 묶어서 조회한다. N+1 문제를 완전히 없애지는 않지만
  // 쿼리 수를 ceil(N/50)으로 대폭 줄인다.
  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @org.hibernate.annotations.BatchSize(size = 50)
  private List<ProductTag> tags = new ArrayList<>();

  public Product(String name, String description, int price, int stockQuantity, String imageUrl) {
    this.name = name;
    this.description = description;
    this.price = price;
    this.stockQuantity = stockQuantity;
    this.imageUrl = imageUrl;
  }

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  public void update(
      String name, String description, int price, int stockQuantity, String imageUrl) {
    this.name = name;
    this.description = description;
    this.price = price;
    this.stockQuantity = stockQuantity;
    this.imageUrl = imageUrl;
  }

  // 이미지 주소만 바꾸는 메서드. update()는 이름·가격·재고까지 전부 덮어쓰기 때문에,
  // 시드가 이미지만 다시 맞춰줄 때 관리자가 고쳐둔 가격·재고까지 되돌려버린다.
  // 그래서 바꿔야 하는 필드 하나만 바꾸는 좁은 메서드를 따로 둔다.
  public void changeImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public void decreaseStock(int quantity) {
    if (this.stockQuantity < quantity) {
      throw new InsufficientStockException(
          "재고가 부족합니다. 상품: " + this.name + ", 재고: " + this.stockQuantity);
    }
    this.stockQuantity -= quantity;
  }

  public void increaseStock(int quantity) {
    this.stockQuantity += quantity;
  }

  public void increaseViewCount() {
    this.viewCount++;
  }

  public void addTag(ProductTag tag) {
    tags.add(tag);
    tag.assignProduct(this);
  }

  public void clearTags() {
    tags.clear();
  }
}
