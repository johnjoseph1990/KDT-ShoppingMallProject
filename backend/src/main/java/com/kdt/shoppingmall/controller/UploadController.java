package com.kdt.shoppingmall.controller;

import com.kdt.shoppingmall.service.AzureBlobService;
import java.io.IOException;
import java.util.Map;
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

  private final AzureBlobService azureBlobService;

  public UploadController(AzureBlobService azureBlobService) {
    this.azureBlobService = azureBlobService;
  }

  // @RequestParam("file"): multipart/form-data 요청에서 "file" 필드를 꺼낸다
  @PostMapping("/upload")
  public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file)
      throws IOException {
    String url = azureBlobService.upload(file);
    // { "url": "https://xxx.blob.core.windows.net/product-images/uuid_파일명" }
    return ResponseEntity.ok(Map.of("url", url));
  }
}
