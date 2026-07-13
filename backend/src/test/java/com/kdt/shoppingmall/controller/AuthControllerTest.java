package com.kdt.shoppingmall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.shoppingmall.config.SecurityConfig;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.dto.member.MemberResponse;
import com.kdt.shoppingmall.dto.member.SignupRequest;
import com.kdt.shoppingmall.exception.DuplicateEmailException;
import com.kdt.shoppingmall.exception.GlobalExceptionHandler;
import com.kdt.shoppingmall.security.MemberUserDetailsService;
import com.kdt.shoppingmall.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
}
