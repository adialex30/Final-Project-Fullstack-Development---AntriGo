//package com.antrigo.backend.payment;
//
//import com.antrigo.backend.domain.entity.Order;
//import com.antrigo.backend.exception.PaymentGatewayException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatusCode;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestClientException;
//import org.springframework.web.client.RestTemplate;
//
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.util.Base64;
//import java.util.HashMap;
//import java.util.Map;
//
//
//@Service
//@ConditionalOnExpression("!'${midtrans.server-key:}'.isEmpty()")
//@Slf4j
//public class RealMidtransGatewayService implements MidtransGatewayService {
//
//    private final String baseUrl;
//    private final String serverKey;
//    private final int expiryMinutes;
//    private final RestTemplate restTemplate = new RestTemplate();
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    public RealMidtransGatewayService(
//            @Value("${midtrans.server-key}") String serverKey,
//            @Value("${midtrans.sandbox:true}") String sandboxRaw,
//            @Value("${app.qris.expiry-minutes:15}") int expiryMinutes
//    ) {
//        this.serverKey = serverKey;
//        this.expiryMinutes = expiryMinutes;
//        boolean sandbox = !"false".equalsIgnoreCase(sandboxRaw == null ? "" : sandboxRaw.trim());
//        this.baseUrl = sandbox ? "https://api.sandbox.midtrans.com" : "https://api.midtrans.com";
//        log.info("Midtrans gateway aktif: {} (sandbox={})", baseUrl, sandbox);
//    }
//
//    @Override
//    public QrisChargeResult createQrisCharge(Order order) {
//        Map<String, Object> transactionDetails = new HashMap<>();
//        transactionDetails.put("order_id", order.getOrderNumber());
//        transactionDetails.put("gross_amount", order.getTotalAmount().longValue());
//
//        Map<String, Object> customExpiry = new HashMap<>();
//        customExpiry.put("expiry_duration", expiryMinutes);
//        customExpiry.put("unit", "minute");
//
//        Map<String, Object> body = new HashMap<>();
//        body.put("payment_type", "qris");
//        body.put("transaction_details", transactionDetails);
//        body.put("qris", Map.of("acquirer", "gopay"));
//        body.put("custom_expiry", customExpiry);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
//        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
//                .encodeToString((serverKey + ":").getBytes(StandardCharsets.UTF_8)));
//
//        try {
//            var response = restTemplate.postForEntity(
//                    baseUrl + "/v2/charge", new HttpEntity<>(body, headers), String.class);
//            JsonNode json = objectMapper.readTree(response.getBody());
//
//            HttpStatusCode httpStatus = response.getStatusCode();
//            String midtransStatusCode = json.path("status_code").asText("");
//            // Midtrans selalu balas HTTP 200/201 bahkan untuk error tervalidasi — status_code di
//            // body-nya (bukan HTTP status) yang menandakan sukses ("200"/"201") atau gagal.
//            if (!httpStatus.is2xxSuccessful() || (!midtransStatusCode.isEmpty()
//                    && !midtransStatusCode.equals("200") && !midtransStatusCode.equals("201"))) {
//                String message = json.path("status_message").asText("Gagal charge QRIS di Midtrans");
//                log.error("Midtrans charge gagal untuk order {}: {}", order.getOrderNumber(), json);
//                throw new PaymentGatewayException(message);
//            }
//
//            String qrString = json.path("qr_string").asText(null);
//            String transactionId = json.path("transaction_id").asText(null);
//            if (qrString == null || transactionId == null) {
//                log.error("Response Midtrans tidak berisi qr_string/transaction_id untuk order {}: {}",
//                        order.getOrderNumber(), json);
//                throw new PaymentGatewayException("Response Midtrans tidak lengkap");
//            }
//
//            return new QrisChargeResult(transactionId, qrString, LocalDateTime.now().plusMinutes(expiryMinutes));
//        } catch (PaymentGatewayException e) {
//            throw e;
//        } catch (RestClientException e) {
//            log.error("Gagal menghubungi Midtrans untuk order {}", order.getOrderNumber(), e);
//            throw new PaymentGatewayException("Tidak bisa menghubungi payment gateway, coba lagi");
//        } catch (Exception e) {
//            log.error("Gagal memproses response Midtrans untuk order {}", order.getOrderNumber(), e);
//            throw new PaymentGatewayException("Gagal memproses response payment gateway");
//        }
//    }
//}

package com.antrigo.backend.payment;

import com.antrigo.backend.domain.entity.Order;
import com.antrigo.backend.exception.PaymentGatewayException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@ConditionalOnExpression("!'${midtrans.server-key:}'.isEmpty()")
@Slf4j
public class RealMidtransGatewayService implements MidtransGatewayService {

    private static final String GENERATE_QR_CODE_ACTION = "generate-qr-code";

    private final String baseUrl;
    private final String serverKey;
    private final int expiryMinutes;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RealMidtransGatewayService(
            @Value("${midtrans.server-key}") String serverKey,
            @Value("${midtrans.sandbox:true}") String sandboxRaw,
            @Value("${app.qris.expiry-minutes:15}") int expiryMinutes
    ) {
        this.serverKey = serverKey;
        this.expiryMinutes = expiryMinutes;
        boolean sandbox = !"false".equalsIgnoreCase(sandboxRaw == null ? "" : sandboxRaw.trim());
        this.baseUrl = sandbox ? "https://api.sandbox.midtrans.com" : "https://api.midtrans.com";
        log.info("Midtrans gateway aktif: {} (sandbox={})", baseUrl, sandbox);
    }

    @Override
    public QrisChargeResult createQrisCharge(Order order) {
        Map<String, Object> transactionDetails = new HashMap<>();
        transactionDetails.put("order_id", order.getOrderNumber());
        transactionDetails.put("gross_amount", order.getTotalAmount().longValue());

        Map<String, Object> customExpiry = new HashMap<>();
        customExpiry.put("expiry_duration", expiryMinutes);
        customExpiry.put("unit", "minute");

        Map<String, Object> body = new HashMap<>();
        body.put("payment_type", "qris");
        body.put("transaction_details", transactionDetails);
        body.put("qris", Map.of("acquirer", "gopay"));
        body.put("custom_expiry", customExpiry);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                .encodeToString((serverKey + ":").getBytes(StandardCharsets.UTF_8)));

        try {
            var response = restTemplate.postForEntity(
                    baseUrl + "/v2/charge", new HttpEntity<>(body, headers), String.class);
            JsonNode json = objectMapper.readTree(response.getBody());

            HttpStatusCode httpStatus = response.getStatusCode();
            String midtransStatusCode = json.path("status_code").asText("");
            // Midtrans selalu balas HTTP 200/201 bahkan untuk error tervalidasi — status_code di
            // body-nya (bukan HTTP status) yang menandakan sukses ("200"/"201") atau gagal.
            if (!httpStatus.is2xxSuccessful() || (!midtransStatusCode.isEmpty()
                    && !midtransStatusCode.equals("200") && !midtransStatusCode.equals("201"))) {
                String message = json.path("status_message").asText("Gagal charge QRIS di Midtrans");
                log.error("Midtrans charge gagal untuk order {}: {}", order.getOrderNumber(), json);
                throw new PaymentGatewayException(message);
            }

            String transactionId = json.path("transaction_id").asText(null);
            String qrImageUrl = extractQrImageUrl(json);
            if (qrImageUrl == null || transactionId == null) {
                log.error("Response Midtrans tidak berisi actions[generate-qr-code].url/transaction_id untuk order {}: {}",
                        order.getOrderNumber(), json);
                throw new PaymentGatewayException("Response Midtrans tidak lengkap");
            }

            return new QrisChargeResult(transactionId, qrImageUrl, LocalDateTime.now().plusMinutes(expiryMinutes));
        } catch (PaymentGatewayException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Gagal menghubungi Midtrans untuk order {}", order.getOrderNumber(), e);
            throw new PaymentGatewayException("Tidak bisa menghubungi payment gateway, coba lagi");
        } catch (Exception e) {
            log.error("Gagal memproses response Midtrans untuk order {}", order.getOrderNumber(), e);
            throw new PaymentGatewayException("Gagal memproses response payment gateway");
        }
    }

    /** Ambil url dari actions[] yang name-nya "generate-qr-code" (PNG tanpa border ASPI). */
    private String extractQrImageUrl(JsonNode json) {
        for (JsonNode action : json.path("actions")) {
            if (GENERATE_QR_CODE_ACTION.equals(action.path("name").asText())) {
                return action.path("url").asText(null);
            }
        }
        return null;
    }
}