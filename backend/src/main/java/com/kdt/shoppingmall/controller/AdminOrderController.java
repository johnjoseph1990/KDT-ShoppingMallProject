package com.kdt.shoppingmall.controller;

import com.kdt.shoppingmall.domain.order.OrderStatus;
import com.kdt.shoppingmall.dto.order.OrderResponse;
import com.kdt.shoppingmall.dto.order.OrderStatusUpdateRequest;
import com.kdt.shoppingmall.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Controller는 "HTTP 요청/응답"을 담당하는 웹 진입점 계층이다.
// AdminOrderController는 관리자 주문 관련 API만 모아서 처리한다.
@RestController
// @RestController를 붙이면 반환값을 JSON으로 자동 변환해 응답한다. (템플릿 화면 반환이 아님)
@RequestMapping("/api/admin/orders")
// @RequestMapping은 이 클래스의 공통 URL 시작 경로를 지정한다.
public class AdminOrderController {

  // final 필드는 생성자에서 1번만 주입되어, 실행 중 다른 객체로 바뀌지 않게 안전하게 고정된다.
  private final OrderService orderService;

  // 생성자 주입: 스프링이 OrderService 빈(객체)을 찾아 컨트롤러에 넣어준다.
  public AdminOrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping
  // @GetMapping: 조회(Read)용 HTTP GET 요청을 처리한다.
  public Page<OrderResponse> getAllOrders(
      // 상태로 필터링할 때만 넘기는 선택 파라미터 (생략하면 전체 주문 조회)
      // @RequestParam: URL 쿼리스트링 값(status=...)을 메서드 파라미터로 받는다.
      @RequestParam(required = false) OrderStatus status,
      // @PageableDefault: 페이지 크기/정렬 기본값을 지정한다.
      // 요청에 page,size,sort가 없으면 size=10, createdAt 내림차순이 자동 적용된다.
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    // 컨트롤러는 비즈니스 로직을 직접 처리하지 않고, 서비스 계층에 위임한다.
    return orderService.getAllOrders(status, pageable);
  }

  @PatchMapping("/{orderId}/status")
  // @PatchMapping: 리소스의 "일부 값"을 수정할 때 주로 사용하는 HTTP PATCH 요청을 처리한다.
  // @PreAuthorize: URL 패턴 설정과 이중으로 메서드 레벨에서 ADMIN 권한을 명시한다.
  // SecurityConfig URL 규칙이 변경·제거되더라도 이 메서드는 독립적으로 보호된다.
  @PreAuthorize("hasRole('ADMIN')")
  public OrderResponse changeOrderStatus(
      // @PathVariable: URL 경로의 {orderId} 값을 파라미터로 매핑한다.
      // @Valid: 요청 바디 DTO에 선언된 검증 규칙을 자동 검사한다.
      // @RequestBody: HTTP 요청 본문(JSON)을 Java 객체로 변환한다.
      @PathVariable Long orderId, @Valid @RequestBody OrderStatusUpdateRequest request) {
    // request.status()는 record DTO의 getter처럼 동작하며 변경할 주문 상태 값을 꺼낸다.
    return orderService.changeOrderStatus(orderId, request.status());
  }
}
