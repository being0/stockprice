package com.being0.stockprice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author <a href="mailto:raliakbari@gmail.com">Reza Aliakbari</a>
 * @version 1, 08/20/2020
 */
@RestController
@RequestMapping("/price")
public class PriceController {

    @GetMapping
    Mono<List<PriceResult>> getPrice(@RequestParam List<String> stocks) {
        return null;
    }

}
