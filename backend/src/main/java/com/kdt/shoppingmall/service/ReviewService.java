package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.order.OrderStatus;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.review.Review;
import com.kdt.shoppingmall.dto.review.ReviewRequest;
import com.kdt.shoppingmall.dto.review.ReviewResponse;
import com.kdt.shoppingmall.exception.DuplicateReviewException;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.OrderItemRepository;
import com.kdt.shoppingmall.repository.ProductRepository;
import com.kdt.shoppingmall.repository.ReviewRepository;
import java.util.List;
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
  // P0-1: 리뷰 작성 시 구매자 여부를 확인하는 데 쓰인다.
  private final OrderItemRepository orderItemRepository;

  // PAID·SHIPPING·DELIVERED 상태의 주문만 "구매 완료"로 인정한다.
  // ORDERED(결제 전)·CANCELED(취소)는 구매로 보지 않는다.
  private static final List<OrderStatus> PURCHASED_STATUSES =
      List.of(OrderStatus.PAID, OrderStatus.SHIPPING, OrderStatus.DELIVERED);

  public ReviewService(
      ReviewRepository reviewRepository,
      MemberRepository memberRepository,
      ProductRepository productRepository,
      OrderItemRepository orderItemRepository) {
    this.reviewRepository = reviewRepository;
    this.memberRepository = memberRepository;
    this.productRepository = productRepository;
    this.orderItemRepository = orderItemRepository;
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

    // [P0-1] 구매자 검증: 결제 완료 이상의 주문 이력이 없으면 작성 차단 (OWASP A01 접근 제어)
    // 삭제(본인 확인)와 달리 "작성" 권한은 구매 이력까지 필요하다.
    if (!orderItemRepository.existsByOrderMemberIdAndProductIdAndOrderStatusIn(
        memberId, productId, PURCHASED_STATUSES)) {
      throw new AccessDeniedException("구매자만 리뷰를 작성할 수 있습니다.");
    }

    if (reviewRepository.existsByMemberIdAndProductId(memberId, productId)) {
      throw new DuplicateReviewException("이미 리뷰를 작성한 상품입니다. productId=" + productId);
    }

    Review review =
        reviewRepository.save(new Review(member, product, request.rating(), request.content()));
    return ReviewResponse.from(review);
  }

  // [P1-5] 전체 반환(List) → 페이징(Page)으로 변경.
  // 리뷰가 수백 개 쌓여도 한 번에 다 내려주지 않고 페이지 단위로 잘라 응답한다.
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
    reviewRepository.delete(review);
  }
}
