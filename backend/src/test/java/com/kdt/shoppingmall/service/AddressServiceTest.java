package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.kdt.shoppingmall.domain.address.Address;
import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.dto.address.AddressRequest;
import com.kdt.shoppingmall.dto.address.AddressResponse;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.AddressRepository;
import com.kdt.shoppingmall.repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

  @Mock private AddressRepository addressRepository;

  @Mock private MemberRepository memberRepository;

  @InjectMocks private AddressService addressService;

  private Member member;

  @BeforeEach
  void setUp() {
    member = new Member("test@test.com", "encoded", "테스터", MemberRole.USER);
    ReflectionTestUtils.setField(member, "id", 1L);
  }

  private Address makeAddress(Long id, boolean isDefault) {
    Address address =
        new Address(member, "홍길동", "010-1234-5678", "12345", "서울시 강남구", "101호", "문 앞", isDefault);
    ReflectionTestUtils.setField(address, "id", id);
    return address;
  }

  @Test
  void getAll_배송지목록_반환() {
    Address a1 = makeAddress(1L, true);
    Address a2 = makeAddress(2L, false);
    given(addressRepository.findByMemberId(1L)).willReturn(List.of(a1, a2));

    List<AddressResponse> result = addressService.getAll(1L);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).isDefault()).isTrue();
  }

  @Test
  void create_배송지_추가_성공() {
    AddressRequest request =
        new AddressRequest("홍길동", "010-1234-5678", "12345", "서울시 강남구", "101호", "문 앞", false);
    Address saved = makeAddress(1L, false);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(addressRepository.save(any(Address.class))).willReturn(saved);

    AddressResponse result = addressService.create(1L, request);

    assertThat(result.recipientName()).isEqualTo("홍길동");
    verify(addressRepository).save(any(Address.class));
  }

  @Test
  void create_기본배송지_설정시_기존기본_해제() {
    // isDefault=true로 추가하면 기존 기본 배송지가 해제되어야 한다
    Address existingDefault = makeAddress(99L, true);
    AddressRequest request =
        new AddressRequest("김철수", "010-9876-5432", "54321", "부산시", null, null, true);
    Address saved = makeAddress(2L, true);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(addressRepository.findByMemberIdAndIsDefaultTrue(1L))
        .willReturn(List.of(existingDefault));
    given(addressRepository.save(any(Address.class))).willReturn(saved);

    addressService.create(1L, request);

    // 기존 기본 배송지가 해제됐는지 확인
    assertThat(existingDefault.isDefault()).isFalse();
  }

  @Test
  void create_존재하지않는_회원_예외발생() {
    AddressRequest request =
        new AddressRequest("홍길동", "010-1234-5678", "12345", "서울시", null, null, false);
    given(memberRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> addressService.create(99L, request))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(addressRepository, never()).save(any());
  }

  @Test
  void update_배송지_수정_성공() {
    Address address = makeAddress(1L, false);
    AddressRequest request =
        new AddressRequest("수정된이름", "010-0000-0000", "99999", "인천시", "202호", null, false);
    given(addressRepository.findById(1L)).willReturn(Optional.of(address));

    AddressResponse result = addressService.update(1L, 1L, request);

    assertThat(result.recipientName()).isEqualTo("수정된이름");
    assertThat(result.phone()).isEqualTo("010-0000-0000");
  }

  @Test
  void update_다른회원_배송지_수정시_예외발생() {
    // 다른 회원(memberId=2)의 배송지(memberId=1 소유)를 수정하면 예외가 발생해야 한다
    Address address = makeAddress(1L, false); // member id=1 소유
    given(addressRepository.findById(1L)).willReturn(Optional.of(address));

    AddressRequest request =
        new AddressRequest("해커", "010-1111-1111", "11111", "해커의집", null, null, false);

    // memberId=2로 memberId=1의 배송지 수정 시도
    assertThatThrownBy(() -> addressService.update(2L, 1L, request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void delete_배송지_삭제_성공() {
    Address address = makeAddress(1L, false);
    given(addressRepository.findById(1L)).willReturn(Optional.of(address));

    addressService.delete(1L, 1L);

    verify(addressRepository).delete(address);
  }

  @Test
  void delete_존재하지않는_배송지_예외발생() {
    given(addressRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> addressService.delete(1L, 99L))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(addressRepository, never()).delete(any());
  }
}
