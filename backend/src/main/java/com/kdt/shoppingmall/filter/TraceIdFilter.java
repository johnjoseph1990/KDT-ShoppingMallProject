package com.kdt.shoppingmall.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// @Component: 이 클래스를 스프링 빈으로 등록한다. Filter 타입 빈은 스프링 부트가 자동으로
// 서블릿 컨테이너에 등록해주므로, 별도의 FilterRegistrationBean 설정이 필요 없다.
// @Order(HIGHEST_PRECEDENCE): 이 필터가 Spring Security의 필터 체인(DEFAULT_FILTER_ORDER
// = HIGHEST_PRECEDENCE + 100)보다 먼저 실행되도록 순서를 지정한다. 그래야 인증 실패(401/403)
// 응답에도 traceId가 로그에 남는다.
// OncePerRequestFilter: 한 요청당 doFilterInternal()이 정확히 한 번만 실행되도록 스프링이
// 보장해주는 베이스 클래스. 서블릿 포워드/에러 디스패치로 필터가 중복 실행되는 걸 막아준다.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

  private static final String TRACE_ID_KEY = "traceId";
  private static final String TRACE_ID_HEADER = "X-Trace-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // MDC(Mapped Diagnostic Context): 로그백이 제공하는 "현재 스레드 전용 보관함".
    // 여기 넣은 값은 이 스레드가 처리하는 모든 로그 줄에 자동으로 삽입된다
    // (application.properties의 logging.pattern.level 설정과 연동).
    String traceId = UUID.randomUUID().toString();
    MDC.put(TRACE_ID_KEY, traceId);
    // 클라이언트가 버그를 리포트할 때 이 값을 알려주면 바로 grep할 수 있도록 응답 헤더에도 실어준다.
    response.setHeader(TRACE_ID_HEADER, traceId);

    // try/finally: 하류(컨트롤러 등)에서 예외가 터져도 finally는 반드시 실행되므로,
    // MDC.remove가 항상 호출된다는 것을 보장한다. catch를 두지 않은 이유는 예외를 여기서
    // 삼키지 않고 그대로 위로 전파해 GlobalExceptionHandler가 처리하도록 하기 위함이다.
    try {
      filterChain.doFilter(request, response);
    } finally {
      // Tomcat은 스레드 풀을 재사용하므로, 여기서 지우지 않으면 다음 무관한 요청이
      // 같은 스레드에서 처리될 때 이전 traceId가 로그에 그대로 남는다.
      MDC.remove(TRACE_ID_KEY);
    }
  }
}
