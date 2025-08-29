package com.example.SpringMongoProject.Controller;

import com.example.SpringMongoProject.Entity.Booking;
import com.example.SpringMongoProject.Service.BookingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class PaymentController {

    @Autowired
    private BookingService bookingService;

    @Value("${payos.client.id}")
    private String payosClientId;
    @Value("${payos.api.key}")
    private String payosApiKey;
    @Value("${payos.checksum.key}")
    private String payosChecksumKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/create-payment-link")
    public ResponseEntity<Map<String, Object>> createPaymentLink(@RequestBody Map<String, String> body) {
        try {
            String bookingId = body.get("bookingId");
            Booking booking = bookingService.prepareBookingForPayment(bookingId);

            if (booking.getTotalPrice() == null) {
                throw new IllegalStateException("Đơn hàng này không có thông tin tổng giá tiền. Vui lòng kiểm tra lại.");
            }

            long orderCode = booking.getPaymentOrderCode();

            // ✅ THAY ĐỔI: Gán cứng giá trị thanh toán là 2000 VNĐ
            int amount = 2000;

            String description = "TT don hang " + orderCode;
            String returnUrl = "http://localhost:5173/my-trips";
            String cancelUrl = "http://localhost:5173/checkout/" + bookingId;

            ObjectNode paymentDataNode = objectMapper.createObjectNode();
            paymentDataNode.put("orderCode", orderCode);
            paymentDataNode.put("amount", amount);
            paymentDataNode.put("description", description);
            paymentDataNode.put("returnUrl", returnUrl);
            paymentDataNode.put("cancelUrl", cancelUrl);

            String dataToSign = "amount=" + amount + "&cancelUrl=" + cancelUrl + "&description=" + description + "&orderCode=" + orderCode + "&returnUrl=" + returnUrl;
            String signature = createSignature(dataToSign, payosChecksumKey);
            paymentDataNode.put("signature", signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", payosClientId);
            headers.set("x-api-key", payosApiKey);

            HttpEntity<String> request = new HttpEntity<>(paymentDataNode.toString(), headers);
            String payosUrl = "https://api-merchant.payos.vn/v2/payment-requests";
            ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity(payosUrl, request, JsonNode.class);

            JsonNode responseBody = responseEntity.getBody();
            if (responseBody != null && "00".equals(responseBody.get("code").asText())) {
                String checkoutUrl = responseBody.get("data").get("checkoutUrl").asText();
                return ResponseEntity.ok(Map.of("success", true, "checkoutUrl", checkoutUrl));
            } else {
                String errorMessage = responseBody != null ? responseBody.get("desc").asText() : "Lỗi không xác định từ PayOS.";
                System.out.println("PayOS Error: " + (responseBody != null ? responseBody.toString() : "Empty Response"));
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", errorMessage));
            }

        } catch (Exception e) {
            e.printStackTrace();
            HttpStatus status = e instanceof IllegalStateException ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/payos-webhook")
    public ResponseEntity<String> receiveWebhook(@RequestBody JsonNode webhookData) {
        System.out.println("====== WEBHOOK RECEIVED ======");
        System.out.println(webhookData.toString());

        try {
            JsonNode data = webhookData.get("data");
            if (data != null && "00".equals(data.get("code").asText()) && data.has("orderCode")) {
                long orderCode = data.get("orderCode").asLong();
                System.out.println("Processing successful payment for orderCode: " + orderCode);

                bookingService.confirmBookingByOrderCode(orderCode);

                System.out.println("====== WEBHOOK PROCESSED SUCCESSFULLY for orderCode: " + orderCode + " ======");
            } else {
                System.out.println("Webhook received but no action taken (not a successful payment or no orderCode).");
            }
            return ResponseEntity.ok("Webhook received");
        } catch (Exception e) {
            System.err.println("====== ERROR PROCESSING WEBHOOK ======");
            e.printStackTrace();
            return ResponseEntity.ok("Error processing webhook, but acknowledged.");
        }
    }

    private String createSignature(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}