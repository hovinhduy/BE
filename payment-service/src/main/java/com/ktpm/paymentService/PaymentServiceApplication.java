package com.ktpm.paymentService;

import com.ktpm.paymentService.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@SpringBootApplication
@Slf4j
public class PaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentServiceApplication.class, args);
	}

	@Bean
	public ApplicationRunner webhookRegistrationRunner(WebhookService webhookService,
			@Value("${payos.callback-url:}") String callbackUrl) {
		return args -> {
			if (callbackUrl != null && !callbackUrl.isEmpty()) {
				try {
					log.info("Đang đăng ký webhook tự động với URL: {}", callbackUrl);
					webhookService.verifyAndRegisterWebhook(callbackUrl);
				} catch (Exception e) {
					// Ghi log lỗi nhưng không dừng ứng dụng
					log.error("Không thể đăng ký webhook tự động: {}", e.getMessage(), e);
				}
			} else {
				log.warn("Không có URL webhook được cấu hình trong application.properties");
			}
		};
	}
}
