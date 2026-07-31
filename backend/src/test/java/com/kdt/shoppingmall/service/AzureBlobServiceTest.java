package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
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

    MultipartFile file = mockFile("test.jpg", 100L);

    // when
    String result = azureBlobService.upload(file);

    // then
    assertThat(result).isEqualTo(expectedUrl);
    // overwrite=true 로 upload가 호출됐는지 확인
    verify(blobClient).upload(any(InputStream.class), anyLong(), anyBoolean());
  }

  @Test
  void 파일명_앞에_UUID가_붙어서_Blob_이름이_결정된다() throws IOException {
    // given
    // ArgumentCaptor: mock 호출 시 실제로 어떤 인자가 전달됐는지 캡처해서 검증할 수 있다.
    ArgumentCaptor<String> blobNameCaptor = ArgumentCaptor.forClass(String.class);
    given(containerClient.getBlobClient(blobNameCaptor.capture())).willReturn(blobClient);
    given(blobClient.getBlobUrl())
        .willReturn("https://example.blob.core.windows.net/product-images/uuid_photo.png");

    MultipartFile file = mockFile("photo.png", 200L);

    // when
    azureBlobService.upload(file);

    // then — 전달된 blobName이 "UUID_원본파일명" 형식인지 검증
    String actualBlobName = blobNameCaptor.getValue();
    assertThat(actualBlobName).endsWith("_photo.png");
    // UUID 형식(8-4-4-4-12 자리 16진수): xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
    assertThat(actualBlobName).matches("[0-9a-f\\-]{36}_photo\\.png");
  }

  @Test
  void 파일_크기가_upload_메서드에_그대로_전달된다() throws IOException {
    // given
    long fileSize = 512L;
    ArgumentCaptor<Long> sizeCaptor = ArgumentCaptor.forClass(Long.class);
    given(containerClient.getBlobClient(any(String.class))).willReturn(blobClient);
    given(blobClient.getBlobUrl())
        .willReturn("https://example.blob.core.windows.net/product-images/uuid_large.jpg");

    MultipartFile file = mockFile("large.jpg", fileSize);

    // when
    azureBlobService.upload(file);

    // then — Azure SDK에 파일 크기가 정확히 전달됐는지 확인
    verify(blobClient).upload(any(InputStream.class), sizeCaptor.capture(), anyBoolean());
    assertThat(sizeCaptor.getValue()).isEqualTo(fileSize);
  }

  // 테스트용 MultipartFile Mock 헬퍼 메서드
  private MultipartFile mockFile(String originalFilename, long size) throws IOException {
    MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
    given(file.getOriginalFilename()).willReturn(originalFilename);
    given(file.getInputStream()).willReturn(InputStream.nullInputStream());
    given(file.getSize()).willReturn(size);
    return file;
  }
}
