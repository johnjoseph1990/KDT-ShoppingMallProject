package com.kdt.shoppingmall.dto.order;

import com.kdt.shoppingmall.domain.order.Order;
import com.kdt.shoppingmall.domain.order.OrderStatus;
import com.kdt.shoppingmall.domain.payment.Payment;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    OrderStatus status,
    int totalPrice,
    // 상품 합계에 이미 포함된 배송비. 화면에서 "상품금액 + 배송비 = 합계"를 보여줄 때
    // totalPrice - shippingFee로 상품 합계를 역산할 수 있다.
    int shippingFee,
    LocalDateTime createdAt,
    List<OrderItemResponse> items,
    // 주문 시점의 배송지 정보 — 이후 회원 주소 변경과 무관하게 원본 유지
    String deliveryName,
    String deliveryPhone,
    String deliveryZipCode,
    String deliveryAddress,
    String deliveryAddressDetail,
    String deliveryNote,
    // 가상계좌 입금 대기 화면에 계좌정보를 보여줄 때만 채워진다. Order가 Payment를 직접 갖고 있지
    // 않아(Payment가 Order를 참조하는 방향) 호출하는 쪽에서 조회해 넘겨줘야 한다.
    PaymentInfo payment) {

  public static OrderResponse from(Order order) {
    return from(order, null);
  }

  public static OrderResponse from(Order order, Payment payment) {
    return new OrderResponse(
        order.getId(),
        order.getStatus(),
        order.getTotalPrice(),
        order.getShippingFee(),
        order.getCreatedAt(),
        order.getOrderItems().stream().map(OrderItemResponse::from).toList(),
        order.getDeliveryName(),
        order.getDeliveryPhone(),
        order.getDeliveryZipCode(),
        order.getDeliveryAddress(),
        order.getDeliveryAddressDetail(),
        order.getDeliveryNote(),
        payment != null ? PaymentInfo.from(payment) : null);
  }

  public record PaymentInfo(
      String virtualAccountBankCode,
      String virtualAccountNumber,
      OffsetDateTime virtualAccountDueDate) {

    public static PaymentInfo from(Payment payment) {
      return new PaymentInfo(
          payment.getVirtualAccountBankCode(),
          payment.getVirtualAccountNumber(),
          payment.getVirtualAccountDueDate());
    }
  }
}
