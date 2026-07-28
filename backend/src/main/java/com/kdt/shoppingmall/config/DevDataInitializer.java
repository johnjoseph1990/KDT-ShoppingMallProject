package com.kdt.shoppingmall.config;

import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.member.MemberRole;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.product.ProductTag;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

// 개발 환경에서만 사용할 초기 데이터 주입 설정 클래스다.
// 애플리케이션이 시작될 때 관리자/회원/상품 데이터를 미리 넣어, 화면과 로그인 기능을 바로 확인할 수 있게 한다.
// 이미 데이터가 있으면 다시 넣지 않도록 막아서 중복 저장을 방지한다.
// @Profile("dev"): spring.profiles.active=dev 일 때만 이 빈이 등록된다.
// 운영 배포 시 SPRING_PROFILES_ACTIVE=prod 로 설정하면 시드 데이터가 삽입되지 않는다.
@Profile("dev")
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
      // 상품명, 설명, 가격, 재고, 이미지, 태그 순서로 정의
      //
      // [이미지 규칙] 이미지는 반드시 저장소 안(frontend/public/uploads)의 파일을 가리킨다.
      // 예전에는 Unsplash 주소를 직접 연결(핫링크)했는데 두 가지 사고가 났다:
      //   1) '논산 무항생제 목살'에 사람 얼굴 사진이 걸려 있었다 — 사진 ID(photo-1600180...)가
      //      사람이 읽을 수 없는 값이라 상품명과 맞는지 아무도 확인할 수 없었다.
      //   2) '홍성 한우 국거리' 사진이 외부에서 사라져 우리 화면만 깨졌다.
      // 이제 검증한 사진을 내려받아 저장소에 두므로 클론만 하면 같은 화면이 뜬다.
      // (출처: Unsplash 무료 라이선스. 실제 농가 사진이 아닌 상품 구분용 대체 이미지)
      // 이 규칙은 DevDataInitializerTest 가 자동 검증한다.
      List<Object[]> data =
          List.of(
              new Object[] {
                "충남 금산 당근",
                "삼십 년째 같은 밭에서 직접 만든 퇴비로 기른 당근. 주문 후 수확합니다.",
                8_500,
                50,
                "/uploads/geumsan-carrot.jpg",
                List.of("채소")
              },
              new Object[] {
                "논산 설향 딸기",
                "새벽 네 시에 완전히 익은 것만 골라 딴 딸기. 향이 진하고 당도가 높습니다.",
                18_000,
                30,
                "/uploads/nonsan-strawberry.jpg",
                List.of("과일")
              },
              new Object[] {
                "서천 바닷바람 쌈채소",
                "바닷바람이 닿는 노지에서 천천히 자란 잎채소. 조직이 단단해 쉽게 무르지 않습니다.",
                6_500,
                40,
                "/uploads/seocheon-salad-greens.jpg",
                List.of("채소")
              },
              new Object[] {
                "금산 뿌리채소 꾸러미",
                "당근, 우엉, 연근을 계절에 맞게 구성한 꾸러미. 매주 화요일 수확 후 발송합니다.",
                24_000,
                20,
                "/uploads/geumsan-root-vegetable-box.jpg",
                List.of("꾸러미")
              },
              new Object[] {
                "충남 가을 배",
                "과즙이 풍부하고 아삭한 충남산 신고배. 박스 단위 판매합니다.",
                35_000,
                15,
                "/uploads/chungnam-pear.jpg",
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
                "/uploads/seocheon-potato.jpg",
                List.of("채소")
              },
              new Object[] {
                "금산 인삼 정기배송",
                "4년근 금산 인삼을 매달 한 박스씩 보내드립니다. 첫 달 10% 할인.",
                45_000,
                10,
                "/uploads/geumsan-ginseng.jpg",
                List.of("꾸러미")
              },
              // 정육 카테고리 상품 10개 — 기존에는 관리자 UI로 실행 중 DB에만 넣어
              // git에 안 남아 클론/재시작 시 사라졌다. 재현성을 위해 시드 코드로 옮긴다.
              // 이미지는 Unsplash 무료 사진(상품 구분용 임시 대체)
              new Object[] {
                "홍성 암소 한우 등심",
                "충남 홍성에서 자란 1++ 암소 한우 등심. 마블링이 촘촘해 구이용으로 좋습니다.",
                39_000,
                20,
                "/uploads/hongseong-hanwoo-sirloin.jpg",
                List.of("정육")
              },
              new Object[] {
                "홍성 한우 불고기감",
                "앞다리·설도를 얇게 저민 불고기용. 양념 없이도 육향이 진합니다.",
                21_000,
                25,
                "/uploads/hongseong-hanwoo-bulgogi.jpg",
                List.of("정육")
              },
              new Object[] {
                "논산 무항생제 삼겹살",
                "무항생제 인증 농가의 국내산 삼겹살. 두툼하게 썰어 구워 먹기 좋습니다.",
                16_500,
                30,
                "/uploads/nonsan-pork-belly.jpg",
                List.of("정육")
              },
              new Object[] {
                "논산 무항생제 목살",
                "지방이 적고 담백한 국내산 목살. 수육·스테이크 어디에나 무난합니다.",
                15_000,
                30,
                "/uploads/nonsan-pork-shoulder.jpg",
                List.of("정육")
              },
              new Object[] {
                "서천 방목 토종닭",
                "노지에서 방목해 키운 토종닭 한 마리. 백숙·닭볶음탕용으로 씹는 맛이 좋습니다.",
                13_000,
                20,
                "/uploads/seocheon-free-range-chicken.jpg",
                List.of("정육")
              },
              new Object[] {
                "서천 닭가슴살 1kg",
                "당일 손질한 국내산 닭가슴살 1kg. 냉장 진공 포장으로 신선하게 보내드립니다.",
                11_000,
                40,
                "/uploads/seocheon-chicken-breast.jpg",
                List.of("정육")
              },
              new Object[] {
                "예산 참나무 훈제 오리",
                "참나무 장작으로 천천히 훈제한 오리 슬라이스. 데워서 바로 먹을 수 있습니다.",
                17_500,
                18,
                "/uploads/yesan-smoked-duck.jpg",
                List.of("정육")
              },
              new Object[] {
                "금산 흑돼지 갈비",
                "금산 흑돼지 생갈비. 잔칼집을 넣어 양념이 잘 배도록 손질했습니다.",
                23_000,
                15,
                "/uploads/geumsan-black-pork-ribs.jpg",
                List.of("정육")
              },
              new Object[] {
                "홍성 한우 국거리",
                "양지·사태를 섞은 국거리용. 오래 끓여도 질기지 않고 국물이 깊습니다.",
                18_000,
                22,
                "/uploads/hongseong-hanwoo-soup-beef.jpg",
                List.of("정육")
              },
              new Object[] {
                // 관리자 화면에서 '돼지불고기' → '닭발'로 바꾼 상품. 시드도 같은 이름으로 맞춰야
                // 재시작할 때 시드가 이 상품을 이름으로 찾아낸다. 이름이 어긋나 있으면
                // 시드가 '없는 상품'으로 보고 돼지불고기를 새로 넣어 비슷한 상품이 2개가 된다.
                "청양 양념 닭발",
                "청양고추를 더한 매콤 양념 닭발. 팬에 볶기만 하면 되는 밀키트형 상품입니다.",
                14_000,
                28,
                "/uploads/cheongyang-chicken-feet.jpg",
                List.of("정육")
              });

      for (Object[] row : data) {
        String name = (String) row[0];
        String imageUrl = (String) row[4];

        // 같은 이름의 상품이 이미 있는지 이름으로 확인한다(멱등). 예전의 count()>0 방식은
        // 상품이 하나라도 있으면 새 상품(정육)을 아예 못 넣어서, 이름 기준 체크로 바꿨다.
        //
        // Optional<Product>: "값이 있을 수도, 없을 수도 있는 상자"다. isPresent()로 열어보고 쓴다.
        Optional<Product> existing = productRepository.findByName(name);
        if (existing.isPresent()) {
          Product saved = existing.get();

          // 여기가 이번에 새로 필요해진 부분이다.
          // 위에서 시드의 이미지 주소를 전부 /uploads/... 로 고쳤지만, 이미 DB에 저장돼 있는
          // 상품은 그냥 continue 로 건너뛰면 예전의 잘못된 이미지를 계속 들고 있다.
          // (= 코드는 고쳐서 push 했는데 내 PC 화면은 그대로인 상황)
          //
          // 이미지가 시드 값과 다를 때만 맞춘다. 무조건 save() 하면 서버를 켤 때마다
          // 상품 수만큼 쓸모없는 UPDATE 문이 나가므로, 먼저 비교해서 걸러낸다.
          //
          // equals 를 imageUrl(시드 값) 쪽에서 호출하는 이유:
          // saved.getImageUrl() 은 null 일 수 있어서 saved.getImageUrl().equals(...) 로 쓰면
          // NullPointerException 이 난다. 절대 null 이 아닌 값을 왼쪽에 두는 게 안전한 습관이다.
          if (!imageUrl.equals(saved.getImageUrl())) {
            // 이미지 주소만 바꾼다. update(...)를 쓰면 관리자가 화면에서 고쳐둔
            // 가격·재고까지 시드 값으로 되돌아가 버린다.
            saved.changeImageUrl(imageUrl);

            // ApplicationRunner 는 트랜잭션 밖에서 실행되므로 JPA 더티 체킹이 동작하지 않는다.
            // save()를 직접 불러야 UPDATE 문이 DB로 나간다.
            productRepository.save(saved);
          }

          continue;
        }

        Product product = new Product(name, (String) row[1], (int) row[2], (int) row[3], imageUrl);

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
