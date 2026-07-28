package com.kdt.shoppingmall.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

  private final TraceIdFilter filter = new TraceIdFilter();

  // 한 테스트가 실패해서 MDC.remove가 안 불렸더라도, 다음 테스트가 그 값을 이어받지 않도록 방어.
  @AfterEach
  void MDC_초기화() {
    MDC.clear();
  }

  @Test
  void 필터체인_실행_중에_MDC와_응답_헤더에_traceId가_설정된다() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    // 필터가 끝나면 MDC 값이 지워지므로, doFilter 콜백이 실행되는 "그 순간"에 값을 캡처해야 검증 가능하다.
    String[] traceIdDuringChain = new String[1];
    FilterChain chain = (req, res) -> traceIdDuringChain[0] = MDC.get("traceId");

    filter.doFilter(request, response, chain);

    assertThat(traceIdDuringChain[0]).isNotBlank();
    assertThat(response.getHeader("X-Trace-Id")).isEqualTo(traceIdDuringChain[0]);
  }

  @Test
  void 필터체인_실행이_끝나면_MDC에서_traceId가_제거된다() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> {};

    filter.doFilter(request, response, chain);

    assertThat(MDC.get("traceId")).isNull();
  }

  @Test
  void 필터체인에서_예외가_발생해도_MDC는_반드시_제거된다() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain =
        (req, res) -> {
          throw new RuntimeException("체인 하류에서 발생한 예외");
        };

    // 필터가 예외를 삼키지 않고 그대로 위로 전파하는지도 함께 확인한다 (finally만 있고 catch는 없어야 함).
    assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, chain));

    assertThat(MDC.get("traceId")).isNull();
  }
}
