package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.address.Address;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository를 상속하면 save/findById/delete 등 기본 CRUD 메서드가 자동으로 생긴다.
public interface AddressRepository extends JpaRepository<Address, Long> {

  // "findBy + 필드명" 네이밍 규칙으로 Spring Data가 SELECT 쿼리를 자동 생성한다.
  List<Address> findByMemberId(Long memberId);

  // 특정 회원의 현재 기본 배송지를 조회한다. 기본 배송지 교체 시 기존 것을 해제하기 위해 사용.
  List<Address> findByMemberIdAndIsDefaultTrue(Long memberId);
}
