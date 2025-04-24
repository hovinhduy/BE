package com.ktpm.paymentService.service;

import com.google.gson.JsonObject;
import com.ktpm.paymentService.model.OrderEventDTO;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PaymentService {

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    private final OkHttpClient client = new OkHttpClient();

    public void processPayment(OrderEventDTO order) {
        String callbackUrl = "http://localhost:8085/api/payments/callback"; // tùy biến
        String returnUrl = "http://localhost:3000/payment-success"; // front-end sẽ redirect đến đây sau khi thanh toán

        JsonObject payload = new JsonObject();
        payload.addProperty("orderCode", order.getId());
        payload.addProperty("amount", order.getTotalAmount().longValue()); // đơn vị: VND
        payload.addProperty("description", "Thanh toán đơn hàng #" + order.getId());
        payload.addProperty("returnUrl", returnUrl);
        payload.addProperty("cancelUrl", returnUrl); // fallback
        payload.addProperty("signature", checksumKey); // tùy theo PayOS yêu cầu checksum

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url("https://api.payos.vn/v1/payment-requests")
                .post(body)
                .addHeader("x-client-id", clientId)
                .addHeader("x-api-key", apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String resBody = response.body().string();
                System.out.println("✅ Created payment link: " + resBody);
                // Extract paymentUrl and notify customer via socket/email/...
            } else {
                System.out.println("❌ Payment failed: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
