package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.review.Review;
import com.kdt.shoppingmall.dto.review.ReviewRequest;
import com.kdt.shoppingmall.dto.review.ReviewResponse;
import com.kdt.shoppingmall.exception.DuplicateReviewException;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.ProductRepository;
import com.kdt.shoppingmall.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private ReviewService reviewService;

    private Member member;
    private Product product;

    @BeforeEach
    void setUp() {
        member = new Member("test@test.com", "encoded", "테스터", MemberRole.USER);
        ReflectionTestUtils.setField(member, "id", 1L);

        product = new Product("상품A", "설명", 10000, 100, null);
        ReflectionTestUtils.setField(product, "id", 1L);
    }

    @Test
    void createReview_성공() {
        ReviewRequest request = new ReviewRequest(5, "정말 좋아요!");
        Review review = new Review(member, product, 5, "정말 좋아요!");
        ReflectionTestUtils.setField(review, "id", 1L);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(reviewRepository.existsByMemberIdAndProductId(1L, 1L)).willReturn(false);
        given(reviewRepository.save(any(Review.class))).willReturn(review);

        ReviewResponse response = reviewService.createReview(1L, 1L, request);

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.content()).isEqualTo("정말 좋아요!");
        assertThat(response.memberName()).isEqualTo("테스터");
    }

    @Test
    void createReview_중복리뷰_예외발생() {
        ReviewRequest request = new ReviewRequest(3, "두 번째 리뷰");

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
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
        Review review = new Review(member, product, 4, "좋아요");
        given(productRepository.existsById(1L)).willReturn(true);
        given(reviewRepository.findByProductIdOrderByCreatedAtDesc(1L)).willReturn(List.of(review));

        List<ReviewResponse> responses = reviewService.getReviews(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).rating()).isEqualTo(4);
    }

    @Test
    void getReviews_존재하지않는상품_예외발생() {
        given(productRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> reviewService.getReviews(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteReview_성공() {
        Review review = new Review(member, product, 5, "좋아요");
        ReflectionTestUtils.setField(review, "id", 1L);

        given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

        reviewService.deleteReview(1L, 1L);

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_다른회원_접근금지() {
        Member other = new Member("other@test.com", "encoded", "다른사람", MemberRole.USER);
        ReflectionTestUtils.setField(other, "id", 2L);
        Review review = new Review(other, product, 5, "리뷰");
        ReflectionTestUtils.setField(review, "id", 1L);

        given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

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
