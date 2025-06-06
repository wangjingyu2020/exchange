package org.example.exchange.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.exchange.config.ExchangeRateProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
public class ApiMetricsAspect {

    @Autowired
    private ExchangeRateProperties exchangeRateProperties;

    private final ConcurrentHashMap<String, AtomicInteger> webClientCallCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> webClientSuccessCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> webClientFailureCount = new ConcurrentHashMap<>();

    @Around("execution(* org.example.exchange.utils.WebClientUtil.fetchDataWithFallback(..))")
    public Object trackExchangeRateCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        String apiUrl = extractUrl(joinPoint);
        String apiKey = getApiKey(apiUrl);

        if (apiKey == null) {
            return joinPoint.proceed();
        }

        webClientCallCount.computeIfAbsent(apiKey, k -> new AtomicInteger(0)).incrementAndGet();

        try {
            Object result = joinPoint.proceed();
            webClientSuccessCount.computeIfAbsent(apiKey, k -> new AtomicInteger(0)).incrementAndGet();
            return result;
        } catch (Exception e) {
            webClientFailureCount.computeIfAbsent(apiKey, k -> new AtomicInteger(0)).incrementAndGet();
            throw e;
        }

    }


    private String getApiKey(String apiUrl) {
        return exchangeRateProperties.getUrls().entrySet().stream()
                .filter(entry -> apiUrl.startsWith(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private String extractUrl(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        return "UnknownURL";
    }


    public Map<String, Object> getWebClientMetrics() {
        return Map.of(
                "apis", webClientCallCount.keySet().stream().map(apiKey -> Map.of(
                        "name", apiKey,
                        "totalRequests", webClientCallCount.getOrDefault(apiKey, new AtomicInteger(0)).get(),
                        "successfulRequests", webClientSuccessCount.getOrDefault(apiKey, new AtomicInteger(0)).get(),
                        "failedRequests", webClientFailureCount.getOrDefault(apiKey, new AtomicInteger(0)).get()
                )).toList()
        );
    }
}




