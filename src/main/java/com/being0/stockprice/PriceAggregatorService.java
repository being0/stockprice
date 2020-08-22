package com.being0.stockprice;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author <a href="mailto:raliakbari@gmail.com">Reza Aliakbari</a>
 * @version 1, 08/20/2020
 */
public interface PriceAggregatorService {

    Mono<List<PriceResult>> getPrice(List<String> stocks);
}
