package org.example.exchange;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.example.exchange.service.impl.ExchangeRateServiceImpl;
import org.example.exchange.utils.WebClientUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WebClientUtilTest {

    private MockWebServer mockWebServer;
    private WebClientUtil webClientUtil;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        webClientUtil = new WebClientUtil(WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }


    @Test
    void testFetchDataWithPrimarySuccess() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"date\": \"2025-06-04\", \"eur\": {"
                        + "\"1inch\": 5.17615643,"
                        + "\"aave\": 0.0042849285,"
                        + "\"ada\": 1.58114662,"
                        + "\"aed\": 4.18161263,"
                        + "\"afn\": 79.22778031"
                        + "}}")
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json"));

        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"date\": \"2025-06-04\", \"eur\": {"
                        + "\"1inch\": 5.17615643,"
                        + "\"aave\": 0.0042849285,"
                        + "\"ada\": 1.58114662,"
                        + "\"aed\": 4.18161263,"
                        + "\"afn\": 79.22778031"
                        + "}}")
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json"));


        Mono<Map<String, Object>> result = webClientUtil.fetchDataWithFallback(
                mockWebServer.url("/primary").toString(),
                mockWebServer.url("/fallback").toString()
        );

        StepVerifier.create(result).assertNext(response -> {
                    assertNotNull(response, "API should return data");
                })
                .verifyComplete();
    }

    @Test
    void testFetchDataWithPrimaryFailureFallbackSuccess() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500)); // Primary return
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"base\": \"USD\", \"date\": \"2025-06-05\", \"rates\": {"
                        + "\"AUD\": 1.5364,"
                        + "\"BGN\": 1.7122"
                        + "}}")
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json"));

        Mono<Map<String, Object>> result = webClientUtil.fetchDataWithFallback(
                mockWebServer.url("/primary").toString(),
                mockWebServer.url("/fallback").toString()
        );

        StepVerifier.create(result).assertNext(response -> {
                    assertNotNull(response, "API should return data");
                })
                .verifyComplete();
    }

    @Test
    void testFetchDataWithBothFailure() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        Mono<Map<String, Object>> result = webClientUtil.fetchDataWithFallback(
                mockWebServer.url("/primary").toString(),
                mockWebServer.url("/fallback").toString()
        );

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }


    @Test
    void testFormatExchangeDataWithRates() {
        Map<String, Object> response = Map.of(
                "rates", Map.of("USD", 1.2, "EUR", 0.9, "JPY", 110.5)
        );

        List<String> symbols = List.of("usd", "eur");

        Map<String, Object> result = WebClientUtil.formatExchangeData("USD", symbols, response);

        assertNotNull(result);
        assertEquals("usd", result.get("base"));
        Map<String, BigDecimal> rates = (Map<String, BigDecimal>) result.get("rates");
        assertEquals(BigDecimal.valueOf(1.2), rates.get("usd"));
        assertEquals(BigDecimal.valueOf(0.9), rates.get("eur"));
        assertNull(rates.get("jpy"));
    }

    @Test
    void testFormatExchangeDataWithBaseCurrency() {
        Map<String, Object> response = Map.of(
                "usd", Map.of("eur", 0.9, "jpy", 110.5)
        );

        List<String> symbols = List.of("eur");

        Map<String, Object> result = WebClientUtil.formatExchangeData("USD", symbols, response);

        assertNotNull(result);
        assertEquals("usd", result.get("base"));
        Map<String, BigDecimal> rates = (Map<String, BigDecimal>) result.get("rates");
        assertEquals(BigDecimal.valueOf(0.9), rates.get("eur"));
        assertNull(rates.get("jpy"));
    }

    @Test
    void testFormatExchangeDataWithInvalidData() {
        Map<String, Object> response = Map.of(
                "rates", Map.of("GBP", 0.75)
        );

        List<String> symbols = List.of("usd");

        Map<String, Object> result = WebClientUtil.formatExchangeData("USD", symbols, response);

        assertNotNull(result);
        assertTrue(((Map<?, ?>) result.get("rates")).isEmpty());
    }

    @Test
    void testFormatExchangeDataWithEmptyResponse() {
        Map<String, Object> response = Map.of();

        List<String> symbols = List.of("usd");

        Map<String, Object> result = WebClientUtil.formatExchangeData("USD", symbols, response);

        assertNotNull(result);
        assertEquals("usd", result.get("base"));
        assertTrue(((Map<?, ?>) result.get("rates")).isEmpty());
    }

    @Test
    void testFormatExchangeDataWithNonNumericValues() {
        Map<String, Object> response = Map.of(
                "rates", Map.of("USD", "invalid", "EUR", "invalid")
        );

        List<String> symbols = List.of("usd", "eur");

        Map<String, Object> result = WebClientUtil.formatExchangeData("USD", symbols, response);

        assertNotNull(result);
        Map<String, BigDecimal> rates = (Map<String, BigDecimal>) result.get("rates");
        assertTrue(rates.isEmpty());
    }

    @Test
    void testCalculateAverageRatesWithMultipleAPIs() {
        List<Map<String, Object>> responses = List.of(
                Map.of("rates", Map.of("USD", BigDecimal.valueOf(1.2), "EUR", BigDecimal.valueOf(0.9))),
                Map.of("rates", Map.of("USD", BigDecimal.valueOf(1.4), "EUR", BigDecimal.valueOf(1.1)))
        );

        List<String> symbols = List.of("USD", "EUR");

        ExchangeRateServiceImpl exchangeRateService = new ExchangeRateServiceImpl();
        Map<String, Object> result = exchangeRateService.calculateAverageRates("NZD", responses, symbols);

        assertNotNull(result);
        assertEquals("NZD", result.get("base"));

        Map<String, BigDecimal> rates = (Map<String, BigDecimal>) result.get("rates");
        assertEquals(BigDecimal.valueOf(1.3).setScale(6, BigDecimal.ROUND_HALF_UP), rates.get("USD"));
        assertEquals(BigDecimal.valueOf(1).setScale(6, BigDecimal.ROUND_HALF_UP), rates.get("EUR"));
    }
}
