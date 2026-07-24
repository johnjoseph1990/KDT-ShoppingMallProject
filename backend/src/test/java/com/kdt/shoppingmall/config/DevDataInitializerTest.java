package com.kdt.shoppingmall.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.kdt.shoppingmall.repository.ProductRepository;
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
}
