package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.review.Review;
import com.kdt.shoppingmall.domain.review.ReviewKeyword;
import com.kdt.shoppingmall.dto.review.ReviewRequest;
import com.kdt.shoppingmall.dto.review.ReviewResponse;
import com.kdt.shoppingmall.exception.DuplicateReviewException;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.OrderItemRepository;
import com.kdt.shoppingmall.repository.ProductRepository;
import com.kdt.shoppingmall.repository.ReviewKeywordRepository;
import com.kdt.shoppingmall.repository.ReviewRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock private ReviewRepository reviewRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private ProductRepository productRepository;
  @Mock private OrderItemRepository orderItemRepository;
  @Mock private ReviewKeywordRepository reviewKeywordRepository;
  @Mock private KeywordExtractor keywordExtractor;

  @InjectMocks private ReviewService reviewService;

  private Member member;
  private Product product;
  private Review review;

  @BeforeEach
  void setUp() {
    member = new Member("test@test.com", "encoded", "테스터", MemberRole.USER);
    ReflectionTestUtils.setField(member, "id", 1L);

    product = new Product("상품A", "설명", 10000, 100, null);
    ReflectionTestUtils.setField(product, "id", 1L);

    review = new Review(member, product, 5, "정말 좋아요!");
    ReflectionTestUtils.setField(review, "id", 1L);
  }

  @Test
  void createReview_성공() {
    ReviewRequest request = new ReviewRequest(5, "정말 좋아요!");

    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(productRepository.findById(1L)).willReturn(Optional.of(product));
    given(
            orderItemRepository.existsByOrderMemberIdAndProductIdAndOrderStatusIn(
                eq(1L), eq(1L), any()))
        .willReturn(true);
    given(reviewRepository.existsByMemberIdAndProductId(1L, 1L)).willReturn(false);
    given(reviewRepository.save(any(Review.class))).willReturn(review);
    // 키워드 추출 결과가 비어있으면 save가 호출되지 않는다.
    given(keywordExtractor.extract(any())).willReturn(Set.of());

    ReviewResponse response = reviewService.createReview(1L, 1L, request);

    assertThat(response.rating()).isEqualTo(5);
    assertThat(response.content()).isEqualTo("정말 좋아요!");
    assertThat(response.memberName()).isEqualTo("테스터");
  }

  // 리뷰 저장 시 추출된 키워드 수만큼 ReviewKeyword가 저장되어야 한다.
  @Test
  void createReview_키워드_추출해서_저장한다() {
    ReviewRequest request = new ReviewRequest(5, "신선하고 맛있어요!");

    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(productRepository.findById(1L)).willReturn(Optional.of(product));
    given(
            orderItemRepository.existsByOrderMemberIdAndProductIdAndOrderStatusIn(
                eq(1L), eq(1L), any()))
        .willReturn(true);
    given(reviewRepository.existsByMemberIdAndProductId(1L, 1L)).willReturn(false);
    given(reviewRepository.save(any(Review.class))).willReturn(review);
    // 2개의 키워드가 추출된 경우를 가정한다.
    given(keywordExtractor.extract("신선하고 맛있어요!")).willReturn(Set.of("신선", "맛있"));

    reviewService.createReview(1L, 1L, request);

    // 추출된 키워드 수(2개)만큼 ReviewKeyword가 저장되었는지 검증한다.
    verify(reviewKeywordRepository, times(2)).save(any(ReviewKeyword.class));
  }

  @Test
  void createReview_미구매자_접근금지() {
    ReviewRequest request = new ReviewRequest(5, "좋아요!");

    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(productRepository.findById(1L)).willReturn(Optional.of(product));
    given(
            orderItemRepository.existsByOrderMemberIdAndProductIdAndOrderStatusIn(
                eq(1L), eq(1L), any()))
        .willReturn(false);

    assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("구매자만");
  }

  @Test
  void createReview_중복리뷰_예외발생() {
    ReviewRequest request = new ReviewRequest(3, "두 번째 리뷰");

    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(productRepository.findById(1L)).willReturn(Optional.of(product));
    given(
            orderItemRepository.existsByOrderMemberIdAndProductIdAndOrderStatusIn(
                eq(1L), eq(1L), any()))
        .willReturn(true);
    given(reviewRepository.existsByMemberIdAndProductId(1L, 1L)).willReturn(true);

    assertThatThrownBy(() -> reviewService.createReview(1L, 1L, request))
        .isInstanceOf(DuplicateReviewException.class)
        .hasMessageContaining("1");
  }

  @Test
  void createReview_존재하지않는상품_예외발생() {
    ReviewRequest request = new ReviewRequest(4, "리뷰");

    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(productRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.createReview(1L, 99L, request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void getReviews_성공() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Review> page = new PageImpl<>(List.of(new Review(member, product, 4, "좋아요")));

    given(productRepository.existsById(1L)).willReturn(true);
    given(reviewRepository.findByProductIdOrderByCreatedAtDesc(1L, pageable)).willReturn(page);

    Page<ReviewResponse> responses = reviewService.getReviews(1L, pageable);

    assertThat(responses.getTotalElements()).isEqualTo(1);
    assertThat(responses.getContent().get(0).rating()).isEqualTo(4);
  }

  @Test
  void getReviews_존재하지않는상품_예외발생() {
    given(productRepository.existsById(99L)).willReturn(false);

    assertThatThrownBy(() -> reviewService.getReviews(99L, Pageable.unpaged()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deleteReview_성공() {
    given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

    reviewService.deleteReview(1L, 1L);

    // 리뷰 삭제 전에 연결된 키워드도 삭제되어야 한다.
    verify(reviewKeywordRepository).deleteByReviewId(1L);
    verify(reviewRepository).delete(review);
  }

  @Test
  void deleteReview_다른회원_접근금지() {
    Member other = new Member("other@test.com", "encoded", "다른사람", MemberRole.USER);
    ReflectionTestUtils.setField(other, "id", 2L);
    Review otherReview = new Review(other, product, 5, "리뷰");
    ReflectionTestUtils.setField(otherReview, "id", 1L);

    given(reviewRepository.findById(1L)).willReturn(Optional.of(otherReview));

    assertThatThrownBy(() -> reviewService.deleteReview(1L, 1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void deleteReview_없는리뷰_예외발생() {
    given(reviewRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.deleteReview(1L, 99L))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
