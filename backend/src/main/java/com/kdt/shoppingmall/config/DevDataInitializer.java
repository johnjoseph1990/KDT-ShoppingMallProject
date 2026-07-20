package com.kdt.shoppingmall.config;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.product.ProductTag;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.ProductRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

// 개발 환경에서만 사용할 초기 데이터 주입 설정 클래스다.
// 애플리케이션이 시작될 때 관리자/회원/상품 데이터를 미리 넣어, 화면과 로그인 기능을 바로 확인할 수 있게 한다.
// 이미 데이터가 있으면 다시 넣지 않도록 막아서 중복 저장을 방지한다.
@Configuration
public class DevDataInitializer {

  @Bean
  public ApplicationRunner seedAdmin(
      MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
    return (ApplicationArguments args) -> {
      if (!memberRepository.existsByEmail("admin@shop.com")) {
        memberRepository.save(
            new Member(
                "admin@shop.com", passwordEncoder.encode("admin1234"), "관리자", MemberRole.ADMIN));
      }
    };
  }

  // 일반 회원(USER) 계정을 미리 만들어 두는 시드 로직.
  // seedAdmin 과 동일하게 MemberRepository, PasswordEncoder 를 스프링이 주입(DI)해준다.
  @Bean
  public ApplicationRunner seedMembers(
      MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
    return (ApplicationArguments args) -> {
      // { 이메일(로그인 아이디), 비밀번호(평문), 이름 } 순서로 정의.
      // 첫 번째가 로그인 확인용 테스트 계정이고, 나머지 10개가 일반 회원이다.
      List<String[]> members =
          List.of(
              new String[] {"test@shop.com", "test1234", "테스트회원"},
              new String[] {"user01@shop.com", "user1234", "김민준"},
              new String[] {"user02@shop.com", "user1234", "이서연"},
              new String[] {"user03@shop.com", "user1234", "박도윤"},
              new String[] {"user04@shop.com", "user1234", "최지우"},
              new String[] {"user05@shop.com", "user1234", "정하은"},
              new String[] {"user06@shop.com", "user1234", "강시우"},
              new String[] {"user07@shop.com", "user1234", "조유나"},
              new String[] {"user08@shop.com", "user1234", "윤예준"},
              new String[] {"user09@shop.com", "user1234", "임수아"},
              new String[] {"user10@shop.com", "user1234", "한지호"});

      for (String[] m : members) {
        // 이미 존재하는 이메일이면 이번 반복은 건너뛴다 (seedAdmin과 동일한 패턴).
        // continue는 for문의 나머지 부분을 실행하지 않고 바로 다음 회원으로 넘어가게 한다.
        if (memberRepository.existsByEmail(m[0])) continue;

        // 비밀번호는 평문이 아니라 BCrypt 로 암호화(encode)해서 저장한다.
        memberRepository.save(
            new Member(m[0], passwordEncoder.encode(m[1]), m[2], MemberRole.USER));
      }
    };
  }

  @Bean
  public ApplicationRunner seedProducts(ProductRepository productRepository) {
    return (ApplicationArguments args) -> {
      // 이미 상품이 있으면 중복 삽입 방지
      if (productRepository.count() > 0) return;

      // 상품명, 설명, 가격, 재고, 이미지, 태그 순서로 정의
      // 이미지는 Unsplash 무료 라이선스 사진(실제 농가 사진 아님, 상품 구분용 임시 대체)
      List<Object[]> data =
          List.of(
              new Object[] {
                "충남 금산 당근",
                "삼십 년째 같은 밭에서 직접 만든 퇴비로 기른 당근. 주문 후 수확합니다.",
                8_500,
                50,
                "https://images.unsplash.com/photo-1605712776391-47f283ad1423?w=800&q=80&auto=format&fit=crop",
                List.of("채소")
              },
              new Object[] {
                "논산 설향 딸기",
                "새벽 네 시에 완전히 익은 것만 골라 딴 딸기. 향이 진하고 당도가 높습니다.",
                18_000,
                30,
                "https://images.unsplash.com/photo-1560691023-ca1f295a5173?w=800&q=80&auto=format&fit=crop",
                List.of("과일")
              },
              new Object[] {
                "서천 바닷바람 쌈채소",
                "바닷바람이 닿는 노지에서 천천히 자란 잎채소. 조직이 단단해 쉽게 무르지 않습니다.",
                6_500,
                40,
                "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=800&q=80&auto=format&fit=crop",
                List.of("채소")
              },
              new Object[] {
                "금산 뿌리채소 꾸러미",
                "당근, 우엉, 연근을 계절에 맞게 구성한 꾸러미. 매주 화요일 수확 후 발송합니다.",
                24_000,
                20,
                "https://images.unsplash.com/photo-1518843875459-f738682238a6?w=800&q=80&auto=format&fit=crop",
                List.of("꾸러미")
              },
              new Object[] {
                "충남 가을 배",
                "과즙이 풍부하고 아삭한 충남산 신고배. 박스 단위 판매합니다.",
                35_000,
                15,
                "https://images.unsplash.com/photo-1543363136-314062964bef?w=800&q=80&auto=format&fit=crop",
                List.of("과일")
              },
              new Object[] {
                "논산 딸기 수제잼",
                "첨가물 없이 딸기와 설탕만으로 만든 수제 잼. 냉장 보관 6개월.",
                12_000,
                25,
                "/uploads/nonsan-strawberry-jam.jpg",
                List.of("베이커리")
              },
              new Object[] {
                "서천 햇감자",
                "바닷바람을 맞고 자란 햇감자. 분이 많고 포슬포슬해서 쪄 먹기 좋습니다.",
                9_000,
                35,
                "https://images.unsplash.com/photo-1573196444577-af471298e034?w=800&q=80&auto=format&fit=crop",
                List.of("채소")
              },
              new Object[] {
                "금산 인삼 정기배송",
                "4년근 금산 인삼을 매달 한 박스씩 보내드립니다. 첫 달 10% 할인.",
                45_000,
                10,
                "https://images.unsplash.com/photo-1695798790639-c3c4294373ab?w=800&q=80&auto=format&fit=crop",
                List.of("꾸러미")
              });

      for (Object[] row : data) {
        Product product =
            new Product(
                (String) row[0], (String) row[1], (int) row[2], (int) row[3], (String) row[4]);

        @SuppressWarnings("unchecked")
        List<String> tagNames = (List<String>) row[5];
        for (String tagName : tagNames) {
          product.addTag(new ProductTag(tagName));
        }

        productRepository.save(product);
      }
    };
  }
}
