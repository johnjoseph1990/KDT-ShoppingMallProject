package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.domain.order.OrderStatus;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.product.ProductTag;
import com.kdt.shoppingmall.dto.product.BestProductResponse;
import com.kdt.shoppingmall.dto.product.ProductRequest;
import com.kdt.shoppingmall.dto.product.ProductResponse;
import com.kdt.shoppingmall.dto.review.KeywordResponse;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.ProductRepository;
import com.kdt.shoppingmall.repository.ReviewKeywordRepository;
import com.kdt.shoppingmall.repository.ReviewRepository;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

  private final ProductRepository productRepository;
  private final ReviewRepository reviewRepository;
  private final ReviewKeywordRepository reviewKeywordRepository;

  private static final List<OrderStatus> PAID_STATUSES =
      List.of(OrderStatus.PAID, OrderStatus.SHIPPING, OrderStatus.DELIVERED);

  public ProductService(
      ProductRepository productRepository,
      ReviewRepository reviewRepository,
      ReviewKeywordRepository reviewKeywordRepository) {
    this.productRepository = productRepository;
    this.reviewRepository = reviewRepository;
    this.reviewKeywordRepository = reviewKeywordRepository;
  }

  @Transactional
  public ProductResponse create(ProductRequest request) {
    Product product =
        new Product(
            request.name(),
            request.description(),
            request.price(),
            request.stockQuantity(),
            request.imageUrl());
    addTags(product, request.tags());
    return toResponse(productRepository.save(product));
  }

  // minRating: 평균 별점 하한선. null이면 별점 조건 없이 조회한다.
  // minRating이 null이면 별점 조건이 아예 없는 쿼리를, 값이 있으면 별점 조건이 있는
  // 쿼리를 호출한다 (repository의 minRating 파라미터 관련 주석 참고).
  public Page<ProductResponse> search(
      String keyword, String tag, Double minRating, Pageable pageable) {
    Page<Product> products =
        minRating == null
            ? productRepository.searchProducts(keyword, tag, pageable)
            : productRepository.searchProductsWithMinRating(keyword, tag, minRating, pageable);
    return products.map(this::toResponse);
  }

  // [P1-2 + P1-3] 베스트 상품 3단계 채우기(fill-through):
  //   1순위 REVIEW_BEST — 리뷰 5개 이상 + 평균 별점 내림차순 (N+1도 동시에 해결)
  //   2순위 SALES       — 결제 완료 주문에서 판매량 순
  //   3순위 LATEST      — 최신 등록 상품
  //
  // 예전에는 앞 단계가 "하나라도" 결과를 내면 그걸로 전량 확정하고 끝냈다. 그래서
  // 판매 실적 있는 상품이 1개뿐이어도 그 1개만 반환돼, 화면 그리드에 카드 1개 +
  // 빈 칸 3개가 뜨는 결함(2026-08-02 DEF-4)이 있었다. 지금은 각 단계가 "부족한 만큼만"
  // 다음 단계로 채워 항상 요청한 개수(size)를 최대한 채우려 시도한다.
  //
  // 같은 상품이 여러 단계의 후보에 동시에 낄 수 있어(예: 리뷰도 많고 판매도 많은 상품)
  // LinkedHashMap으로 상품 id를 키 삼아 중복을 제거하면서, 먼저 뽑힌 순서
  // (REVIEW_BEST → SALES → LATEST)를 그대로 유지한다. 화면 상단 배지는 이 목록의
  // 첫 항목 source를 쓰므로, "가장 강한 근거가 있으면 그 근거가 대표로 노출"되는
  // 기존 우선순위 의미는 그대로 유지된다.
  public Page<BestProductResponse> findBestProducts(Pageable pageable) {
    int size = pageable.getPageSize();
    LinkedHashMap<Long, BestProductResponse> picked = new LinkedHashMap<>();

    // 1순위: 리뷰 기반
    for (Object[] row : productRepository.findBestProductsWithAvgRating(pageable)) {
      // Object[0]=Product, Object[1]=AVG(rating) — 쿼리에서 함께 반환해 N+1 제거
      Product p = (Product) row[0];
      picked.put(p.getId(), BestProductResponse.from(p, (Double) row[1], "REVIEW_BEST"));
    }

    // 2순위: 판매량 기반 — 1순위가 다 채우지 못한 만큼만 진행
    if (picked.size() < size) {
      for (Object[] row : productRepository.findTopBySales(PAID_STATUSES, pageable)) {
        Product p = (Product) row[0];
        if (picked.containsKey(p.getId())) {
          continue; // 이미 REVIEW_BEST로 뽑힌 상품은 건너뛴다
        }
        // 폴백 상태에서는 상품 수가 적어 N+1의 실질적 영향이 작다.
        Double avg = reviewRepository.findAverageRatingByProductId(p.getId());
        picked.put(p.getId(), BestProductResponse.from(p, avg != null ? avg : 0.0, "SALES"));
      }
    }

    // 3순위: 최신 등록 순 — 그래도 부족하면 채운다. pageable의 정렬을 createdAt DESC로 고정.
    if (picked.size() < size) {
      Pageable latestSort =
          PageRequest.of(
              pageable.getPageNumber(),
              pageable.getPageSize(),
              Sort.by(Sort.Direction.DESC, "createdAt"));
      for (Product p : productRepository.findAll(latestSort)) {
        if (picked.containsKey(p.getId())) {
          continue;
        }
        Double avg = reviewRepository.findAverageRatingByProductId(p.getId());
        picked.put(p.getId(), BestProductResponse.from(p, avg != null ? avg : 0.0, "LATEST"));
      }
    }

    List<BestProductResponse> content = picked.values().stream().limit(size).toList();
    return new PageImpl<>(content, pageable, content.size());
  }

  // 상품에 달린 리뷰에서 추출된 키워드를 빈도 내림차순으로 반환한다.
  // limit: 반환할 최대 키워드 수 (기본값은 컨트롤러에서 10으로 설정).
  public List<KeywordResponse> getKeywords(Long productId, int limit) {
    getProductOrThrow(productId); // 상품 존재 확인 — 없으면 404
    return reviewKeywordRepository
        .findTopKeywordsByProductId(productId, PageRequest.of(0, limit))
        .stream()
        .map(row -> new KeywordResponse((String) row[0], (Long) row[1]))
        .toList();
  }

  // 상품 상세 조회 + 조회수 1 증가.
  // @Transactional: readOnly = false (클래스 기본값 true를 메서드 수준에서 덮어씀).
  // JPA dirty checking으로 viewCount 변경이 트랜잭션 커밋 시 자동으로 DB에 반영된다.
  @Transactional
  public ProductResponse findById(Long id) {
    Product product = getProductOrThrow(id);
    product.increaseViewCount();
    return toResponse(product);
  }

  // [추천 폴백] 태그 기반 추천이 없을 때 조회수 높은 상품으로 대체.
  // 1순위: 같은 태그를 가진 상품 (연관 추천)
  // 2순위: 조회수 높은 상품 — 태그가 없거나 결과가 비었을 때 (인기 기반 폴백)
  public List<ProductResponse> getRecommendations(Long productId) {
    Product product = getProductOrThrow(productId);
    List<String> tagNames = product.getTags().stream().map(ProductTag::getName).toList();

    if (!tagNames.isEmpty()) {
      List<ProductResponse> byTag =
          productRepository
              .findByTagNamesExcludingProduct(tagNames, productId, PageRequest.of(0, 4))
              .stream()
              .map(this::toResponse)
              .toList();
      if (!byTag.isEmpty()) {
        return byTag;
      }
    }

    // 태그가 없거나 같은 태그 상품이 0개이면 조회수 높은 상품 4개를 폴백으로 반환한다.
    return productRepository
        .findByIdNotOrderByViewCountDesc(productId, PageRequest.of(0, 4))
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public ProductResponse update(Long id, ProductRequest request) {
    Product product = getProductOrThrow(id);
    product.update(
        request.name(),
        request.description(),
        request.price(),
        request.stockQuantity(),
        request.imageUrl());
    product.clearTags();
    addTags(product, request.tags());
    return toResponse(product);
  }

  @Transactional
  public void delete(Long id) {
    Product product = getProductOrThrow(id);
    productRepository.delete(product);
  }

  private Product getProductOrThrow(Long id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다. id=" + id));
  }

  private ProductResponse toResponse(Product product) {
    Double averageRating = reviewRepository.findAverageRatingByProductId(product.getId());
    return ProductResponse.from(product, averageRating);
  }

  private void addTags(Product product, List<String> tags) {
    if (tags == null) {
      return;
    }
    tags.forEach(name -> product.addTag(new ProductTag(name)));
  }
}
