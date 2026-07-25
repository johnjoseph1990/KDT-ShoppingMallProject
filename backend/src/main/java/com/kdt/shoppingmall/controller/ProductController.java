package com.kdt.shoppingmall.controller;

import com.kdt.shoppingmall.dto.product.BestProductResponse;
import com.kdt.shoppingmall.dto.product.ProductRequest;
import com.kdt.shoppingmall.dto.product.ProductResponse;
import com.kdt.shoppingmall.dto.review.KeywordResponse;
import com.kdt.shoppingmall.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @PostMapping
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
  }

  // GET /api/products?keyword=사과&tag=과일&minRating=4
  // @RequestParam(required = false): 쿼리 파라미터가 없어도 400이 아니라 null이 들어온다.
  // minRating을 원시 타입 double이 아니라 래퍼 타입 Double로 받는 이유 —
  // 원시 타입은 null을 담을 수 없어서 "필터 미적용"과 "0점 이상"을 구분할 수 없다.
  @GetMapping
  public Page<ProductResponse> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String tag,
      @RequestParam(required = false) Double minRating,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return productService.search(keyword, tag, minRating, pageable);
  }

  // [P1-3] 반환 타입을 BestProductResponse로 변경 — source 필드로 폴백 단계를 구분한다.
  // 응답의 나머지 필드(id, name, price ...)는 ProductResponse와 동일하므로
  // 프론트엔드 코드 변경 없이 호환된다.
  @GetMapping("/best")
  public Page<BestProductResponse> getBestProducts(@PageableDefault(size = 5) Pageable pageable) {
    return productService.findBestProducts(pageable);
  }

  @GetMapping("/{id}")
  public ProductResponse findById(@PathVariable Long id) {
    return productService.findById(id);
  }

  // R-6: 같은 태그를 가진 다른 상품 추천 (상품 상세 페이지에서 호출)
  @GetMapping("/{id}/recommendations")
  public List<ProductResponse> getRecommendations(@PathVariable Long id) {
    return productService.getRecommendations(id);
  }

  // [키워드 추천] 상품에 달린 리뷰 본문에서 자주 나온 키워드 상위 limit개를 반환한다.
  // 프론트에서 상품 상세 페이지에 "이 상품의 키워드: 신선 · 맛있 · 재구매"처럼 표시할 수 있다.
  @GetMapping("/{id}/keywords")
  public List<KeywordResponse> getKeywords(
      @PathVariable Long id, @RequestParam(defaultValue = "10") int limit) {
    return productService.getKeywords(id, limit);
  }

  @PutMapping("/{id}")
  public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
    return productService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    productService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
