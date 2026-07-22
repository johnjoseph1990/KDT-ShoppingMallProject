package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.domain.address.Address;
import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.dto.address.AddressRequest;
import com.kdt.shoppingmall.dto.address.AddressResponse;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.AddressRepository;
import com.kdt.shoppingmall.repository.MemberRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 배송지 목록 조회·추가·수정·삭제 비즈니스 로직을 담당한다.
// @Transactional(readOnly = true)를 클래스 레벨에 걸어두면 기본이 읽기 전용이 되고,
// 변경 작업 메서드에만 @Transactional을 추가해서 쓰기 트랜잭션을 적용한다.
@Service
@Transactional(readOnly = true)
public class AddressService {

  private final AddressRepository addressRepository;
  private final MemberRepository memberRepository;

  public AddressService(AddressRepository addressRepository, MemberRepository memberRepository) {
    this.addressRepository = addressRepository;
    this.memberRepository = memberRepository;
  }

  // 내 배송지 목록 전체 조회
  public List<AddressResponse> getAll(Long memberId) {
    return addressRepository.findByMemberId(memberId).stream().map(AddressResponse::from).toList();
  }

  // 배송지 추가. isDefault=true면 기존 기본 배송지를 먼저 해제한다.
  @Transactional
  public AddressResponse create(Long memberId, AddressRequest request) {
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 회원입니다."));

    // 새 배송지를 기본으로 설정하는 경우, 기존 기본 배송지를 해제한다.
    // isDefault가 둘 이상이 되는 상황을 방지하는 핵심 로직.
    if (request.isDefault()) {
      clearDefaultAddresses(memberId);
    }

    Address address =
        new Address(
            member,
            request.recipientName(),
            request.phone(),
            request.zipCode(),
            request.address(),
            request.addressDetail(),
            request.note(),
            request.isDefault());

    return AddressResponse.from(addressRepository.save(address));
  }

  // 배송지 수정. 주소 내용을 변경하고 isDefault도 재설정한다.
  @Transactional
  public AddressResponse update(Long memberId, Long addressId, AddressRequest request) {
    Address address = findAddressOwnedBy(memberId, addressId);

    address.update(
        request.recipientName(),
        request.phone(),
        request.zipCode(),
        request.address(),
        request.addressDetail(),
        request.note());

    // 기본 배송지로 변경하는 경우, 기존 기본 배송지를 해제한다.
    if (request.isDefault() && !address.isDefault()) {
      clearDefaultAddresses(memberId);
    }
    address.setDefault(request.isDefault());

    return AddressResponse.from(address);
  }

  // 배송지 삭제
  @Transactional
  public void delete(Long memberId, Long addressId) {
    Address address = findAddressOwnedBy(memberId, addressId);
    addressRepository.delete(address);
  }

  // 특정 배송지가 해당 회원 소유인지 확인하는 공통 검증 메서드.
  // 다른 사람의 배송지를 수정·삭제하는 A01(접근 제어) 취약점을 방지한다.
  private Address findAddressOwnedBy(Long memberId, Long addressId) {
    Address address =
        addressRepository
            .findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 배송지입니다."));

    if (!address.getMember().getId().equals(memberId)) {
      throw new ResourceNotFoundException("존재하지 않는 배송지입니다.");
    }
    return address;
  }

  // 해당 회원의 모든 기본 배송지를 해제한다. 새 기본 배송지 설정 전에 호출한다.
  private void clearDefaultAddresses(Long memberId) {
    addressRepository.findByMemberIdAndIsDefaultTrue(memberId).forEach(a -> a.setDefault(false));
  }
}
