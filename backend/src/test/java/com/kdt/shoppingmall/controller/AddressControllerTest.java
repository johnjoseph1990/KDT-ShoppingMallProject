package com.kdt.shoppingmall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.shoppingmall.config.SecurityConfig;
import com.kdt.shoppingmall.dto.address.AddressRequest;
import com.kdt.shoppingmall.dto.address.AddressResponse;
import com.kdt.shoppingmall.exception.GlobalExceptionHandler;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.security.MemberUserDetailsService;
import com.kdt.shoppingmall.service.AddressService;
import com.kdt.shoppingmall.support.WithMockMemberPrincipal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AddressController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AddressControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AddressService addressService;

  @MockitoBean private MemberUserDetailsService memberUserDetailsService;

  private AddressRequest validRequest() {
    return new AddressRequest("홍길동", "010-1234-5678", "12345", "서울시 강남구", "101호", "문 앞", false);
  }

  @Test
  @WithMockMemberPrincipal
  void 배송지목록_조회_200() throws Exception {
    AddressResponse response =
        new AddressResponse(1L, "홍길동", "010-1234-5678", "12345", "서울시 강남구", "101호", "문 앞", false);
    given(addressService.getAll(1L)).willReturn(List.of(response));

    mockMvc
        .perform(get("/api/addresses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].recipientName").value("홍길동"));
  }

  @Test
  void 미로그인_배송지조회_401() throws Exception {
    mockMvc.perform(get("/api/addresses")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockMemberPrincipal
  void 배송지_추가_201() throws Exception {
    AddressResponse response =
        new AddressResponse(1L, "홍길동", "010-1234-5678", "12345", "서울시 강남구", "101호", "문 앞", false);
    given(addressService.create(eq(1L), any())).willReturn(response);

    mockMvc
        .perform(
            post("/api/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L));
  }

  @Test
  @WithMockMemberPrincipal
  void 배송지_추가_필수필드누락_400() throws Exception {
    // recipientName이 비어있으면 @Valid에 의해 400이 반환되어야 한다
    AddressRequest invalidRequest =
        new AddressRequest("", "010-1234-5678", "12345", "서울시", null, null, false);

    mockMvc
        .perform(
            post("/api/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockMemberPrincipal
  void 배송지_수정_200() throws Exception {
    AddressResponse response =
        new AddressResponse(1L, "수정이름", "010-0000-0000", "99999", "인천시", null, null, false);
    given(addressService.update(eq(1L), eq(1L), any())).willReturn(response);

    mockMvc
        .perform(
            put("/api/addresses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recipientName").value("수정이름"));
  }

  @Test
  @WithMockMemberPrincipal
  void 존재하지않는_배송지_수정_404() throws Exception {
    willThrow(new ResourceNotFoundException("존재하지 않는 배송지입니다."))
        .given(addressService)
        .update(eq(1L), eq(99L), any());

    mockMvc
        .perform(
            put("/api/addresses/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("존재하지 않는 배송지입니다."));
  }

  @Test
  @WithMockMemberPrincipal
  void 배송지_삭제_204() throws Exception {
    willDoNothing().given(addressService).delete(1L, 1L);

    mockMvc.perform(delete("/api/addresses/1")).andExpect(status().isNoContent());
  }

  @Test
  void 미로그인_배송지삭제_401() throws Exception {
    mockMvc.perform(delete("/api/addresses/1")).andExpect(status().isUnauthorized());
  }
}
