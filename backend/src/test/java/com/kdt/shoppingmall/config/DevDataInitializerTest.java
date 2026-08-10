package com.kdt.shoppingmall.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.repository.ProductRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;

// @DataJpaTest: 실제 H2 DB로 시드 로직(DevDataInitializer.seedProducts)이
// "클론/재시작 시 정육 상품이 그대로 재현되는가 + 여러 번 돌려도 중복이 안 쌓이는가"를 검증한다.
// 이 검증이 있어야 어제처럼 "손으로 DB에 넣은 데이터가 git에 없어 사라지는" 문제를 막을 수 있다.
@DataJpaTest
class DevDataInitializerTest {

  @Autowired private ProductRepository productRepository;

  @Test
  void 정육_카테고리_상품이_10개_시드된다() throws Exception {
    // seedProducts는 ProductRepository만 의존하므로 직접 생성해 실행한다.
    ApplicationRunner runner = new DevDataInitializer().seedProducts(productRepository);
    runner.run(null);

    // 프론트 카테고리 필터와 동일하게 '정육' 태그로 조회했을 때 10개가 나와야 한다.
    long meatCount =
        productRepository.searchProducts(null, "정육", Pageable.unpaged()).getTotalElements();
    assertThat(meatCount).isEqualTo(10);
  }

  // [카테고리 균형] 프론트 상품목록의 카테고리 필터(ProductListPage.jsx)는
  // 채소·과일·정육·베이커리·꾸러미 5개 태그로 고정돼 있다. 어느 하나가 1~2개뿐이면
  // 그 탭을 눌렀을 때 카드가 한 장만 뜨는 "고장 난 화면"처럼 보인다.
  // 정육(10개)을 제외한 4개 카테고리를 각 5개로 못 박아, 시드가 균형을 잃으면 테스트가 깨지게 한다.
  @Test
  void 정육을_제외한_카테고리는_각각_5개씩_시드된다() throws Exception {
    new DevDataInitializer().seedProducts(productRepository).run(null);

    // List.of(...)로 검사할 태그를 나열하고 for문으로 한 번에 확인한다.
    // 테스트를 4개로 나누는 대신 하나로 묶되, as(...)로 어느 카테고리가 깨졌는지 알 수 있게 한다.
    for (String tag : List.of("채소", "과일", "베이커리", "꾸러미")) {
      long count =
          productRepository.searchProducts(null, tag, Pageable.unpaged()).getTotalElements();

      assertThat(count).as("'%s' 카테고리 상품 수", tag).isEqualTo(5);
    }
  }

  // 전체 개수도 함께 못 박는다. 상품목록 페이지 크기가 10(ProductController의 @PageableDefault)이라
  // 30개여야 페이지네이션이 3페이지로 동작하는 걸 시연할 수 있다.
  @Test
  void 상품은_모두_30개_시드된다() throws Exception {
    new DevDataInitializer().seedProducts(productRepository).run(null);

    assertThat(productRepository.count()).isEqualTo(30);
  }

  // [재고 배지 시연 보장]
  // 프론트의 stockBadge(frontend/src/utils/product.js)는 재고를 세 구간으로 나눈다.
  //   재고 0      → '품절' 배지 + 담기 버튼 disabled
  //   재고 1~5    → '마감임박' 배지
  //   재고 6 이상 → 배지 없음
  // 예전 시드는 최소 재고가 10이라 클론 직후 화면에서 배지가 하나도 안 보였다.
  // = 코드는 있는데 시연이 불가능한 UI. 이 테스트로 "시드가 세 구간을 모두 만든다"를 못 박는다.
  //
  // [단언 강도를 왜 '정확히 N개'가 아니라 '1개 이상'으로 했나]
  // 위의 카테고리·전체 개수 테스트는 '균형' 자체가 명세라서 정확한 값(isEqualTo)을 쓴다.
  // 반면 이 테스트가 지키려는 건 개수가 아니라 "배지를 화면에서 보여줄 수 있는가"다.
  // 정확한 개수로 못 박으면 나중에 품절 상품을 하나 더 넣는 무관한 변경에도 테스트가 깨진다.
  // 목적에 맞는 최소 조건만 단언해 취성(brittle) 테스트가 되지 않게 한다.
  @Test
  void 재고_배지_세_구간을_모두_시연할_수_있게_시드된다() throws Exception {
    new DevDataInitializer().seedProducts(productRepository).run(null);

    List<Product> products = productRepository.findAll();

    // stream(): 컬렉션을 훑는 도구. filter(조건)로 걸러 count()로 개수를 센다.
    // p -> p.getStockQuantity() == 0 은 "상품 p를 받아 재고가 0인지 판단하는 식"(람다).
    long soldOut = products.stream().filter(p -> p.getStockQuantity() == 0).count();
    long almostGone =
        products.stream()
            .filter(p -> p.getStockQuantity() >= 1 && p.getStockQuantity() <= 5)
            .count();
    long noBadge = products.stream().filter(p -> p.getStockQuantity() >= 6).count();

    // as(...): 실패했을 때 출력될 메시지. 6개월 뒤 이 테스트가 빨개졌을 때
    // 코드를 읽지 않고도 "왜 이 조건이 필요한지"를 알 수 있어야 한다.
    assertThat(soldOut).as("재고 0인 상품이 없으면 '품절' 배지와 담기 버튼 비활성화를 시연할 수 없다").isGreaterThanOrEqualTo(1);

    assertThat(almostGone).as("재고 1~5인 상품이 없으면 '마감임박' 배지를 시연할 수 없다").isGreaterThanOrEqualTo(1);

    assertThat(noBadge).as("재고 6 이상인 상품이 없으면 '배지 없음' 기본 상태를 시연할 수 없다").isGreaterThanOrEqualTo(1);
  }

  @Test
  void 시드를_두_번_실행해도_상품이_중복되지_않는다() throws Exception {
    ApplicationRunner runner = new DevDataInitializer().seedProducts(productRepository);

    runner.run(null);
    long afterFirst = productRepository.count();

    // 재시작을 흉내내어 한 번 더 실행 — 멱등하면 개수가 그대로여야 한다.
    runner.run(null);
    long afterSecond = productRepository.count();

    assertThat(afterSecond).isEqualTo(afterFirst);
  }

  // 예전에는 상품 이미지를 Unsplash 주소로 직접 연결(핫링크)했는데,
  // 외부 사진이 삭제되면 우리 화면이 같이 깨지고(실제로 '한우 국거리'가 깨졌다)
  // 상품명과 무관한 사진이 걸려도 아무도 못 잡아낸다.
  // 그래서 이미지를 저장소 안(frontend/public/uploads)에 두기로 하고, 그 규칙을 테스트로 못 박는다.
  @Test
  void 모든_시드_상품_이미지가_저장소에_포함된_파일을_가리킨다() throws Exception {
    new DevDataInitializer().seedProducts(productRepository).run(null);

    List<Product> products = productRepository.findAll();
    assertThat(products).isNotEmpty();

    for (Product product : products) {
      String imageUrl = product.getImageUrl();

      // 1) 외부 URL(http로 시작) 금지 — 저장소 안의 경로여야 한다.
      assertThat(imageUrl)
          .as("상품 '%s' 의 이미지는 /uploads/ 로 시작해야 한다 (외부 핫링크 금지)", product.getName())
          .startsWith("/uploads/");

      // 2) 그 경로에 실제 파일이 있어야 한다.
      //    테스트는 backend/ 에서 실행되므로 ../frontend/public 을 기준으로 찾는다.
      Path file = Path.of("..", "frontend", "public", imageUrl.substring(1));
      assertThat(Files.exists(file))
          .as("상품 '%s' 의 이미지 파일(%s)이 저장소에 없다", product.getName(), imageUrl)
          .isTrue();
    }
  }

  // 시드는 '이름이 이미 있으면 건너뛴다'로 멱등성을 지키는데, 그 때문에
  // 시드의 이미지 주소만 고쳐도 이미 DB에 저장된 상품은 옛 이미지를 계속 들고 있다.
  // (= 코드를 고쳐 push 했는데 화면은 그대로인 상황) 이미지 동기화가 되는지 검증한다.
  @Test
  void 시드의_이미지가_바뀌면_기존_상품의_이미지도_갱신된다() throws Exception {
    ApplicationRunner runner = new DevDataInitializer().seedProducts(productRepository);
    runner.run(null);

    // 옛 이미지를 들고 있는 상황을 흉내낸다 — 잘못된 외부 URL로 되돌려 놓는다.
    Product product = productRepository.findAll().get(0);
    String seededImageUrl = product.getImageUrl();
    product.changeImageUrl("https://images.unsplash.com/photo-000000-stale");
    productRepository.saveAndFlush(product);

    // 다시 시드를 실행하면 시드에 적힌 이미지로 되돌아와야 한다.
    runner.run(null);

    Product refreshed = productRepository.findById(product.getId()).orElseThrow();
    assertThat(refreshed.getImageUrl()).isEqualTo(seededImageUrl);
  }
}
