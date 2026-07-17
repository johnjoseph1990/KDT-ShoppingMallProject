package com.kdt.shoppingmall.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.order.Order;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

// @DataJpaTest: JPA 관련 설정(엔티티, 리포지토리)만 로드하고 내장 H2로 실제 쿼리를 실행해보는 테스트.
// findByMemberIdOrderByCreatedAtDesc처럼 이름으로 쿼리를 만드는 메서드는 오타/조건 실수가
// 컴파일 타임에 안 걸리기 때문에 실제로 실행해서 검증할 가치가 있다.
@DataJpaTest
class OrderRepositoryTest {

  @Autowired private TestEntityManager em;

  @Autowired private OrderRepository orderRepository;

  private Member member;

  @BeforeEach
  void setUp() {
    member = em.persistAndFlush(new Member("test@test.com", "encoded", "테스터", MemberRole.USER));
  }

  @Test
  void findByMemberIdOrderByCreatedAtDesc_성공() throws InterruptedException {
    // 먼저 생성된 주문 (더 과거)
    Order older = em.persistAndFlush(new Order(member));
    // createdAt이 @PrePersist(LocalDateTime.now())로 채워지므로, 순서를 확실히 구분하기 위해
    // 아주 짧게 대기 후 두 번째 주문을 저장한다.
    Thread.sleep(10);
    // 나중에 생성된 주문 (더 최근)
    Order newer = em.persistAndFlush(new Order(member));

    List<Order> result = orderRepository.findByMemberIdOrderByCreatedAtDesc(member.getId());

    // 최신순(내림차순) 정렬이므로 나중에 만든 주문이 앞에 와야 한다.
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getId()).isEqualTo(newer.getId());
    assertThat(result.get(1).getId()).isEqualTo(older.getId());
  }

  @Test
  void findByMemberIdOrderByCreatedAtDesc_다른회원은제외() {
    Member other =
        em.persistAndFlush(new Member("other@test.com", "encoded", "다른유저", MemberRole.USER));
    em.persistAndFlush(new Order(other));

    List<Order> result = orderRepository.findByMemberIdOrderByCreatedAtDesc(member.getId());

    assertThat(result).isEmpty();
  }
}
