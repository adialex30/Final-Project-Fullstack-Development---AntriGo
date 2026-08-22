package com.antrigo.backend.payment;

import com.antrigo.backend.domain.entity.Order;
import com.antrigo.backend.exception.PaymentGatewayException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Service
@ConditionalOnExpression("!'${midtrans.server-key:}'.isEmpty()")
@Slf4j
public class RealMidtransGatewayService implements MidtransGatewayService {

    private static final String ACTION_GENERATE_QR_CODE = "generate-qr-code";
    private static final String ACTION_GENERATE_QR_CODE_V2 = "generate-qr-code-v2";

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

        HttpHeaders headers = authJsonHeaders();

        try {
            var response = restTemplate.postForEntity(
                    baseUrl + "/v2/charge", new HttpEntity<>(body, headers), String.class);
            JsonNode json = objectMapper.readTree(response.getBody());

            HttpStatusCode httpStatus = response.getStatusCode();
            String midtransStatusCode = json.path("status_code").asText("");
            if (!httpStatus.is2xxSuccessful() || (!midtransStatusCode.isEmpty()
                    && !midtransStatusCode.equals("200") && !midtransStatusCode.equals("201"))) {
                String message = json.path("status_message").asText("Gagal charge QRIS di Midtrans");
                log.error("Midtrans charge gagal untuk order {}: {}", order.getOrderNumber(), json);
                throw new PaymentGatewayException(message);
            }

            String transactionId = json.path("transaction_id").asText(null);
            if (transactionId == null) {
                log.error("Response Midtrans tidak berisi transaction_id untuk order {}: {}",
                        order.getOrderNumber(), json);
                throw new PaymentGatewayException("Response Midtrans tidak lengkap");
            }

            String qrPayload = resolveQrPayload(json, order.getOrderNumber());

            return new QrisChargeResult(transactionId, qrPayload, LocalDateTime.now().plusMinutes(expiryMinutes));
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

    /**
     * qrPayload diisi LANGSUNG dari URL yang Midtrans kasih sendiri di {@code actions[].url}
     * ({@code generate-qr-code} / {@code generate-qr-code-v2}) — bukan qr_string, bukan hasil
     * generate/fetch ulang di backend. Ini satu-satunya cara supaya "Copy image address" di
     * frontend beneran nunjukin URL asli Midtrans (api(.sandbox).midtrans.com/...), sesuai
     * permintaan eksplisit.
     * <p>
     * CATATAN PENTING: endpoint ini didokumentasikan Midtrans untuk "ditampilkan langsung ke
     * customer via POS/frontend" — kemungkinan besar publik (gak butuh Authorization). Tapi ini
     * BELUM diverifikasi langsung. Kalau ternyata butuh auth, gambar QR akan GAGAL muncul di
     * <img> (balik ke bug paling awal). WAJIB dites: checkout sekali, lalu buka URL hasil "Copy
     * image address" itu di tab baru (dalam window masa berlaku QR) — kalau gambarnya muncul,
     * ini aman dipakai; kalau nggak, harus balik ke pendekatan backend-proxy (base64 data URI /
     * endpoint sendiri).
     */
    private String resolveQrPayload(JsonNode json, String orderNumber) {
        String qrCodeUrl = findActionUrl(json.path("actions"), ACTION_GENERATE_QR_CODE);
        if (qrCodeUrl == null) {
            qrCodeUrl = findActionUrl(json.path("actions"), ACTION_GENERATE_QR_CODE_V2);
        }
        if (qrCodeUrl != null) {
            log.info("Midtrans charge sukses untuk order {} — pakai URL Midtrans langsung: {}",
                    orderNumber, qrCodeUrl);
            return qrCodeUrl;
        }

        String qrString = json.path("qr_string").asText(null);
        if (qrString != null && !qrString.isBlank()) {
            log.info("Midtrans charge sukses untuk order {} — actions[] kosong, fallback generate PNG dari qr_string (ZXing).",
                    orderNumber);
            return renderQrStringAsDataUri(qrString, orderNumber);
        }

        log.error("Response Midtrans tidak berisi actions[generate-qr-code] maupun qr_string untuk order {}: {}",
                orderNumber, json);
        throw new PaymentGatewayException("Response Midtrans tidak lengkap");
    }

    private String renderQrStringAsDataUri(String qrString, String orderNumber) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(qrString, BarcodeFormat.QR_CODE, 320, 320, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            log.error("Gagal generate PNG QR dari qr_string untuk order {}", orderNumber, e);
            throw new PaymentGatewayException("Gagal membuat gambar QRIS");
        }
    }

    private String findActionUrl(JsonNode actionsNode, String actionName) {
        if (!actionsNode.isArray()) return null;
        for (JsonNode action : actionsNode) {
            if (actionName.equals(action.path("name").asText(null))) {
                return action.path("url").asText(null);
            }
        }
        return null;
    }

    private HttpHeaders authJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, basicAuthHeader());
        return headers;
    }

    private String basicAuthHeader() {
        return "Basic " + Base64.getEncoder().encodeToString((serverKey + ":").getBytes(StandardCharsets.UTF_8));
    }
}