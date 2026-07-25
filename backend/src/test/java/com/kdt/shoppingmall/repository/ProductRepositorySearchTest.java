package com.kdt.shoppingmall.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.product.ProductTag;
import com.kdt.shoppingmall.domain.review.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class ProductRepositorySearchTest {

  @Autowired private TestEntityManager em;

  @Autowired private ProductRepository productRepository;

  @BeforeEach
  void setUp() {
    Product shoes = new Product("나이키 운동화", "편안한 운동화", 89000, 50, null);
    shoes.addTag(new ProductTag("신발"));
    shoes.addTag(new ProductTag("스포츠"));
    em.persist(shoes);

    Product shirt = new Product("폴로 티셔츠", "여름 티셔츠", 45000, 100, null);
    shirt.addTag(new ProductTag("의류"));
    em.persist(shirt);

    Product bag = new Product("나이키 가방", "스포츠 백팩", 120000, 20, null);
    bag.addTag(new ProductTag("가방"));
    bag.addTag(new ProductTag("스포츠"));
    em.persist(bag);

    // [별점 필터] 평균 별점을 만들려면 리뷰가 필요하다.
    // review 테이블은 (member_id, product_id) 유니크 제약이 있으므로,
    // 한 상품에 리뷰 2개를 달려면 서로 다른 회원이 있어야 한다.
    Member reviewer1 = new Member("r1@test.com", "pw", "리뷰어1", MemberRole.USER);
    Member reviewer2 = new Member("r2@test.com", "pw", "리뷰어2", MemberRole.USER);
    em.persist(reviewer1);
    em.persist(reviewer2);

    // 나이키 운동화: (5 + 4) / 2 = 4.5점
    em.persist(new Review(reviewer1, shoes, 5, "최고예요"));
    em.persist(new Review(reviewer2, shoes, 4, "괜찮아요"));
    // 폴로 티셔츠: 3.0점
    em.persist(new Review(reviewer1, shirt, 3, "보통"));
    // 나이키 가방: 리뷰 없음 → 평균이 NULL

    em.flush();
    em.clear();
  }

  @Test
  void 키워드없이_전체조회() {
    Page<Product> result = productRepository.searchProducts(null, null, PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(3);
  }

  @Test
  void 상품명_키워드검색() {
    Page<Product> result = productRepository.searchProducts("나이키", null, PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent())
        .extracting("name")
        .containsExactlyInAnyOrder("나이키 운동화", "나이키 가방");
  }

  @Test
  void 설명_키워드검색() {
    Page<Product> result = productRepository.searchProducts("티셔츠", null, PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).getName()).isEqualTo("폴로 티셔츠");
  }

  @Test
  void 태그_필터링() {
    Page<Product> result = productRepository.searchProducts(null, "스포츠", PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent())
        .extracting("name")
        .containsExactlyInAnyOrder("나이키 운동화", "나이키 가방");
  }

  @Test
  void 키워드_태그_복합검색() {
    Page<Product> result = productRepository.searchProducts("나이키", "스포츠", PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(2);
  }

  @Test
  void 별점필터_평균별점이_기준이상인_상품만_조회() {
    // 4.0 이상 → 나이키 운동화(4.5)만 통과, 폴로 티셔츠(3.0)는 탈락
    Page<Product> result =
        productRepository.searchProductsWithMinRating(null, null, 4.0, PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).getName()).isEqualTo("나이키 운동화");
  }

  @Test
  void 별점필터_리뷰가_없는_상품은_제외된다() {
    // 1.0 이상이면 사실상 모든 리뷰가 통과하지만, 리뷰가 아예 없는 '나이키 가방'은
    // AVG 결과가 NULL이라 비교가 성립하지 않아 제외된다.
    Page<Product> result =
        productRepository.searchProductsWithMinRating(null, null, 1.0, PageRequest.of(0, 10));

    assertThat(result.getContent())
        .extracting("name")
        .containsExactlyInAnyOrder("나이키 운동화", "폴로 티셔츠");
  }

  @Test
  void 별점필터가_null이면_리뷰없는_상품도_포함된다() {
    // minRating이 null일 때는 searchProducts(별점 조건 없는 쪽)를 호출하는 게
    // ProductService의 책임이므로, 여기서는 그 쪽 쿼리로 검증한다.
    Page<Product> result = productRepository.searchProducts(null, null, PageRequest.of(0, 10));

    assertThat(result.getContent()).extracting("name").contains("나이키 가방");
  }

  @Test
  void 키워드_태그_별점_복합검색() {
    // '나이키' + '스포츠' 후보는 운동화·가방 2개지만, 4.0점 필터로 운동화만 남는다.
    Page<Product> result =
        productRepository.searchProductsWithMinRating("나이키", "스포츠", 4.0, PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).getName()).isEqualTo("나이키 운동화");
  }

  @Test
  void 일치결과없음_빈페이지반환() {
    Page<Product> result =
        productRepository.searchProducts("존재하지않는상품", null, PageRequest.of(0, 10));

    assertThat(result.getTotalElements()).isEqualTo(0);
    assertThat(result.getContent()).isEmpty();
  }

  @Test
  void 페이징_동작확인() {
    Page<Product> page0 = productRepository.searchProducts(null, null, PageRequest.of(0, 2));
    Page<Product> page1 = productRepository.searchProducts(null, null, PageRequest.of(1, 2));

    assertThat(page0.getTotalElements()).isEqualTo(3);
    assertThat(page0.getTotalPages()).isEqualTo(2);
    assertThat(page0.getContent()).hasSize(2);
    assertThat(page1.getContent()).hasSize(1);
  }
}
