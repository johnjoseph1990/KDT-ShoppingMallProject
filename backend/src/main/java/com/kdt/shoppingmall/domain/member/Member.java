package com.kdt.shoppingmall.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MemberRole role;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public Member(String email, String encodedPassword, String name, MemberRole role) {
    this.email = email;
    this.password = encodedPassword;
    this.name = name;
    this.role = role;
  }

  @jakarta.persistence.PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  // 이름과 비밀번호를 선택적으로 변경한다. null을 전달하면 해당 필드는 그대로 유지.
  public void update(String name, String encodedPassword) {
    if (name != null && !name.isBlank()) {
      this.name = name;
    }
    if (encodedPassword != null) {
      this.password = encodedPassword;
    }
  }
}
