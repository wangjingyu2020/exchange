package org.example.exchange.service.impl;

import org.example.exchange.service.ExchangeRateService;
import org.example.exchange.utils.WebClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {

    @Autowired
    private WebClientUtil webClientUtil;

    @Value("${exchange-rate.urls.frankfurter-url}")
    private String frankfurterUrl;

    @Value("${exchange-rate.urls.primary-currency-url}")
    private String primaryCurrencyUrl;

    @Value("${exchange-rate.urls.fallback-currency-url}")
    private String fallbackCurrencyUrl;

    @Value("${cache-expiry-seconds}")
    private int cacheExpirySeconds;

    private static class CacheEntry {
        Instant timestamp;
        Map<String, BigDecimal> rates;

        CacheEntry(Map<String, BigDecimal> rates) {
            this.timestamp = Instant.now();
            this.rates = rates;
        }
    }

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();


    @Override
    public Mono<Map<String, Object>> getAverageExchangeRates(String baseCur, List<String> symbols) {
        String cacheKey = baseCur.toLowerCase() + ":" + String.join(",", symbols.stream().map(String::toLowerCase).sorted().toList());
        CacheEntry cachedResult = cache.get(cacheKey);


        if (cachedResult != null && Instant.now().isBefore(cachedResult.timestamp.plusSeconds(cacheExpirySeconds))) {
            return Mono.just(Map.of("base", baseCur, "rates", cachedResult.rates));
        }

        String frankfurterUrlWithParams = frankfurterUrl + "/latest?base=" + baseCur + "&symbols=" + String.join(",", symbols);
        String primaryCurrencyUrlWithParams = primaryCurrencyUrl + "/currencies/" + baseCur + ".json";
        String fallbackCurrencyUrlWithParams = fallbackCurrencyUrl + "/currencies/" + baseCur + ".json";


        Mono<Map<String, Object>> frankfurterResponse = webClientUtil.fetchDataWithFallback(frankfurterUrlWithParams, null)
                .map(response -> WebClientUtil.formatExchangeData(baseCur, symbols,response))
                .switchIfEmpty(Mono.error(new RuntimeException("Frankfurter API Failed")));

        Mono<Map<String, Object>> currencyApiResponse = webClientUtil.fetchDataWithFallback(primaryCurrencyUrlWithParams, fallbackCurrencyUrlWithParams)
                .map(response -> WebClientUtil.formatExchangeData(baseCur, symbols,response))
                .switchIfEmpty(Mono.error(new RuntimeException("Free Currency Exchange Rates API Failed")));

        return frankfurterResponse
                .concatWith(currencyApiResponse)
                .collectList()
                .map(responses -> calculateAverageRates(baseCur, responses, symbols))
                .doOnNext(result -> {
                    cache.put(cacheKey, new CacheEntry((Map<String, BigDecimal>) result.get("rates")));
                });


    }

    @Override
    public Map<String, Object> calculateAverageRates(String baseCur, List<Map<String, Object>> formattedResponses, List<String> symbols) {
        Map<String, BigDecimal> aggregatedRates = new HashMap<>();
        int apiCount = (int) formattedResponses.stream().filter(response -> !response.isEmpty()).count();

        for (Map<String, Object> response : formattedResponses) {
            Map<String, BigDecimal> rates = (Map<String, BigDecimal>) response.get("rates");
            rates.forEach((currency, rate) -> {
                if (symbols.contains(currency)) {
                    aggregatedRates.merge(currency, rate, BigDecimal::add);
                }
            });
        }
        if (apiCount > 0) {
            aggregatedRates.replaceAll((key, value) -> value.divide(BigDecimal.valueOf(apiCount), 6, BigDecimal.ROUND_HALF_UP));
        }

        return Map.of("base", baseCur, "rates", aggregatedRates);
    }



}
