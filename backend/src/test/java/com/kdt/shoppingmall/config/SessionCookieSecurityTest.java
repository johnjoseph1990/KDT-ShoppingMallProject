package com.kdt.shoppingmall.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.dto.member.LoginRequest;
import com.kdt.shoppingmall.repository.MemberRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

// 로그인 시 내려가는 세션 쿠키(JSESSIONID)에 SameSite 속성이 실제로 붙는지 검증한다.
//
// 왜 이 테스트가 필요한가 (2026-08-07 security-reviewer 검토 결과):
//   이 프로젝트는 SecurityConfig에서 CSRF 방어를 꺼둔 채(`.csrf(csrf -> csrf.disable())`)
//   세션 쿠키로 인증한다. CSRF 토큰이 없으면 남는 방어선은 "브라우저가 이 쿠키를
//   다른 사이트發 요청에 실어 보내지 않는 것"뿐인데, 그게 바로 SameSite 속성이다.
//   설정하지 않으면 최신 크롬의 기본값(Lax)에 기대게 되는데, 이는 우리 서버 설정이
//   아니라 브라우저 정책이라 보증되지 않는다(파이어폭스 등은 기본값이 다르다).
//
// ★ 왜 application.properties의 `server.servlet.session.cookie.same-site=Lax` 한 줄로
//   해결하지 않았는가 — 그렇게 하면 이 테스트를 쓸 수 없기 때문이다:
//   스프링 부트는 `classpath:/application.properties`를 "단일 리소스"로 찾아 맨 처음
//   발견한 하나만 읽는다. 테스트 실행 시에는 src/test/resources 쪽이 먼저 잡히므로
//   src/main/resources의 설정은 테스트에서 **아예 보이지 않는다**. 그래서 설정을
//   프로퍼티가 아니라 CookieSameSiteSupplier 빈(SecurityConfig)으로 넣었다.
//   빈은 스프링 컨텍스트의 일부라 테스트가 실제로 관찰할 수 있다.
//
// @SpringBootTest(webEnvironment = RANDOM_PORT):
//   쿠키 속성은 서블릿 컨테이너(톰캣)가 응답 헤더를 조립할 때 붙는다. @WebMvcTest의
//   MockMvc는 톰캣을 띄우지 않는 "가짜" 서블릿 환경이라 이 동작을 재현하지 못한다.
//   그래서 실제 톰캣을 임의의 빈 포트로 띄우고 진짜 HTTP 요청을 보낸다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionCookieSecurityTest {

  // 이 테스트 전용 계정. DevDataInitializer의 시드 계정에 의존하지 않는다 —
  // 그 초기화 코드는 @Profile("dev")라서 테스트 컨텍스트에서는 돌지 않을 수 있고,
  // "시드가 있겠지"라는 가정 위에 선 테스트는 조용히 깨지기 때문이다.
  private static final String EMAIL = "cookie-test@example.com";
  private static final String PASSWORD = "test1234!";

  // TestRestTemplate: 위에서 띄운 실제 서버로 HTTP 요청을 보내는 테스트용 클라이언트.
  // RANDOM_PORT로 띄우면 포트를 몰라도 되게 스프링이 baseUrl을 미리 채워 넣어준다.
  @Autowired private TestRestTemplate restTemplate;

  @Autowired private MemberRepository memberRepository;

  // 비밀번호는 BCrypt로 암호화되어 저장되므로, 로그인이 성공하려면 테스트도
  // 같은 인코더로 암호화해서 넣어야 한다(평문으로 넣으면 인증 실패).
  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void 로그인_가능한_회원을_준비한다() {
    // 컨텍스트가 테스트 클래스 간에 재사용될 수 있으므로 중복 저장을 막는다(멱등).
    if (!memberRepository.existsByEmail(EMAIL)) {
      memberRepository.save(
          new Member(EMAIL, passwordEncoder.encode(PASSWORD), "쿠키테스트", MemberRole.USER));
    }
  }

  @Test
  void 로그인_응답의_세션_쿠키에_SameSite_Lax가_붙는다() {
    // given & when: 실제 HTTP로 로그인한다.
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/auth/login", new LoginRequest(EMAIL, PASSWORD), String.class);

    // then ①: 먼저 로그인 자체가 성공했는지 확인한다.
    // 이걸 빼면 로그인이 깨졌을 때 "쿠키가 없다"는 엉뚱한 실패 메시지만 보게 된다.
    assertThat(response.getStatusCode().is2xxSuccessful())
        .as("로그인이 성공해야 세션 쿠키를 검사할 수 있다. 실제 응답: %s", response.getStatusCode())
        .isTrue();

    // then ②: Set-Cookie 헤더가 존재하는지. 서버가 세션을 만들지 않으면 이 헤더 자체가 없다.
    List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertThat(setCookies).as("로그인 응답에 Set-Cookie 헤더가 있어야 한다").isNotNull().isNotEmpty();

    // then ③: 그 중 세션 쿠키(JSESSIONID)를 찾는다.
    String sessionCookie =
        setCookies.stream()
            .filter(cookie -> cookie.startsWith("JSESSIONID="))
            .findFirst()
            .orElse(null);
    assertThat(sessionCookie).as("세션 쿠키(JSESSIONID)가 내려와야 한다").isNotNull();

    // then ④: 핵심 단언 — SameSite 속성이 실제로 붙어 있는가.
    // 대소문자를 가리지 않고 비교한다(서블릿 스펙상 속성명은 대소문자 구분이 없다).
    assertThat(sessionCookie.toLowerCase())
        .as("CSRF 방어가 꺼져 있으므로 세션 쿠키에 SameSite=Lax가 반드시 붙어야 한다. 실제 쿠키: %s", sessionCookie)
        .contains("samesite=lax");
  }
}
