package org.example.exchange.controller;

import jakarta.validation.constraints.NotEmpty;
import org.example.exchange.service.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/exchangeRates")
@Validated
public class ExchangeRateController {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @GetMapping("/{baseCur}")
    public Mono<ResponseEntity<Map<String, Object>>> getExchangeRates(
            @PathVariable @NotEmpty String baseCur,
            @RequestParam @NotEmpty List<String> symbols) {

        String normalizedBaseCur = baseCur.toLowerCase();
        List<String> normalizedSymbols = symbols.stream().map(String::toLowerCase).collect(Collectors.toList());

        return exchangeRateService.getAverageExchangeRates(normalizedBaseCur, normalizedSymbols)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to query the exchange rate: " + ex.getMessage()))));
    }
}
