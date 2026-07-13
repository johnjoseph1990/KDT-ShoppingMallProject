package com.kdt.shoppingmall.controller;

import com.kdt.shoppingmall.dto.cart.CartItemQuantityRequest;
import com.kdt.shoppingmall.dto.cart.CartItemRequest;
import com.kdt.shoppingmall.dto.cart.CartItemResponse;
import com.kdt.shoppingmall.security.MemberPrincipal;
import com.kdt.shoppingmall.service.CartService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

  private final CartService cartService;

  public CartController(CartService cartService) {
    this.cartService = cartService;
  }

  @PostMapping
  public ResponseEntity<CartItemResponse> addItem(
      @AuthenticationPrincipal MemberPrincipal principal,
      @Valid @RequestBody CartItemRequest request) {
    CartItemResponse response = cartService.addItem(principal.getMember().getId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public List<CartItemResponse> getCart(@AuthenticationPrincipal MemberPrincipal principal) {
    return cartService.getCart(principal.getMember().getId());
  }

  @PutMapping("/{cartItemId}")
  public CartItemResponse updateQuantity(
      @AuthenticationPrincipal MemberPrincipal principal,
      @PathVariable Long cartItemId,
      @Valid @RequestBody CartItemQuantityRequest request) {
    return cartService.updateQuantity(
        principal.getMember().getId(), cartItemId, request.quantity());
  }

  @DeleteMapping("/{cartItemId}")
  public ResponseEntity<Void> removeItem(
      @AuthenticationPrincipal MemberPrincipal principal, @PathVariable Long cartItemId) {
    cartService.removeItem(principal.getMember().getId(), cartItemId);
    return ResponseEntity.noContent().build();
  }
}
