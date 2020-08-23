package com.being0.stockprice.thirdparty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:raliakbari@gmail.com">Reza Aliakbari</a>
 * @version 1, 08/22/2020
 */
@Service
@Slf4j
public class DefaultSpotPriceProvider implements SpotPriceProviderService {

    @Resource
    private WebClient spotApiClient;
    @Value("${spot-api.timeout}")
    private Duration spotApiTimeOut;
    @Value("${spot-api.uri}")
    private String spotApiUri;

    @Override
    public Mono<List<Price>>  getSpotPrice(List<String> stocks) {

        return callSpotPrice(stocks);
    }

    private Mono<List<Price>> callSpotPrice(List<String> stocks) {
        if (stocks == null || stocks.isEmpty()) return Mono.empty();

        return spotApiClient.get()
                .uri(spotApiUri, String.join(",", stocks))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Price>>() {
                })
                .timeout(spotApiTimeOut)
                .onErrorReturn(e -> e instanceof WebClientResponseException || e instanceof TimeoutException,
                        Collections.emptyList()).doOnError(t -> log.error("call error for " + String.join(",", stocks), t));
    }


}
