package com.being0.stockprice.thirdparty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author <a href="mailto:raliakbari@gmail.com">Reza Aliakbari</a>
 * @version 1, 08/20/2020
 */
@Configuration
public class ApiConfig {

    @Bean
    public WebClient spotApiClient(@Value("spot-api.url") String spotUrl){
        return WebClient.create(spotUrl);
    }

    @Bean
    public WebClient dailyApiClient(@Value("daily-api.url") String dailyUrl){
        return WebClient.create(dailyUrl);
    }
}
