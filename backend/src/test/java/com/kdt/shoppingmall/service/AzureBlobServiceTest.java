package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.kdt.shoppingmall.exception.UnsupportedFileTypeException;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

// @ExtendWith(MockitoExtension.class): JUnit 5에서 Mockito를 쓰기 위한 선언.
// 이 어노테이션이 있어야 @Mock, @InjectMocks 가 동작한다.
@ExtendWith(MockitoExtension.class)
class AzureBlobServiceTest {

  // @Mock: 실제 Azure SDK 대신 가짜(Mock) 객체를 만든다.
  // 실제 Azure 계정 없이도 테스트할 수 있고, 특정 메서드가 호출됐는지 검증할 수 있다.
  @Mock private BlobContainerClient containerClient;

  // BlobContainerClient.getBlobClient()가 반환하는 BlobClient도 Mock으로 만든다.
  @Mock private BlobClient blobClient;

  // @InjectMocks: AzureBlobService 생성자에 @Mock으로 만든 containerClient를 주입해준다.
  // 생성자 파라미터 타입이 BlobContainerClient 하나뿐이라 Mockito가 자동으로 매핑한다.
  @InjectMocks private AzureBlobService azureBlobService;

  @Test
  void 업로드_성공_시_Blob_URL을_반환한다() throws IOException {
    // given
    String expectedUrl = "https://example.blob.core.windows.net/product-images/uuid_test.jpg";
    given(containerClient.getBlobClient(any(String.class))).willReturn(blobClient);
    given(blobClient.getBlobUrl()).willReturn(expectedUrl);

    MultipartFile file = mockFile("test.jpg", 100L, "image/jpeg");

    // when
    String result = azureBlobService.upload(file);

    // then
    assertThat(result).isEqualTo(expectedUrl);
    verify(blobClient).uploadWithResponse(any(BlobParallelUploadOptions.class), any());
  }

  // ─── DEF-11 회귀 테스트 ────────────────────────────────────────────
  // 2026-08-02 배포 검증: 업로드된 Blob이 전부 application/octet-stream으로 서빙되고 있었다.
  // 원인은 upload()가 파일 내용만 보내고 "이게 무슨 타입인지"는 안 알려준 것.
  // Azure는 Content-Type을 안 주면 octet-stream(= 정체불명 바이트 덩어리)을 기본값으로 쓴다.
  @Test
  void 업로드할_때_파일의_Content_Type이_Blob_헤더로_함께_전달된다() throws IOException {
    // given
    given(containerClient.getBlobClient(any(String.class))).willReturn(blobClient);
    given(blobClient.getBlobUrl())
        .willReturn("https://example.blob.core.windows.net/product-images/uuid_photo.png");

    MultipartFile file = mockFile("photo.png", 300L, "image/png");

    // when
    azureBlobService.upload(file);

    // then — Azure SDK에 넘긴 업로드 옵션 안에 Content-Type이 들어있어야 한다
    BlobParallelUploadOptions options = captureUploadOptions();
    assertThat(options.getHeaders()).isNotNull();
    assertThat(options.getHeaders().getContentType()).isEqualTo("image/png");
  }

  @Test
  void 파일명_앞에_UUID가_붙어서_Blob_이름이_결정된다() throws IOException {
    // given
    // ArgumentCaptor: mock 호출 시 실제로 어떤 인자가 전달됐는지 캡처해서 검증할 수 있다.
    ArgumentCaptor<String> blobNameCaptor = ArgumentCaptor.forClass(String.class);
    given(containerClient.getBlobClient(blobNameCaptor.capture())).willReturn(blobClient);
    given(blobClient.getBlobUrl())
        .willReturn("https://example.blob.core.windows.net/product-images/uuid_photo.png");

    MultipartFile file = mockFile("photo.png", 200L, "image/png");

    // when
    azureBlobService.upload(file);

    // then — 전달된 blobName이 "UUID_원본파일명" 형식인지 검증
    String actualBlobName = blobNameCaptor.getValue();
    assertThat(actualBlobName).endsWith("_photo.png");
    // UUID 형식(8-4-4-4-12 자리 16진수): xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
    assertThat(actualBlobName).matches("[0-9a-f\\-]{36}_photo\\.png");
  }

  @Test
  void 파일_크기가_업로드_옵션에_그대로_전달된다() throws IOException {
    // given
    long fileSize = 512L;
    given(containerClient.getBlobClient(any(String.class))).willReturn(blobClient);
    given(blobClient.getBlobUrl())
        .willReturn("https://example.blob.core.windows.net/product-images/uuid_large.jpg");

    MultipartFile file = mockFile("large.jpg", fileSize, "image/jpeg");

    // when
    azureBlobService.upload(file);

    // then — Azure SDK에 파일 크기가 정확히 전달됐는지 확인
    assertThat(captureUploadOptions().getOptionalLength()).isEqualTo(fileSize);
  }

  // ─── 허용 목록(화이트리스트) 검증 ─────────────────────────────────
  // Content-Type을 클라이언트가 준 값 그대로 Blob에 붙이게 됐으므로,
  // "무슨 값이든 붙는" 상태가 되지 않도록 서버가 직접 걸러야 한다.
  // 프론트(imageUpload.js)에도 같은 검사가 있지만 그건 실수 방지용이고,
  // curl로 API를 직접 부르면 통째로 건너뛴다 — 진짜 방어선은 여기다.
  @Test
  void 이미지가_아닌_타입은_업로드하지_않고_예외를_던진다() {
    // given — 공개 컨테이너에 HTML이 올라가면 그 도메인에서 스크립트를 띄울 수 있다
    MultipartFile file = mockTypeOnlyFile("text/html");

    // when & then
    assertThatThrownBy(() -> azureBlobService.upload(file))
        .isInstanceOf(UnsupportedFileTypeException.class)
        .hasMessageContaining("이미지 파일만");

    // Azure에 요청 자체가 나가지 않아야 한다 — 거부는 "올린 뒤 지우는" 게 아니라 "안 올리는" 것이다
    verifyNoInteractions(containerClient);
  }

  @Test
  void SVG는_이미지지만_허용하지_않는다() {
    // given — SVG는 내부에 <script>를 담을 수 있는 "실행 가능한 이미지"라 의도적으로 목록에서 뺐다
    MultipartFile file = mockTypeOnlyFile("image/svg+xml");

    // when & then
    assertThatThrownBy(() -> azureBlobService.upload(file))
        .isInstanceOf(UnsupportedFileTypeException.class);
    verifyNoInteractions(containerClient);
  }

  @Test
  void Content_Type이_없는_파일도_거부한다() {
    // given — 멀티파트 요청에 타입 정보가 없으면 getContentType()이 null이다.
    //         이 테스트가 실제로 버그를 잡았다: 처음엔 null 검사 없이 contains(contentType)만
    //         호출했는데, Set.of(...)의 contains(null)은 false가 아니라 NPE를 던진다.
    //         NPE는 GlobalExceptionHandler에서 500이 되므로 "잘못된 파일"이 "서버 장애"로 둔갑한다.
    MultipartFile file = mockTypeOnlyFile(null);

    // when & then
    assertThatThrownBy(() -> azureBlobService.upload(file))
        .isInstanceOf(UnsupportedFileTypeException.class);
    verifyNoInteractions(containerClient);
  }

  // Azure SDK에 실제로 넘어간 업로드 옵션 객체를 꺼내오는 헬퍼.
  // 세 테스트가 같은 캡처 코드를 쓰기에 한 곳으로 모았다.
  private BlobParallelUploadOptions captureUploadOptions() {
    ArgumentCaptor<BlobParallelUploadOptions> captor =
        ArgumentCaptor.forClass(BlobParallelUploadOptions.class);
    verify(blobClient).uploadWithResponse(captor.capture(), any());
    return captor.getValue();
  }

  // 거부 케이스 전용 Mock — 타입만 지정한다.
  // 파일명·크기·스트림까지 스텁하면 Mockito가 "쓰이지 않은 스텁"이라며 테스트를 실패시킨다
  // (@ExtendWith(MockitoExtension.class)의 기본 STRICT_STUBS 모드).
  // 거부되면 그 값들을 읽기 전에 예외가 나가므로, 안 쓰는 스텁은 애초에 만들지 않는다.
  private MultipartFile mockTypeOnlyFile(String contentType) {
    MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
    given(file.getContentType()).willReturn(contentType);
    return file;
  }

  // 테스트용 MultipartFile Mock 헬퍼 메서드
  private MultipartFile mockFile(String originalFilename, long size, String contentType)
      throws IOException {
    MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
    given(file.getOriginalFilename()).willReturn(originalFilename);
    given(file.getInputStream()).willReturn(InputStream.nullInputStream());
    given(file.getSize()).willReturn(size);
    given(file.getContentType()).willReturn(contentType);
    return file;
  }
}
