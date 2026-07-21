package com.kdt.shoppingmall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.shoppingmall.config.SecurityConfig;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.dto.member.MemberResponse;
import com.kdt.shoppingmall.dto.member.MemberUpdateRequest;
import com.kdt.shoppingmall.exception.GlobalExceptionHandler;
import com.kdt.shoppingmall.exception.PasswordMismatchException;
import com.kdt.shoppingmall.security.MemberUserDetailsService;
import com.kdt.shoppingmall.service.MemberService;
import com.kdt.shoppingmall.support.WithMockMemberPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MemberController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class MemberControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private MemberService memberService;

  @MockitoBean private MemberUserDetailsService memberUserDetailsService;

  @Test
  @WithMockMemberPrincipal
  void 이름_수정_성공_200() throws Exception {
    MemberUpdateRequest request = new MemberUpdateRequest("새이름", null, null);
    MemberResponse response = new MemberResponse(1L, "test@test.com", "새이름", MemberRole.USER);
    given(memberService.update(eq(1L), any())).willReturn(response);

    mockMvc
        .perform(
            put("/api/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("새이름"));
  }

  @Test
  @WithMockMemberPrincipal
  void 비밀번호_틀림_401() throws Exception {
    MemberUpdateRequest request = new MemberUpdateRequest(null, "wrongPass", "newPass123");
    willThrow(new PasswordMismatchException("현재 비밀번호가 올바르지 않습니다."))
        .given(memberService)
        .update(eq(1L), any());

    mockMvc
        .perform(
            put("/api/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("현재 비밀번호가 올바르지 않습니다."));
  }

  @Test
  @WithMockMemberPrincipal
  void 새비밀번호_8자미만_400() throws Exception {
    MemberUpdateRequest request = new MemberUpdateRequest(null, "currentPass", "short");

    mockMvc
        .perform(
            put("/api/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockMemberPrincipal
  void 회원탈퇴_성공_204() throws Exception {
    willDoNothing().given(memberService).delete(1L);

    mockMvc.perform(delete("/api/members/me")).andExpect(status().isNoContent());
  }

  @Test
  void 미로그인_수정요청_401() throws Exception {
    MemberUpdateRequest request = new MemberUpdateRequest("새이름", null, null);

    mockMvc
        .perform(
            put("/api/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }
}
