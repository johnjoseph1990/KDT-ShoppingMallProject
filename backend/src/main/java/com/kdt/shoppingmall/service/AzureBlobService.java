package com.kdt.shoppingmall.service;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.kdt.shoppingmall.exception.UnsupportedFileTypeException;
import java.io.IOException;
import java.util.Set;
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

  // 업로드를 허용할 MIME 타입 목록.
  //
  // 프론트의 frontend/src/utils/imageUpload.js ALLOWED_IMAGE_TYPES와 같은 값이어야 한다.
  // 그런데 왜 백엔드에도 같은 검사를 두나? 프론트 검증은 "실수 방지"지 "보안"이 아니기 때문이다.
  // 브라우저를 거치지 않고 curl로 직접 POST /api/admin/upload 를 부르면 프론트 검증은 통째로 건너뛴다.
  // 서버 쪽 검사가 진짜 방어선이다.
  //
  // image/svg+xml이 빠져 있는 건 실수가 아니다 — SVG는 그 안에 <script>를 넣을 수 있는
  // "실행 가능한 이미지"라서, 공개 컨테이너에 올려 두면 XSS 발판이 된다.
  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

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
   *
   * <p>Content-Type을 함께 저장한다 — 이걸 빼먹으면 Azure가 application/octet-stream(정체불명 바이트 덩어리)으로 서빙한다
   * (2026-08-02 DEF-11).
   */
  public String upload(MultipartFile file) throws IOException {
    // 저장하기 전에 "이 파일을 받아도 되는지 + 무슨 타입으로 기록할지"를 먼저 정한다.
    String contentType = resolveContentType(file);

    // UUID로 파일명 중복 방지
    String blobName = UUID.randomUUID() + "_" + file.getOriginalFilename();
    BlobClient blobClient = containerClient.getBlobClient(blobName);

    // 예전에는 blobClient.upload(스트림, 크기, true) 3-인자 버전을 썼다.
    // 그 버전에는 "헤더"를 끼워 넣을 자리가 없어서 Content-Type을 줄 방법이 아예 없었다.
    // 옵션 객체(BlobParallelUploadOptions)를 받는 버전으로 바꾸면 내용과 헤더를
    // 한 번의 요청으로 함께 보낼 수 있다.
    //   ※ 업로드 후 setHttpHeaders()로 따로 붙이는 방법도 있지만, 그러면 두 요청 사이에
    //     "octet-stream으로 보이는 순간"이 생기고 두 번째 요청이 실패하면 결함이 그대로 남는다.
    // requestConditions를 지정하지 않았으므로 기존 blob이 있으면 덮어쓴다(= 이전의 overwrite=true).
    BlobParallelUploadOptions options =
        new BlobParallelUploadOptions(file.getInputStream(), file.getSize())
            .setHeaders(new BlobHttpHeaders().setContentType(contentType));
    // Context.NONE: 이 요청에 추가로 실어 보낼 부가 정보(추적 ID 등)가 없다는 뜻.
    blobClient.uploadWithResponse(options, Context.NONE);

    return blobClient.getBlobUrl();
  }

  /**
   * 업로드된 파일에 붙일 Content-Type을 결정한다.
   *
   * <p>file.getContentType()은 브라우저(=클라이언트)가 보내준 값이라 그대로 믿으면 안 된다. 공개 컨테이너에 text/html을 심을 수 있게 되기
   * 때문이다.
   */
  private String resolveContentType(MultipartFile file) {
    String contentType = file.getContentType();

    // 허용 목록에 없으면 저장 자체를 거부한다(거부 정책).
    // 폴백(octet-stream으로 저장) 대신 거부를 고른 이유:
    //  - 프론트(imageUpload.js)가 이미 같은 4종만 허용한다. 서버가 더 관대하면 규칙이 두 곳에서 어긋난다.
    //  - 정체불명 파일이 공개 컨테이너에 쌓이지 않는다. 한 번 올라간 blob은 지우기 번거롭다.
    //
    // null 검사를 왜 따로 하나 — contains(null)에 맡길 수 없기 때문이다.
    // Set.of(...)가 만드는 불변 Set은 null을 담을 수 없는 컬렉션이라
    // contains(null) 호출 자체를 NullPointerException으로 거부한다.
    // (HashSet이었다면 조용히 false를 돌려준다. 같은 Set 인터페이스인데 구현마다 다르다 —
    //  실제로 이 프로젝트의 테스트가 그 차이를 잡아냈다.)
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new UnsupportedFileTypeException("이미지 파일만 업로드할 수 있습니다. (JPG, PNG, WebP, GIF)");
    }

    return contentType;
  }
}
