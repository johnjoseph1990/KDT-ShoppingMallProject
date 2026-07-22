package com.kdt.shoppingmall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.shoppingmall.config.SecurityConfig;
import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.dto.member.LoginRequest;
import com.kdt.shoppingmall.dto.member.MemberResponse;
import com.kdt.shoppingmall.dto.member.SignupRequest;
import com.kdt.shoppingmall.exception.DuplicateEmailException;
import com.kdt.shoppingmall.exception.GlobalExceptionHandler;
import com.kdt.shoppingmall.security.MemberPrincipal;
import com.kdt.shoppingmall.security.MemberUserDetailsService;
import com.kdt.shoppingmall.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private MemberService memberService;

  @MockitoBean private MemberUserDetailsService memberUserDetailsService;

  @Test
  void 회원가입_성공_201() throws Exception {
    SignupRequest request = new SignupRequest("test@test.com", "password123", "테스터");
    MemberResponse response = new MemberResponse(1L, "test@test.com", "테스터", MemberRole.USER);
    given(memberService.signup(any())).willReturn(response);

    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("test@test.com"))
        .andExpect(jsonPath("$.role").value("USER"));
  }

  @Test
  void 회원가입_이메일형식오류_400() throws Exception {
    SignupRequest request = new SignupRequest("invalid-email", "password123", "테스터");

    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 회원가입_비밀번호짧음_400() throws Exception {
    SignupRequest request = new SignupRequest("test@test.com", "short", "테스터");

    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 회원가입_중복이메일_409() throws Exception {
    SignupRequest request = new SignupRequest("dup@test.com", "password123", "중복자");
    given(memberService.signup(any()))
        .willThrow(new DuplicateEmailException("이미 가입된 이메일입니다: dup@test.com"));

    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다: dup@test.com"));
  }

  // AuthController.login()은 AuthenticationManager를 직접 호출한다.
  // 이 매니저는 (SecurityConfig에서) MemberUserDetailsService로 회원을 조회한 뒤
  // 비밀번호를 비교하므로, 로그인 테스트는 MemberService가 아니라
  // memberUserDetailsService.loadUserByUsername()을 스텁해야 한다.
  @Test
  void 로그인_성공_200() throws Exception {
    String rawPassword = "test1234";
    // 실제 로그인 흐름과 동일하게 BCrypt로 암호화한 값을 "DB에 저장된 비밀번호"인 것처럼 반환한다.
    Member member =
        new Member(
            "test@shop.com",
            new BCryptPasswordEncoder().encode(rawPassword),
            "테스터",
            MemberRole.USER);
    given(memberUserDetailsService.loadUserByUsername("test@shop.com"))
        .willReturn(new MemberPrincipal(member));

    LoginRequest request = new LoginRequest("test@shop.com", rawPassword);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@shop.com"));
  }

  @Test
  void 로그인_실패_비밀번호틀림_401() throws Exception {
    Member member =
        new Member(
            "test@shop.com",
            new BCryptPasswordEncoder().encode("test1234"),
            "테스터",
            MemberRole.USER);
    given(memberUserDetailsService.loadUserByUsername("test@shop.com"))
        .willReturn(new MemberPrincipal(member));

    LoginRequest request = new LoginRequest("test@shop.com", "wrongpassword");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // 인증 자체가 실패한 상황이므로 401 (SecurityConfig의 authenticationEntryPoint 참고)
        .andExpect(status().isUnauthorized());
  }

  @Test
  void 로그인_실패_없는이메일_401() throws Exception {
    given(memberUserDetailsService.loadUserByUsername("nobody@shop.com"))
        .willThrow(new UsernameNotFoundException("가입되지 않은 이메일입니다: nobody@shop.com"));

    LoginRequest request = new LoginRequest("nobody@shop.com", "whatever123");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // 비밀번호 틀림과 동일하게 401. 이메일 존재 여부를 노출하지 않기 위함(3장 참고).
        .andExpect(status().isUnauthorized());
  }
}
