package com.example.gatewayservice.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class RateLimitExceededFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(error -> {
                    // Kiểm tra nếu lỗi liên quan đến rate limit (status code 429)
                    if (exchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

                        String errorMessage = "{\"status\":" + HttpStatus.TOO_MANY_REQUESTS.value() +
                                ",\"error\":\"Too Many Requests\"," +
                                "\"message\":\"Quá nhiều yêu cầu. Vui lòng thử lại sau ít phút.\"}";

                        DataBuffer buffer = exchange.getResponse()
                                .bufferFactory()
                                .wrap(errorMessage.getBytes(StandardCharsets.UTF_8));

                        return exchange.getResponse().writeWith(Mono.just(buffer));
                    }

                    return Mono.error(error);
                });
    }

    @Override
    public int getOrder() {
        // Execute after rate limiter filter
        return Ordered.LOWEST_PRECEDENCE;
    }
}