package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.order.OrderStatus;
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
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final MemberRepository memberRepository;
  private final ProductRepository productRepository;
  private final OrderItemRepository orderItemRepository;
  // [키워드 추천] 리뷰 저장 시 본문에서 키워드를 추출해 저장한다.
  private final ReviewKeywordRepository reviewKeywordRepository;
  private final KeywordExtractor keywordExtractor;

  private static final List<OrderStatus> PURCHASED_STATUSES =
      List.of(OrderStatus.PAID, OrderStatus.SHIPPING, OrderStatus.DELIVERED);

  public ReviewService(
      ReviewRepository reviewRepository,
      MemberRepository memberRepository,
      ProductRepository productRepository,
      OrderItemRepository orderItemRepository,
      ReviewKeywordRepository reviewKeywordRepository,
      KeywordExtractor keywordExtractor) {
    this.reviewRepository = reviewRepository;
    this.memberRepository = memberRepository;
    this.productRepository = productRepository;
    this.orderItemRepository = orderItemRepository;
    this.reviewKeywordRepository = reviewKeywordRepository;
    this.keywordExtractor = keywordExtractor;
  }

  @Transactional
  public ReviewResponse createReview(Long memberId, Long productId, ReviewRequest request) {
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("회원을 찾을 수 없습니다. id=" + memberId));
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다. id=" + productId));

    if (!orderItemRepository.existsByOrderMemberIdAndProductIdAndOrderStatusIn(
        memberId, productId, PURCHASED_STATUSES)) {
      throw new AccessDeniedException("구매자만 리뷰를 작성할 수 있습니다.");
    }

    if (reviewRepository.existsByMemberIdAndProductId(memberId, productId)) {
      throw new DuplicateReviewException("이미 리뷰를 작성한 상품입니다. productId=" + productId);
    }

    Review review =
        reviewRepository.save(new Review(member, product, request.rating(), request.content()));

    // [키워드 추천] 리뷰 저장과 같은 트랜잭션 안에서 키워드를 추출·저장한다.
    // 추출에 실패해도 리뷰 저장은 롤백되지 않도록 예외를 전파하지 않으면 되지만,
    // 현재는 단순 문자열 검사라 실패 경로가 없어 함께 처리한다.
    Set<String> keywords = keywordExtractor.extract(request.content());
    keywords.forEach(kw -> reviewKeywordRepository.save(new ReviewKeyword(review, kw)));

    return ReviewResponse.from(review);
  }

  public Page<ReviewResponse> getReviews(Long productId, Pageable pageable) {
    if (!productRepository.existsById(productId)) {
      throw new ResourceNotFoundException("상품을 찾을 수 없습니다. id=" + productId);
    }
    return reviewRepository
        .findByProductIdOrderByCreatedAtDesc(productId, pageable)
        .map(ReviewResponse::from);
  }

  @Transactional
  public void deleteReview(Long memberId, Long reviewId) {
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("리뷰를 찾을 수 없습니다. id=" + reviewId));
    if (!review.getMember().getId().equals(memberId)) {
      throw new AccessDeniedException("본인의 리뷰만 삭제할 수 있습니다.");
    }
    // 외래 키 제약 때문에 리뷰를 삭제하기 전에 연결된 키워드를 먼저 삭제한다.
    reviewKeywordRepository.deleteByReviewId(reviewId);
    reviewRepository.delete(review);
  }
}
