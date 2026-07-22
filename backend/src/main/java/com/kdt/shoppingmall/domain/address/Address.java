package com.kdt.shoppingmall.domain.address;

import com.kdt.shoppingmall.domain.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원이 저장해 둔 배송지 엔티티.
// 한 회원이 여러 배송지를 가질 수 있으므로 Member와 N:1 관계다.
// 주문 시 이 중 하나를 선택해 Order.deliveryXxx 필드에 복사 저장한다
// (주문 이후 회원이 배송지를 수정·삭제해도 주문 내역이 바뀌지 않도록).
@Entity
@Table(name = "address")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 이 배송지를 소유한 회원. @ManyToOne = "여러 Address가 하나의 Member에 속한다"
  @ManyToOne
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(nullable = false)
  private String recipientName; // 받는 분

  @Column(nullable = false)
  private String phone;

  @Column(nullable = false)
  private String zipCode;

  @Column(nullable = false)
  private String address;

  @Column private String addressDetail;

  @Column private String note;

  // 기본 배송지 여부. 목록 상단에 표시되고 주문 화면에서 자동 선택된다.
  @Column(nullable = false)
  private boolean isDefault;

  public Address(
      Member member,
      String recipientName,
      String phone,
      String zipCode,
      String address,
      String addressDetail,
      String note,
      boolean isDefault) {
    this.member = member;
    this.recipientName = recipientName;
    this.phone = phone;
    this.zipCode = zipCode;
    this.address = address;
    this.addressDetail = addressDetail;
    this.note = note;
    this.isDefault = isDefault;
  }

  // 배송지 정보를 수정한다. null이 아닌 필드만 덮어쓴다.
  public void update(
      String recipientName,
      String phone,
      String zipCode,
      String address,
      String addressDetail,
      String note) {
    this.recipientName = recipientName;
    this.phone = phone;
    this.zipCode = zipCode;
    this.address = address;
    this.addressDetail = addressDetail;
    this.note = note;
  }

  // 기본 배송지로 설정하거나 해제한다.
  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault;
  }
}
