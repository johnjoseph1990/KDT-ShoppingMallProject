package com.kdt.shoppingmall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kdt.shoppingmall.config.TossPaymentsProperties;
import com.kdt.shoppingmall.domain.payment.PaymentMethod;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// MockRestServiceServer: 실제 네트워크 호출 없이 RestClient가 만드는 HTTP 요청/응답을 가짜로
// 흉내낸다. 토스 서버를 실제로 띄우지 않고도 confirm()/checkStatus()의 분기를 검증할 수 있다.
class TossPaymentProcessorTest {

  private TossPaymentProcessor buildProcessor(MockRestServiceServer[] serverHolder) {
    RestClient.Builder builder = RestClient.builder();
    serverHolder[0] = MockRestServiceServer.bindTo(builder).build();
    return new TossPaymentProcessor(builder, new TossPaymentsProperties("test_sk_dummy"));
  }

  @Test
  void confirm_카드결제_DONE응답시_DONE과_CARD_반환() {
    MockRestServiceServer[] holder = new MockRestServiceServer[1];
    TossPaymentProcessor processor = buildProcessor(holder);
    holder[0]
        .expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess("{\"status\":\"DONE\",\"method\":\"카드\"}", MediaType.APPLICATION_JSON));

    PaymentProcessor.ConfirmResult result = processor.confirm("pk_test", "ORDER-1", 20000);

    assertThat(result.status()).isEqualTo(PaymentProcessor.ConfirmStatus.DONE);
    assertThat(result.method()).isEqualTo(PaymentMethod.CARD);
  }

  @Test
  void confirm_가상계좌_WAITING_FOR_DEPOSIT응답시_계좌정보_함께_반환() {
    MockRestServiceServer[] holder = new MockRestServiceServer[1];
    TossPaymentProcessor processor = buildProcessor(holder);
    holder[0]
        .expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {
                  "status": "WAITING_FOR_DEPOSIT",
                  "method": "가상계좌",
                  "virtualAccount": {
                    "bankCode": "20",
                    "accountNumber": "1234567890",
                    "dueDate": "2026-08-01T23:59:59+09:00"
                  }
                }
                """,
                MediaType.APPLICATION_JSON));

    PaymentProcessor.ConfirmResult result = processor.confirm("pk_test", "ORDER-1", 20000);

    assertThat(result.status()).isEqualTo(PaymentProcessor.ConfirmStatus.WAITING_FOR_DEPOSIT);
    assertThat(result.method()).isEqualTo(PaymentMethod.VIRTUAL_ACCOUNT);
    assertThat(result.virtualAccountBankCode()).isEqualTo("20");
    assertThat(result.virtualAccountNumber()).isEqualTo("1234567890");
  }

  @Test
  void confirm_토스가_4xx응답시_FAILED_반환() {
    MockRestServiceServer[] holder = new MockRestServiceServer[1];
    TossPaymentProcessor processor = buildProcessor(holder);
    holder[0]
        .expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withStatus(HttpStatus.BAD_REQUEST)
                .body("{\"code\":\"REJECT_CARD_COMPANY\"}")
                .contentType(MediaType.APPLICATION_JSON));

    PaymentProcessor.ConfirmResult result = processor.confirm("pk_test", "ORDER-1", 20000);

    assertThat(result.status()).isEqualTo(PaymentProcessor.ConfirmStatus.FAILED);
  }

  @Test
  void confirm_네트워크오류시_FAILED_반환() {
    MockRestServiceServer[] holder = new MockRestServiceServer[1];
    TossPaymentProcessor processor = buildProcessor(holder);
    holder[0]
        .expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            request -> {
              throw new IOException("연결 시간 초과");
            });

    PaymentProcessor.ConfirmResult result = processor.confirm("pk_test", "ORDER-1", 20000);

    assertThat(result.status()).isEqualTo(PaymentProcessor.ConfirmStatus.FAILED);
  }

  @Test
  void checkStatus_입금완료시_DONE_반환() {
    MockRestServiceServer[] holder = new MockRestServiceServer[1];
    TossPaymentProcessor processor = buildProcessor(holder);
    holder[0]
        .expect(requestTo("https://api.tosspayments.com/v1/payments/pk_test"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess("{\"status\":\"DONE\",\"method\":\"가상계좌\"}", MediaType.APPLICATION_JSON));

    PaymentProcessor.ConfirmResult result = processor.checkStatus("pk_test");

    assertThat(result.status()).isEqualTo(PaymentProcessor.ConfirmStatus.DONE);
  }

  @Test
  void checkStatus_조회자체가_실패하면_예외를_그대로_던진다() {
    MockRestServiceServer[] holder = new MockRestServiceServer[1];
    TossPaymentProcessor processor = buildProcessor(holder);
    holder[0]
        .expect(requestTo("https://api.tosspayments.com/v1/payments/pk_test"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            request -> {
              throw new IOException("연결 시간 초과");
            });

    assertThatThrownBy(() -> processor.checkStatus("pk_test"))
        .isInstanceOf(RestClientException.class);
  }
}
