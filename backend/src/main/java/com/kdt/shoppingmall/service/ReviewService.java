package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.review.Review;
import com.kdt.shoppingmall.dto.review.ReviewRequest;
import com.kdt.shoppingmall.dto.review.ReviewResponse;
import com.kdt.shoppingmall.exception.DuplicateReviewException;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.ProductRepository;
import com.kdt.shoppingmall.repository.ReviewRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository, MemberRepository memberRepository,
            ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public ReviewResponse createReview(Long memberId, Long productId, ReviewRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("회원을 찾을 수 없습니다. id=" + memberId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다. id=" + productId));

        if (reviewRepository.existsByMemberIdAndProductId(memberId, productId)) {
            throw new DuplicateReviewException("이미 리뷰를 작성한 상품입니다. productId=" + productId);
        }

        Review review = reviewRepository.save(new Review(member, product, request.rating(), request.content()));
        return ReviewResponse.from(review);
    }

    public List<ReviewResponse> getReviews(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("상품을 찾을 수 없습니다. id=" + productId);
        }
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public void deleteReview(Long memberId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("리뷰를 찾을 수 없습니다. id=" + reviewId));
        if (!review.getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("본인의 리뷰만 삭제할 수 있습니다.");
        }
        reviewRepository.delete(review);
    }
}
