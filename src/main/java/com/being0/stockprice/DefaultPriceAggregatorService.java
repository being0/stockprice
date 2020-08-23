package com.being0.stockprice;

import com.being0.stockprice.thirdparty.DailyPriceProviderService;
import com.being0.stockprice.thirdparty.Price;
import com.being0.stockprice.thirdparty.SpotPriceProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:raliakbari@gmail.com">Reza Aliakbari</a>
 * @version 1, 8/23/20
 */
@Service
@Slf4j
public class DefaultPriceAggregatorService implements PriceAggregatorService {

    @Resource
    private SpotPriceProviderService spotPriceProviderService;
    @Resource
    private DailyPriceProviderService dailyPriceProviderService;

    @Override
    public Mono<List<PriceResult>> getPrice(List<String> stocks) {
        if (stocks == null || stocks.isEmpty()) return Mono.empty();

        return Mono.zip(
                spotPriceProviderService.getSpotPrice(stocks),
                dailyPriceProviderService.getDailyPrice(stocks)
        ).map(t -> {
            Map<String, Double> spotPriceMap = t.getT1().stream().collect(Collectors.toMap(Price::getStock, Price::getValue));
            Map<String, Double> dailyPriceMap = t.getT2().stream().collect(Collectors.toMap(Price::getStock, Price::getValue));

            return stocks.stream().map(s -> new PriceResult(s, spotPriceMap.get(s), dailyPriceMap.get(s))).collect(Collectors.toList());
        });
    }
}
