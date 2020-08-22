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
public class DefaultDailyPriceProvider implements DailyPriceProviderService {

    @Resource
    private WebClient dailyApiClient;
    @Value("${daily-api.timeout}")
    private Duration dailyApiTimeOut;
    @Value("${daily-api.uri}")
    private String dailyApiUri;

    @Override
    public Mono<List<Price>> getDailyPrice(List<String> stocks) {
        return null;
    }

    private Mono<List<Price>> callDailyPrice(List<String> stocks) {

        return dailyApiClient.get()
                .uri(dailyApiUri, String.join(",", stocks))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Price>>() {
                })
                .timeout(dailyApiTimeOut)
                .onErrorReturn(e -> e instanceof WebClientResponseException || e instanceof TimeoutException,
                        Collections.emptyList()).doOnError(t -> log.error("call error for " + String.join(",", stocks), t));
    }


}
