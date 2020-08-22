package com.being0.stockprice.thirdparty;

import reactor.core.publisher.Mono;

import java.util.List;

public interface DailyPriceProviderService {

    Mono<List<Price>> getDailyPrice(List<String> stocks);
}
