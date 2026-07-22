package com.kdt.shoppingmall.controller;

import com.kdt.shoppingmall.dto.address.AddressRequest;
import com.kdt.shoppingmall.dto.address.AddressResponse;
import com.kdt.shoppingmall.security.MemberPrincipal;
import com.kdt.shoppingmall.service.AddressService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 배송지 CRUD REST API 컨트롤러.
// 모든 엔드포인트는 로그인이 필요하며 SecurityConfig의 anyRequest().authenticated()가 이를 보장한다.
@RestController
@RequestMapping("/api/addresses")
public class AddressController {

  private final AddressService addressService;

  public AddressController(AddressService addressService) {
    this.addressService = addressService;
  }

  // GET /api/addresses — 내 배송지 목록 조회
  @GetMapping
  public List<AddressResponse> getAll(@AuthenticationPrincipal MemberPrincipal principal) {
    return addressService.getAll(principal.getMember().getId());
  }

  // POST /api/addresses — 배송지 추가. 201 Created 반환.
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AddressResponse create(
      @AuthenticationPrincipal MemberPrincipal principal,
      @Valid @RequestBody AddressRequest request) {
    return addressService.create(principal.getMember().getId(), request);
  }

  // PUT /api/addresses/{id} — 배송지 수정
  @PutMapping("/{id}")
  public AddressResponse update(
      @AuthenticationPrincipal MemberPrincipal principal,
      @PathVariable Long id,
      @Valid @RequestBody AddressRequest request) {
    return addressService.update(principal.getMember().getId(), id, request);
  }

  // DELETE /api/addresses/{id} — 배송지 삭제. 204 No Content 반환.
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@AuthenticationPrincipal MemberPrincipal principal, @PathVariable Long id) {
    addressService.delete(principal.getMember().getId(), id);
  }
}
