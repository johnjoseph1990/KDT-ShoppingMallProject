package com.kdt.shoppingmall.config;

import com.kdt.shoppingmall.security.MemberUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

// @Configuration: "이 클래스 안에 있는 @Bean 메서드들이 반환하는 객체를 스프링 컨테이너가
// 관리해달라"는 표시. 스프링은 앱이 뜰 때 이 클래스를 읽어서 passwordEncoder(),

// authenticationManager(), filterChain() 세 개를 미리 만들어두고, 다른 클래스에서
// 필요할 때 주입(DI)해준다.
// @EnableWebSecurity: 스프링 시큐리티의 "요청을 가로채는 필터들"을 활성화하는 스위치.
// 이게 없으면 아래 filterChain() Bean을 만들어도 실제로 요청을 검사하지 않는다.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  // 이 클래스가 회원 정보를 조회할 방법이 필요해서 UserDetailsService 구현체를 주입받는다.
  // 로그인 시 "이 이메일의 회원이 실제로 있는지, 비밀번호가 맞는지"를 확인하는 데 쓰인다.
  private final MemberUserDetailsService userDetailsService;

  // 생성자 주입: 필드에 @Autowired를 붙이는 대신, 생성자의 파라미터로 받으면
  // 스프링이 알아서 MemberUserDetailsService 빈을 찾아서 넣어준다.
  // 생성자 주입을 쓰는 이유는 필드가 final이라 불변(생성 이후 변경 불가)이 보장되기 때문.
  public SecurityConfig(MemberUserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  // PasswordEncoder는 인터페이스(비밀번호를 암호화하고, 입력값과 비교하는 규약만 정의)이고
  // BCryptPasswordEncoder가 그 구현체다. BCrypt는 같은 비밀번호를 넣어도 매번 다른
  // 암호문이 나오는 해시 알고리즘이라, DB에 평문 비밀번호를 저장하지 않아도 되게 해준다.
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // AuthenticationManager는 "로그인 시도(이메일+비밀번호)를 받아서 인증 성공/실패를
  // 판단하는" 인터페이스다. 실제 판단 로직은 DaoAuthenticationProvider가 맡는데,
  // 이 Provider가 userDetailsService로 회원을 조회하고 passwordEncoder로 비밀번호를
  // 비교한다. ProviderManager는 여러 Provider를 순서대로 시도해주는 실행기다
  // (여기서는 Provider가 1개뿐이라 단순히 그 하나를 감싸는 역할).
  @Bean
  public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new org.springframework.security.authentication.ProviderManager(provider);
  }

  // SecurityFilterChain은 "이 요청은 인증이 필요한지, 어떤 권한이 있어야 하는지"를
  // 정의하는 필터 목록을 의미하는 인터페이스다. HttpSecurity는 이 필터 체인을
  // 코드로 조립할 수 있게 해주는 빌더(설정을 하나씩 이어붙이고 마지막에 build()로 완성)다.
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // 세션 기반 REST API + Postman/curl 테스트 전제로 CSRF는 끈다 (프로덕션이면 재검토 필요)
        // CSRF(Cross-Site Request Forgery)는 브라우저가 쿠키를 자동으로 실어 보내는 특성을
        // 악용해 사용자 몰래 요청을 보내는 공격이다. 원래 세션 기반 인증에서는 CSRF 토큰
        // 검증을 켜두는 게 정석이지만, 지금은 REST 클라이언트 테스트 편의를 위해 꺼둔 상태.
        .csrf(csrf -> csrf.disable())
        // exceptionHandling: 인증/인가 실패 시 어떤 응답을 내려줄지 설정하는 부분.
        // authenticationEntryPoint는 "인증 자체가 안 된(로그인 안 했거나 정보가 틀린)" 경우
        // 호출되는데, 기본값은 403(Forbidden)이라 "인증 실패"와 "권한 부족"이 구분되지 않는다.
        // HttpStatusEntryPoint로 401(Unauthorized)을 명시해 HTTP 의미에 맞게 고친다.
        .exceptionHandling(
            exception ->
                exception.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        // authorizeHttpRequests: URL 패턴별로 "누가 접근 가능한지" 규칙을 순서대로 등록한다.
        // 스프링 시큐리티는 위에서부터 매칭되는 첫 규칙을 적용하므로 순서가 중요하다.
        .authorizeHttpRequests(
            auth ->
                // 회원가입/로그인은 로그인 전에 호출해야 하니 인증 없이 허용
                auth.requestMatchers("/api/auth/signup", "/api/auth/login")
                    .permitAll()
                    // /error: 컨트롤러/DB에서 예외가 나면 스프링이 내부적으로 이 경로로
                    // 재전송(forward)한다. 허용하지 않으면 진짜 에러(예: 500)가 인증 안 된
                    // 사용자에게는 403으로 잘못 뒤바뀌어 원인을 숨기게 된다.
                    .requestMatchers("/error")
                    .permitAll()
                    // 상품 목록/상세 조회(GET)는 로그인 안 해도 볼 수 있어야 하는 화면이라 허용
                    .requestMatchers(HttpMethod.GET, "/api/products/**")
                    .permitAll()
                    // 리뷰 작성/삭제는 로그인한 회원이면 누구나 가능 (ADMIN 권한까지는 필요 없음)
                    .requestMatchers(HttpMethod.POST, "/api/products/*/reviews")
                    .authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/products/*/reviews/*")
                    .authenticated()
                    // 상품 등록/수정/삭제는 관리자만 가능. hasRole("ADMIN")은 로그인 사용자의
                    // 권한 목록에 "ROLE_ADMIN"이 있는지 확인한다 (ROLE_ 접두사는 자동으로 붙음).
                    .requestMatchers(HttpMethod.POST, "/api/products/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/products/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                    .hasRole("ADMIN")
                    // 관리자 전용 API 전체도 동일하게 ADMIN 권한 필요
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    // 위 규칙에 안 걸린 나머지 모든 요청은 "로그인만 하면" 접근 가능
                    .anyRequest()
                    .authenticated());
    // build()를 호출해야 지금까지 이어붙인 설정들이 실제 SecurityFilterChain 객체로 완성된다.
    return http.build();
  }
}
