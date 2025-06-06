package org.example.exchange.service;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface ExchangeRateService {
    Mono<Map<String, Object>> getAverageExchangeRates(String baseCur, List<String> symbols);
    Map<String, Object> calculateAverageRates(String baseCur, List<Map<String, Object>> formattedResponses, List<String> symbols);
}

