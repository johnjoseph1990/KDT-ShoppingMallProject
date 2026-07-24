package com.kdt.shoppingmall.service;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

// 리뷰 본문에서 미리 정의한 사전(DICTIONARY)에 속하는 키워드를 추출하는 유틸리티.
//
// 방식: 단순 문자열 포함 검사(contains). 형태소 분석 없이도 "신선"이 포함되면
//      "신선하다/신선해요/신선한" 모두 잡힌다. 단, 동의어·오탈자는 잡지 못하는 한계가 있다.
//      (진단 보고서 4부 리스크로 기록됨)
//
// @Component: 스프링 빈으로 등록해서 ReviewService가 DI로 주입받는다.
//             사전을 DB 테이블로 옮기거나 형태소 분석기를 붙이는 확장도 이 클래스 안에서만 변경하면 된다.
@Component
public class KeywordExtractor {

  // 지역 농산물 쇼핑몰에서 자주 언급되는 키워드 사전.
  // 초기에는 코드 상수로 관리해 재현성을 보장한다 (CLAUDE.md 재현성 원칙).
  // 단어의 뿌리(어근) 형태로 저장하면 어미 변화("신선하다/신선해요")를 모두 포착한다.
  private static final Set<String> DICTIONARY =
      Set.of(
          // 맛·품질
          "신선",
          "맛있",
          "달콤",
          "아삭",
          "고소",
          "촉촉",
          // 배송·포장
          "포장",
          "배송",
          // 가격·가치
          "가성비",
          "저렴",
          "합리",
          // 재구매·만족
          "재구매",
          "추천",
          "만족");

  // content에서 사전 키워드를 찾아 Set으로 반환한다.
  // Set이므로 같은 키워드가 여러 번 나와도 중복 없이 하나만 반환한다.
  public Set<String> extract(String content) {
    if (content == null || content.isBlank()) {
      return Set.of();
    }
    return DICTIONARY.stream().filter(content::contains).collect(Collectors.toSet());
  }
}
