package com.kdt.shoppingmall.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Azure Blob Storage 관련 빈(Bean)을 만드는 설정 클래스.
// @ConditionalOnExpression: AZURE_STORAGE_CONNECTION_STRING 환경변수가 비어있으면
// 이 빈을 아예 생성하지 않는다 — 로컬 테스트나 Azure 미설정 환경에서도 앱이 정상 기동된다.
@Configuration
@ConditionalOnExpression("!'${azure.storage.connection-string:}'.trim().isEmpty()")
public class AzureStorageConfig {

  // BlobContainerClient: Azure SDK에서 특정 컨테이너(폴더)에 파일을 올리고 내리는 클라이언트.
  // @Bean으로 등록해두면 AzureBlobService가 생성자 주입으로 받아 쓸 수 있고,
  // 테스트에서는 이 빈 대신 Mock을 주입하면 실제 Azure 연결 없이 테스트할 수 있다.
  @Bean
  public BlobContainerClient blobContainerClient(
      @Value("${azure.storage.connection-string}") String connectionString,
      @Value("${azure.storage.container-name}") String containerName) {
    return new BlobServiceClientBuilder()
        .connectionString(connectionString)
        .buildClient()
        .getBlobContainerClient(containerName);
  }
}
