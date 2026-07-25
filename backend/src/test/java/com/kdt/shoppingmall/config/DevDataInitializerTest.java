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
