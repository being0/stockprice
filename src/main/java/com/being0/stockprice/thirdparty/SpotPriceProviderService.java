package com.being0.stockprice.thirdparty;

import reactor.core.publisher.Mono;

import java.util.List;

public interface SpotPriceProviderService {

    Mono<List<Price>> getSpotPrice(List<String> stocks);
}
