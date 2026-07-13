package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.domain.cart.CartItem;
import com.kdt.shoppingmall.domain.member.Member;
import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.dto.cart.CartItemRequest;
import com.kdt.shoppingmall.dto.cart.CartItemResponse;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.CartItemRepository;
import com.kdt.shoppingmall.repository.MemberRepository;
import com.kdt.shoppingmall.repository.ProductRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CartService {

  private final CartItemRepository cartItemRepository;
  private final MemberRepository memberRepository;
  private final ProductRepository productRepository;

  public CartService(
      CartItemRepository cartItemRepository,
      MemberRepository memberRepository,
      ProductRepository productRepository) {
    this.cartItemRepository = cartItemRepository;
    this.memberRepository = memberRepository;
    this.productRepository = productRepository;
  }

  @Transactional
  public CartItemResponse addItem(Long memberId, CartItemRequest request) {
    Member member = getMemberOrThrow(memberId);
    Product product =
        productRepository
            .findById(request.productId())
            .orElseThrow(
                () -> new ResourceNotFoundException("상품을 찾을 수 없습니다. id=" + request.productId()));

    CartItem cartItem =
        cartItemRepository
            .findByMemberAndProduct(member, product)
            .map(
                existing -> {
                  existing.addQuantity(request.quantity());
                  return existing;
                })
            .orElseGet(
                () -> cartItemRepository.save(new CartItem(member, product, request.quantity())));

    return CartItemResponse.from(cartItem);
  }

  public List<CartItemResponse> getCart(Long memberId) {
    return cartItemRepository.findByMemberId(memberId).stream()
        .map(CartItemResponse::from)
        .toList();
  }

  @Transactional
  public CartItemResponse updateQuantity(Long memberId, Long cartItemId, int quantity) {
    CartItem cartItem = getOwnedCartItemOrThrow(memberId, cartItemId);
    cartItem.changeQuantity(quantity);
    return CartItemResponse.from(cartItem);
  }

  @Transactional
  public void removeItem(Long memberId, Long cartItemId) {
    CartItem cartItem = getOwnedCartItemOrThrow(memberId, cartItemId);
    cartItemRepository.delete(cartItem);
  }

  private CartItem getOwnedCartItemOrThrow(Long memberId, Long cartItemId) {
    CartItem cartItem =
        cartItemRepository
            .findById(cartItemId)
            .orElseThrow(
                () -> new ResourceNotFoundException("장바구니 항목을 찾을 수 없습니다. id=" + cartItemId));
    if (!cartItem.getMember().getId().equals(memberId)) {
      throw new AccessDeniedException("본인의 장바구니만 수정할 수 있습니다.");
    }
    return cartItem;
  }

  private Member getMemberOrThrow(Long memberId) {
    return memberRepository
        .findById(memberId)
        .orElseThrow(() -> new ResourceNotFoundException("회원을 찾을 수 없습니다. id=" + memberId));
  }
}
