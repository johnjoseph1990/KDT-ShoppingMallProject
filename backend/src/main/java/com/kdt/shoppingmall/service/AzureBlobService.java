package com.kdt.shoppingmall.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import java.io.IOException;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// Azure Blob Storage에 파일을 업로드하고 공개 URL을 반환하는 서비스.
// @ConditionalOnBean: AzureStorageConfig가 BlobContainerClient 빈을 만들었을 때만
// 이 서비스 빈도 생성된다. 환경변수 미설정 시 빈이 없어도 앱이 정상 기동된다.
@Service
@ConditionalOnBean(BlobContainerClient.class)
public class AzureBlobService {

  // BlobContainerClient를 직접 만들지 않고 AzureStorageConfig가 만든 빈을 주입받는다.
  // 이렇게 하면 테스트에서 Mock BlobContainerClient를 주입해 실제 Azure 없이 단위 테스트 가능.
  private final BlobContainerClient containerClient;

  public AzureBlobService(BlobContainerClient containerClient) {
    this.containerClient = containerClient;
  }

  /**
   * 이미지 파일을 Azure Blob Storage에 업로드하고 공개 URL을 반환한다.
   *
   * <p>파일명 충돌을 막기 위해 UUID를 앞에 붙인다 (예: uuid_원본파일명.jpg).
   */
  public String upload(MultipartFile file) throws IOException {
    // UUID로 파일명 중복 방지
    String blobName = UUID.randomUUID() + "_" + file.getOriginalFilename();
    BlobClient blobClient = containerClient.getBlobClient(blobName);
    // overwrite=true: 동일 이름 blob이 이미 있으면 덮어쓴다 (UUID 사용 시 사실상 발생 안 함)
    blobClient.upload(file.getInputStream(), file.getSize(), true);
    return blobClient.getBlobUrl();
  }
}
