package org.example.exchange.utils;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WebClientUtil {

    private final WebClient webClient;

    public WebClientUtil(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Map<String, Object>> fetchDataWithFallback(String primaryUrl, String fallbackUrl) {
        return webClient.get().uri(primaryUrl).retrieve().bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(e -> fallbackUrl != null
                        ? webClient.get().uri(fallbackUrl).retrieve().bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .timeout(Duration.ofSeconds(5))
                        .onErrorResume( ex-> Mono.empty())
                        : Mono.empty());
    }

    public static Map<String, Object> formatExchangeData(String baseCur, List<String> symbols, Map<String, Object> response) {
        Map<String, BigDecimal> formattedRates = new HashMap<>();

        if (response.containsKey("rates")) {
            Map<String, Object> rates = (Map<String, Object>) response.get("rates");
            rates.forEach((currency, value) -> {
                if (symbols.contains(currency.toLowerCase()) && value instanceof Number) {
                    formattedRates.put(currency.toLowerCase(), BigDecimal.valueOf(((Number) value).doubleValue()));
                }
            });
        } else if (response.containsKey(baseCur.toLowerCase())) {
            Map<String, Object> rates = (Map<String, Object>) response.get(baseCur.toLowerCase());
            rates.forEach((currency, value) -> {
                if (symbols.contains(currency.toLowerCase()) && value instanceof Number) {
                    formattedRates.put(currency.toLowerCase(), BigDecimal.valueOf(((Number) value).doubleValue()));
                }
            });
        }

        return Map.of("base", baseCur.toLowerCase(), "rates", formattedRates);
    }
}
