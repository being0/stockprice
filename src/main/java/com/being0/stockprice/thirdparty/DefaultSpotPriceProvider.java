package com.being0.stockprice.thirdparty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;
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

    private final UnicastProcessor<String> bufferProcessor;
    private Flux<List<Price>> bufferedFlux;
    private final FluxSink<String> bufferSink;
    @Value("${spot-api.buffer-size:5}")
    private Integer spotApiBufferSize;
    @Value("${spot-api.buffer-timeout:5}")
    private Integer spotApiBufferTimeout;

    public DefaultSpotPriceProvider() {
        bufferProcessor = new UnicastProcessor<>(new ArrayDeque<>());
        // Here we use bufferTimeout to queue requested items and make a call when we have 5 items or when oldest
        // item has already wait 5 seconds we call share to make sure we can share the Flux with multiple subscribers
        bufferedFlux = bufferProcessor.bufferTimeout(spotApiBufferSize, Duration.ofSeconds(spotApiBufferTimeout))
                .flatMap(this::callSpotPrice).share().log();
        // We call subscribe() to make sure we always have at least one subscriber even if all subscribers got disposed
        // This is because if all subscribers on a Flux have canceled it will cancel the source
        bufferedFlux.subscribe();
        // To run processor subscribe and publish on different thread as it is allowed
        bufferProcessor.subscribeOn(Schedulers.elastic());
        // We keep each FluxSink that is a safely gates multi-threaded producer
        bufferSink = bufferProcessor.sink();
    }

    @Override
    public Mono<List<Price>> getSpotPrice(List<String> stocks) {

//        Mono.<List<Price>>create(emitter->{
//            Flux.from(bufferedFlux)
//                    //Filtering values which is match with our request
//                    .filter(p->p.get)
//                    .map()
//        }

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
