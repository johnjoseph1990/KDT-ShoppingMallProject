package com.kdt.shoppingmall.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GlobalExceptionHandler 전용 테스트.
 *
 * <p>왜 별도 클래스가 필요한가: 아래에서 검증하는 핸들러들(DataIntegrityViolation, HttpMessageNotReadable, Exception)은 특정
 * 컨트롤러에 속하지 않는 "횡단 관심사"다. 예를 들어 "깨진 JSON을 보내면 400" 같은 계약은 OrderControllerTest에 넣기도,
 * ProductControllerTest에 넣기도 어색하다. 그래서 어느 컨트롤러에도 딸리지 않은 채 아무도 검증하지 않는 상태로 남아 있었다.
 *
 * <p>standaloneSetup을 쓰는 이유: 스프링 컨텍스트 전체를 띄우지 않고 "가짜 컨트롤러 + 검증 대상 핸들러"만 조립하는 MockMvc 구성 방식이다. 원하는
 * 예외를 마음대로 던지는 프로브 엔드포인트를 만들 수 있고, 스프링 시큐리티 필터가 끼어들지 않아 핸들러 동작만 순수하게 확인할 수 있다.
 *
 * <p><b>이 클래스가 커버하지 "못하는" 것 — 지우기 전에 반드시 읽을 것.</b> standaloneSetup은 자기만의 예외 리졸버 체인을 만들기 때문에, 실제 앱의
 * <b>리졸버 실행 순서</b>는 재현하지 않는다. 즉 여기 테스트는 핸들러의 "몸통"(상태코드·메시지 형태)만 검증한다. Exception.class 캐치올이 스프링 MVC
 * 자체 예외를 500으로 가로채던 결함(400/405)은 이 클래스가 아니라 ProductControllerTest의 프로브 2건(실제 @WebMvcTest)이 막고 있다. 그
 * 두 테스트를 "여기서 이미 커버한다"고 오해해 지우면 결함이 조용히 되살아난다.
 */
class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    // setControllerAdvice(): 이 MockMvc가 예외를 만나면 GlobalExceptionHandler에게 처리를 맡기도록 등록한다.
    // 실제 앱에서 @RestControllerAdvice가 자동 등록되는 것을 수동으로 재현하는 것.
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  // ── 1. 여태 한 번도 검증되지 않았던 횡단 핸들러 3건 ──────────────────────────

  @Test
  @DisplayName("DB 유니크 제약 충돌(DataIntegrityViolation)은 409와 안내 문구를 반환한다")
  void DB제약충돌은_409를_반환한다() throws Exception {
    // 서비스 단 중복 선검증(existsBy...)을 동시 요청 두 개가 나란히 통과한 뒤
    // DB 유니크 제약에서 충돌하는 경쟁 조건에서만 오는 경로다.
    // 재현이 어려워 여태 미검증이었지만, 응답 계약 자체는 여기서 고정할 수 있다.
    mockMvc
        .perform(get("/probe/data-integrity"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("이미 처리된 요청입니다. 잠시 후 다시 시도해주세요."));
  }

  @Test
  @DisplayName("JSON 문법이 깨진 요청은 400을 반환한다")
  void 깨진JSON은_400을_반환한다() throws Exception {
    // @Valid 검증(MethodArgumentNotValidException)보다 앞 단계에서 터진다.
    // 본문을 객체로 바꾸는 것 자체가 실패하므로 필드 검증까지 가지도 못한다.
    mockMvc
        .perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\": "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다. JSON 형식을 확인해주세요."));
  }

  @Test
  @DisplayName("예상 못 한 예외는 500을 반환하되 내부 정보를 노출하지 않는다")
  void 예상못한예외는_500이고_내부정보를_숨긴다() throws Exception {
    // 이 프로젝트 테스트 전체에서 5xx를 검증하는 첫 테스트다.
    // 핵심은 "500이 나온다"가 아니라 "스택 트레이스·내부 메시지가 응답에 새지 않는다"이다.
    mockMvc
        .perform(get("/probe/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."))
        // 응답 "전체"에 내부 예외 메시지가 섞이지 않았는지까지 본다.
        // DB 접속 문자열이 그대로 나가면 심각한 정보 노출이기 때문.
        .andExpect(content().string(not(containsString("jdbc:postgresql"))));
  }

  // ── 2. 핸들러엔 도달했지만 분기·응답 body가 미검증이던 것들 ──────────────────

  @Test
  @DisplayName("스프링 시큐리티의 영문 메시지는 한국어 안내로 교체한다")
  void 영문_AccessDenied_메시지는_한국어로_교체된다() throws Exception {
    // @PreAuthorize(메서드 보안)가 거부하면 영문 "Access Denied"가 담긴 예외가 올라온다.
    // 이 영문을 그대로 사용자에게 보여주지 않는다는 것이 이 분기의 목적.
    mockMvc
        .perform(get("/probe/access-denied").param("message", "Access Denied"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
  }

  @Test
  @DisplayName("서비스가 직접 던진 한국어 메시지는 그대로 전달한다")
  void 한국어_AccessDenied_메시지는_그대로_전달된다() throws Exception {
    // 반대 방향 검증. 치환 로직이 과하게 동작해서 서비스가 정성껏 쓴 안내 문구까지
    // "접근 권한이 없습니다."로 뭉개버리면 안 된다.
    mockMvc
        .perform(get("/probe/access-denied").param("message", "구매자만 리뷰를 작성할 수 있습니다."))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("구매자만 리뷰를 작성할 수 있습니다."));
  }

  @Test
  @DisplayName("@Valid 실패 메시지는 '필드명: 사유' 형태로 가공된다")
  void 유효성검증_실패메시지는_필드명을_포함한다() throws Exception {
    // 기존 400 테스트 8건은 전부 상태코드만 확인하고 body는 보지 않았다.
    // 그래서 "field + ': ' + message" 가공 로직이 깨져도 아무도 몰랐다.
    mockMvc
        .perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("name: 이름은 필수입니다"));
  }

  // ── 프로브용 가짜 컨트롤러 ────────────────────────────────────────────────
  // 실제 서비스 코드가 아니라, 원하는 예외를 원하는 순간에 던지기 위한 테스트 전용 컨트롤러다.

  @RestController
  static class ProbeController {

    @GetMapping("/probe/data-integrity")
    void throwDataIntegrity() {
      throw new DataIntegrityViolationException("uk_member_email 제약 위반");
    }

    @GetMapping("/probe/boom")
    void throwUnexpected() {
      // 내부 정보(가짜 DB 접속 문자열)를 일부러 메시지에 넣어, 이게 응답으로 새지 않는지 확인한다.
      throw new IllegalStateException("연결 실패: jdbc:postgresql://localhost:5433/shop");
    }

    @GetMapping("/probe/access-denied")
    void throwAccessDenied(@RequestParam String message) {
      throw new AccessDeniedException(message);
    }

    @PostMapping("/probe/body")
    void acceptBody(@Valid @RequestBody ProbeRequest request) {}
  }

  // @NotBlank: 값이 null이거나 공백뿐이면 검증 실패. message는 사용자에게 보일 사유 문구다.
  record ProbeRequest(@NotBlank(message = "이름은 필수입니다") String name) {}
}
