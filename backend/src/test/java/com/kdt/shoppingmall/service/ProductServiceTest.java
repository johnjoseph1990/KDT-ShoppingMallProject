package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.kdt.shoppingmall.domain.order.OrderStatus;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.product.ProductTag;
import com.kdt.shoppingmall.dto.product.BestProductResponse;
import com.kdt.shoppingmall.dto.product.ProductRequest;
import com.kdt.shoppingmall.dto.product.ProductResponse;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.ProductRepository;
import com.kdt.shoppingmall.repository.ReviewRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock private ProductRepository productRepository;
  @Mock private ReviewRepository reviewRepository;

  @InjectMocks private ProductService productService;

  @Test
  void create_성공() {
    ProductRequest request = new ProductRequest("상품A", "설명", 10000, 100, null, null);
    Product product = new Product("상품A", "설명", 10000, 100, null);
    given(productRepository.save(any(Product.class))).willReturn(product);

    ProductResponse response = productService.create(request);

    assertThat(response.name()).isEqualTo("상품A");
    assertThat(response.price()).isEqualTo(10000);
    assertThat(response.stockQuantity()).isEqualTo(100);
    verify(productRepository).save(any(Product.class));
  }

  @Test
  void findById_성공() {
    Product product = new Product("상품A", "설명", 10000, 100, null);
    given(productRepository.findById(1L)).willReturn(Optional.of(product));

    ProductResponse response = productService.findById(1L);

    assertThat(response.name()).isEqualTo("상품A");
  }

  @Test
  void findById_리뷰있으면_평균별점반환() {
    Product product = new Product("상품A", "설명", 10000, 100, null);
    given(productRepository.findById(1L)).willReturn(Optional.of(product));
    given(reviewRepository.findAverageRatingByProductId(product.getId())).willReturn(4.5);

    ProductResponse response = productService.findById(1L);

    assertThat(response.averageRating()).isEqualTo(4.5);
  }

  @Test
  void findById_리뷰없으면_평균별점0점() {
    Product product = new Product("상품A", "설명", 10000, 100, null);
    given(productRepository.findById(1L)).willReturn(Optional.of(product));
    given(reviewRepository.findAverageRatingByProductId(product.getId())).willReturn(null);

    ProductResponse response = productService.findById(1L);

    assertThat(response.averageRating()).isEqualTo(0.0);
  }

  @Test
  void findById_존재하지않는상품_예외발생() {
    given(productRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> productService.findById(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("99");
  }

  @Test
  void update_성공() {
    Product product = new Product("기존상품", "기존설명", 5000, 10, null);
    ProductRequest request = new ProductRequest("수정상품", "수정설명", 9000, 20, null, null);
    given(productRepository.findById(1L)).willReturn(Optional.of(product));

    ProductResponse response = productService.update(1L, request);

    assertThat(response.name()).isEqualTo("수정상품");
    assertThat(response.price()).isEqualTo(9000);
    assertThat(response.stockQuantity()).isEqualTo(20);
  }

  @Test
  void delete_성공() {
    Product product = new Product("상품A", "설명", 10000, 100, null);
    given(productRepository.findById(1L)).willReturn(Optional.of(product));

    productService.delete(1L);

    verify(productRepository).delete(product);
  }

  @Test
  void delete_존재하지않는상품_예외발생() {
    given(productRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> productService.delete(99L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void search_키워드없이_전체조회() {
    Product product = new Product("상품A", "설명", 10000, 100, null);
    Pageable pageable = PageRequest.of(0, 10);
    Page<Product> page = new PageImpl<>(List.of(product));
    given(productRepository.searchProducts(isNull(), isNull(), any(Pageable.class)))
        .willReturn(page);

    Page<ProductResponse> result = productService.search(null, null, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).name()).isEqualTo("상품A");
  }

  @Test
  void search_키워드로_검색() {
    Product product = new Product("나이키 운동화", "설명", 89000, 50, null);
    Pageable pageable = PageRequest.of(0, 10);
    Page<Product> page = new PageImpl<>(List.of(product));
    given(productRepository.searchProducts(any(), isNull(), any(Pageable.class))).willReturn(page);

    Page<ProductResponse> result = productService.search("나이키", null, pageable);

    assertThat(result.getContent().get(0).name()).isEqualTo("나이키 운동화");
  }

  // [P1-2 + P1-3] 리뷰 5개 이상인 상품이 있으면 REVIEW_BEST를 반환한다.
  @Test
  void findBestProducts_리뷰기반_REVIEW_BEST_반환() {
    Product product = new Product("상품A", "설명", 10000, 100, null);
    Pageable pageable = PageRequest.of(0, 5);
    // Object[0]=Product, Object[1]=avgRating — DB 쿼리가 함께 반환하는 구조
    // List.of(Object[])는 컴파일러가 varargs와 혼동해 타입 추론에 실패하므로 ArrayList를 사용한다.
    java.util.ArrayList<Object[]> rows = new java.util.ArrayList<>();
    rows.add(new Object[] {product, 4.5});
    given(productRepository.findBestProductsWithAvgRating(pageable))
        .willReturn(new PageImpl<>(rows));

    Page<BestProductResponse> result = productService.findBestProducts(pageable);

    assertThat(result.getContent().get(0).averageRating()).isEqualTo(4.5);
    assertThat(result.getContent().get(0).source()).isEqualTo("REVIEW_BEST");
  }

  // [P1-3] 리뷰 기반 결과가 없으면 판매량 폴백(SALES)으로 전환한다.
  @Test
  void findBestProducts_리뷰없으면_판매량_SALES_폴백() {
    Product product = new Product("판매왕상품", "설명", 15000, 50, null);
    Pageable pageable = PageRequest.of(0, 5);
    List<OrderStatus> paidStatuses =
        List.of(OrderStatus.PAID, OrderStatus.SHIPPING, OrderStatus.DELIVERED);

    given(productRepository.findBestProductsWithAvgRating(pageable)).willReturn(Page.empty());
    java.util.ArrayList<Object[]> salesRows = new java.util.ArrayList<>();
    salesRows.add(new Object[] {product, 100L});
    given(productRepository.findTopBySales(any(), eq(pageable)))
        .willReturn(new PageImpl<>(salesRows));
    given(reviewRepository.findAverageRatingByProductId(product.getId())).willReturn(null);

    Page<BestProductResponse> result = productService.findBestProducts(pageable);

    assertThat(result.getContent().get(0).source()).isEqualTo("SALES");
  }

  // [P1-3] 리뷰·판매 데이터가 모두 없으면 최신 등록 상품(LATEST)을 반환한다.
  @Test
  void findBestProducts_판매없으면_최신_LATEST_폴백() {
    Product product = new Product("신상품", "설명", 20000, 10, null);
    Pageable pageable = PageRequest.of(0, 5);

    given(productRepository.findBestProductsWithAvgRating(pageable)).willReturn(Page.empty());
    given(productRepository.findTopBySales(any(), any())).willReturn(Page.empty());
    // 최신 폴백: findAll(Pageable) 호출 — 정렬이 바뀐 Pageable이 전달됨
    given(productRepository.findAll(any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(product)));
    given(reviewRepository.findAverageRatingByProductId(product.getId())).willReturn(null);

    Page<BestProductResponse> result = productService.findBestProducts(pageable);

    assertThat(result.getContent().get(0).source()).isEqualTo("LATEST");
  }

  @Test
  void search_결과없음_빈페이지반환() {
    Pageable pageable = PageRequest.of(0, 10);
    given(productRepository.searchProducts(any(), any(), any(Pageable.class)))
        .willReturn(Page.empty());

    Page<ProductResponse> result = productService.search("없는상품", null, pageable);

    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void getRecommendations_태그기반_추천상품_반환() {
    Product product = new Product("운동화", "설명", 89000, 50, null);
    product.addTag(new ProductTag("스포츠"));
    product.addTag(new ProductTag("신발"));
    Product recommended = new Product("러닝화", "설명", 79000, 30, null);

    given(productRepository.findById(1L)).willReturn(Optional.of(product));
    given(productRepository.findByTagNamesExcludingProduct(any(), any(), any()))
        .willReturn(List.of(recommended));

    List<ProductResponse> result = productService.getRecommendations(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("러닝화");
  }

  @Test
  void getRecommendations_태그없는상품_빈리스트반환() {
    Product product = new Product("태그없는상품", "설명", 10000, 10, null);
    given(productRepository.findById(1L)).willReturn(Optional.of(product));

    List<ProductResponse> result = productService.getRecommendations(1L);

    assertThat(result).isEmpty();
  }

  @Test
  void getRecommendations_존재하지않는상품_예외발생() {
    given(productRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> productService.getRecommendations(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("99");
  }
}
