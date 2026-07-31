package com.kdt.shoppingmall.controller;

import com.kdt.shoppingmall.service.AzureBlobService;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// 이미지 업로드 전용 컨트롤러.
// /api/admin/** 경로는 SecurityConfig에서 ADMIN 역할만 접근 가능하도록 설정되어 있어
// 별도의 @PreAuthorize 없이도 관리자만 호출할 수 있다.
@RestController
@RequestMapping("/api/admin")
public class UploadController {

  // required = false: AZURE_STORAGE_CONNECTION_STRING 환경변수 미설정 시
  // AzureBlobService 빈이 없어도 컨트롤러 자체는 정상 기동된다.
  // 이 경우 upload() 호출 시 503을 반환한다.
  private final AzureBlobService azureBlobService;

  public UploadController(@Autowired(required = false) AzureBlobService azureBlobService) {
    this.azureBlobService = azureBlobService;
  }

  // @RequestParam("file"): multipart/form-data 요청에서 "file" 필드를 꺼낸다
  @PostMapping("/upload")
  public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file)
      throws IOException {
    if (azureBlobService == null) {
      // Azure 환경변수가 설정되지 않은 경우 — 운영 환경에서는 발생하지 않아야 한다
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("message", "이미지 업로드 기능이 구성되지 않았습니다."));
    }
    String url = azureBlobService.upload(file);
    // { "url": "https://xxx.blob.core.windows.net/product-images/uuid_파일명" }
    return ResponseEntity.ok(Map.of("url", url));
  }
}
