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
import com.kdt.shoppingmall.dto.member.DeleteRequest;
import com.kdt.shoppingmall.dto.member.MemberResponse;
import com.kdt.shoppingmall.dto.member.MemberUpdateRequest;
import com.kdt.shoppingmall.exception.GlobalExceptionHandler;
import com.kdt.shoppingmall.exception.PasswordMismatchException;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
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
  void 비밀번호_틀림_400() throws Exception {
    // PasswordMismatchException은 400(BAD_REQUEST)을 반환한다.
    // 401을 쓰면 axios 인터셉터가 세션 만료로 오인해 강제 로그아웃시키기 때문이다.
    MemberUpdateRequest request = new MemberUpdateRequest(null, "wrongPass", "newPass123");
    willThrow(new PasswordMismatchException("현재 비밀번호가 올바르지 않습니다."))
        .given(memberService)
        .update(eq(1L), any());

    mockMvc
        .perform(
            put("/api/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
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
    DeleteRequest request = new DeleteRequest("password123");
    willDoNothing().given(memberService).delete(eq(1L), any());

    mockMvc
        .perform(
            delete("/api/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockMemberPrincipal
  void 회원탈퇴_비밀번호_누락_400() throws Exception {
    // password 필드 없이 빈 JSON 요청 시 @NotBlank 검증에 걸려 400이어야 한다
    mockMvc
        .perform(delete("/api/members/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockMemberPrincipal
  void 회원탈퇴_비밀번호_불일치_400() throws Exception {
    // 비밀번호가 틀리면 서비스에서 PasswordMismatchException → 400(BAD_REQUEST).
    // 401을 쓰면 axios 인터셉터가 세션 만료로 오인해 강제 로그아웃시키기 때문이다.
    DeleteRequest request = new DeleteRequest("wrongPass");
    willThrow(new PasswordMismatchException("현재 비밀번호가 올바르지 않습니다."))
        .given(memberService)
        .delete(eq(1L), any());

    mockMvc
        .perform(
            delete("/api/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("현재 비밀번호가 올바르지 않습니다."));
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

  @Test
  void 미로그인_탈퇴요청_401() throws Exception {
    // 인증 없이 DELETE /api/members/me 요청 시 401이 반환되어야 한다
    mockMvc.perform(delete("/api/members/me")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockMemberPrincipal
  void 존재하지않는_회원_수정요청_404() throws Exception {
    // 서비스에서 ResourceNotFoundException이 발생하면 404로 응답해야 한다
    MemberUpdateRequest request = new MemberUpdateRequest("새이름", null, null);
    willThrow(new ResourceNotFoundException("존재하지 않는 회원입니다."))
        .given(memberService)
        .update(eq(1L), any());

    mockMvc
        .perform(
            put("/api/members/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("존재하지 않는 회원입니다."));
  }
}
