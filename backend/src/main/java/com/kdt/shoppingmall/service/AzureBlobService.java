package com.kdt.shoppingmall.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// Azure Blob Storage에 파일을 업로드하고 공개 URL을 반환하는 서비스.
// @Service: 스프링이 이 클래스를 빈으로 등록해 다른 곳에서 @Autowired/생성자 주입으로 쓸 수 있게 한다.
@Service
public class AzureBlobService {

  // BlobContainerClient: 특정 컨테이너에 대한 작업(업로드/다운로드/삭제)을 담당하는 Azure SDK 클라이언트
  private final BlobContainerClient containerClient;

  public AzureBlobService(
      // @Value: application.properties의 값을 생성자 파라미터로 주입한다
      @Value("${azure.storage.connection-string}") String connectionString,
      @Value("${azure.storage.container-name}") String containerName) {
    this.containerClient =
        new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()
            .getBlobContainerClient(containerName);
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
