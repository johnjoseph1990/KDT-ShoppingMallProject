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
import java.util.List;
import org.springframework.data.domain.Page;
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

  // [P1-2 + P1-3] 베스트 상품 3단계 폴백:
  //   1순위 REVIEW_BEST — 리뷰 5개 이상 + 평균 별점 내림차순 (N+1도 동시에 해결)
  //   2순위 SALES       — 결제 완료 주문에서 판매량 순 (리뷰 데이터 부족 시)
  //   3순위 LATEST      — 최신 등록 상품 (판매 데이터도 없을 때)
  //
  // 각 단계는 앞 단계가 결과를 하나도 못 낼 때만 실행한다 (배타적 전환).
  // source 필드로 클라이언트가 어느 단계 결과인지 구분할 수 있다.
  public Page<BestProductResponse> findBestProducts(Pageable pageable) {
    // 1순위: 리뷰 기반
    Page<Object[]> best = productRepository.findBestProductsWithAvgRating(pageable);
    if (best.getTotalElements() > 0) {
      // Object[0]=Product, Object[1]=AVG(rating) — 쿼리에서 함께 반환해 N+1 제거
      return best.map(
          row -> BestProductResponse.from((Product) row[0], (Double) row[1], "REVIEW_BEST"));
    }

    // 2순위: 판매량 기반
    Page<Object[]> sales = productRepository.findTopBySales(PAID_STATUSES, pageable);
    if (sales.getTotalElements() > 0) {
      return sales.map(
          row -> {
            Product p = (Product) row[0];
            // 폴백 상태에서는 상품 수가 적어 N+1의 실질적 영향이 작다.
            Double avg = reviewRepository.findAverageRatingByProductId(p.getId());
            return BestProductResponse.from(p, avg != null ? avg : 0.0, "SALES");
          });
    }

    // 3순위: 최신 등록 순 — pageable의 정렬을 createdAt DESC로 고정한다.
    Pageable latestSort =
        PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt"));
    return productRepository
        .findAll(latestSort)
        .map(
            p -> {
              Double avg = reviewRepository.findAverageRatingByProductId(p.getId());
              return BestProductResponse.from(p, avg != null ? avg : 0.0, "LATEST");
            });
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
