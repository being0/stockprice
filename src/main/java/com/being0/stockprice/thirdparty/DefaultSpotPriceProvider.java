package com.being0.stockprice.thirdparty;

import com.being0.stockprice.PriceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

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

    private UnicastProcessor<String> bufferProcessor;
    private Flux<List<Price>> bufferedFlux;
    private FluxSink<String> bufferSink;
    @Value("${spot-api.buffer-size:5}")
    private Integer spotApiBufferSize;
    @Value("${spot-api.buffer-timeout:5}")
    private Integer spotApiBufferTimeout;

    @PostConstruct
    public void createBuffer() {
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
        if (stocks == null || stocks.isEmpty()) return Mono.empty();

        ArrayList<Price> prices = new ArrayList<>();

        Mutable<Disposable> ds = new Mutable<>();
        return Mono.<List<Price>>create(emitter -> {

            // Subscribe on the buffer
            ds.setValue(Flux.from(bufferedFlux)
                    //Filtering values which is match with our request
                    .map(ps -> ps.stream().filter(p -> stocks.contains(p.getStock())).collect(Collectors.toList()))
                    .subscribe(result -> {
                        log.info("Receive data request={}, result={}", stocks, result);
                        prices.addAll(result);
                        if (prices.size() == stocks.size()) {
                            emitter.success(prices);
                            log.info("Emitter has sent the data.");
                        }
                    }));
        })
                // We send our requested items to FluxSink at doOnSubscribe of our created Mono to make sure our
                // subscription won't miss issued items
                .doOnSubscribe(subscription -> stocks.forEach(i -> bufferSink.next(i))).subscribeOn(Schedulers.elastic())
                .subscribeOn(Schedulers.elastic())
                .doOnTerminate(() -> {
                    if (ds.getValue() != null && !ds.getValue().isDisposed()) ds.getValue().dispose();
                });
    }

    private Mono<List<Price>> callSpotPrice(List<String> stocks) {

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
