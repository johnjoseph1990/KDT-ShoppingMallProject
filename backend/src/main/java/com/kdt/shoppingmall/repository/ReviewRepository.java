package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.review.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

  boolean existsByMemberIdAndProductId(Long memberId, Long productId);
}
