package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.review.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

  boolean existsByMemberIdAndProductId(Long memberId, Long productId);

  @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
  Double findAverageRatingByProductId(@Param("productId") Long productId);
}
