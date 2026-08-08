package com.kdt.shoppingmall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kdt.shoppingmall.config.SecurityConfig;
import com.kdt.shoppingmall.exception.GlobalExceptionHandler;
import com.kdt.shoppingmall.exception.UnsupportedFileTypeException;
import com.kdt.shoppingmall.security.MemberUserDetailsService;
import com.kdt.shoppingmall.service.AzureBlobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

// @WebMvcTest: UploadController 레이어만 띄운다 (JPA, DB 연결 없음).
// SecurityConfig를 @Import해야 /api/admin/** → ADMIN 권한 체크가 실제로 동작한다.
@WebMvcTest(UploadController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UploadControllerTest {

  @Autowired private MockMvc mockMvc;

  // @MockitoBean: 실제 Azure 연결 없이 AzureBlobService를 가짜로 대체한다.
  // @ConditionalOnBean(AzureBlobService.class)가 붙은 UploadController는
  // 이 Mock 빈이 있으면 조건을 만족해서 정상 등록된다.
  @MockitoBean private AzureBlobService azureBlobService;

  // SecurityConfig가 MemberUserDetailsService를 주입받으므로 Mock으로 제공해야 컨텍스트가 뜬다.
  @MockitoBean private MemberUserDetailsService memberUserDetailsService;

  @Test
  @WithMockUser(roles = "ADMIN")
  void ADMIN이_이미지를_업로드하면_URL을_반환한다() throws Exception {
    // given
    String uploadedUrl = "https://example.blob.core.windows.net/product-images/uuid_test.jpg";
    given(azureBlobService.upload(any(MultipartFile.class))).willReturn(uploadedUrl);

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "imagedata".getBytes());

    // when & then
    mockMvc
        .perform(multipart("/api/admin/upload").file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(uploadedUrl));
  }

  @Test
  @WithMockUser(roles = "USER")
  void 일반_USER가_업로드하면_403을_반환한다() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "imagedata".getBytes());

    mockMvc.perform(multipart("/api/admin/upload").file(file)).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void 이미지가_아닌_파일을_업로드하면_400을_반환한다() throws Exception {
    // given — AzureBlobService가 이미지가 아닌 파일을 거부하는 상황을 재현한다.
    // willThrow: 이 Mock 메서드가 호출되면 값을 돌려주는 대신 예외를 던지게 한다.
    given(azureBlobService.upload(any(MultipartFile.class)))
        .willThrow(new UnsupportedFileTypeException("이미지 파일만 업로드할 수 있습니다."));

    MockMultipartFile file =
        new MockMultipartFile("file", "malware.exe", "application/octet-stream", "data".getBytes());

    // when & then — 예외 자체는 AzureBlobServiceTest가 이미 검증한다.
    // 여기서 확인하는 것은 "그 예외가 HTTP 400 + message 형태로 나가는가"라는 API 계약이다.
    // 프론트의 uploadErrorMessage()가 이 message를 그대로 화면에 띄우므로 body까지 본다.
    mockMvc
        .perform(multipart("/api/admin/upload").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("이미지 파일만 업로드할 수 있습니다."));
  }

  @Test
  void 비인증_사용자가_업로드하면_401을_반환한다() throws Exception {
    // SecurityConfig에서 authenticationEntryPoint를 HttpStatus.UNAUTHORIZED로 설정했으므로
    // 세션 없이 접근하면 기본값 403이 아닌 401이 반환된다.
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "imagedata".getBytes());

    mockMvc.perform(multipart("/api/admin/upload").file(file)).andExpect(status().isUnauthorized());
  }
}
