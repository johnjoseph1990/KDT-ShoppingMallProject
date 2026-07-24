package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

// KeywordExtractor는 Spring 컨텍스트가 필요 없는 순수 유틸리티라 @ExtendWith 없이 테스트한다.
// 의존성이 없으니 new로 직접 생성해서 가장 빠르게 검증할 수 있다.
class KeywordExtractorTest {

  private final KeywordExtractor extractor = new KeywordExtractor();

  // 사전에 있는 단어가 리뷰 내용에 포함되면 추출되어야 한다.
  @Test
  void 사전_키워드가_포함된_리뷰에서_키워드를_추출한다() {
    Set<String> keywords = extractor.extract("너무 신선하고 맛있어요. 재구매할게요!");

    assertThat(keywords).contains("신선", "맛있", "재구매");
  }

  // 사전에 없는 단어만 있으면 결과가 비어야 한다.
  @Test
  void 사전에_없는_단어만_있으면_빈_세트를_반환한다() {
    Set<String> keywords = extractor.extract("그냥 그래요");

    assertThat(keywords).isEmpty();
  }

  // 같은 키워드가 여러 번 등장해도 Set이므로 중복 없이 하나만 반환된다.
  @Test
  void 같은_키워드가_여러번_나와도_한번만_반환한다() {
    Set<String> keywords = extractor.extract("맛있고 맛있어요 정말 맛있습니다");

    assertThat(keywords).containsExactly("맛있");
  }

  // null·빈 문자열이 들어오면 NPE 없이 빈 세트를 반환해야 한다.
  @Test
  void 내용이_비어있으면_빈_세트를_반환한다() {
    assertThat(extractor.extract("")).isEmpty();
  }

  @Test
  void 내용이_null이면_빈_세트를_반환한다() {
    assertThat(extractor.extract(null)).isEmpty();
  }
}
