package com.being0.stockprice

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.assertj.core.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration

import static com.github.tomakehurst.wiremock.client.WireMock.*

@SpringBootTest
class PriceAggregatorServiceTest extends Specification {

    @Autowired
    private PriceAggregatorService aggregationService

    @Shared
    private WireMockServer wireMockServer

    def setupSpec() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(9090))
        wireMockServer.start()
        configureFor("localhost", 9090)
    }

    def "test 2 stocks call, it should wait for 5 seconds'"() {
        given:
        stubTwoItems()
        when:
        Mono<List<PriceResult>> mono = aggregationService.getPrice(['GOOG', 'AAPL'])
        then:
        Duration duration = StepVerifier
                .create(mono)
                .thenConsumeWhile({ result ->
                    assert result != null
                    assert result.size() == 2

                    assert result.get(0).stock == 'GOOG'
                    assert result.get(0).spot == 1580.42
                    assert result.get(0).daily == 1590.16

                    assert result.get(1).stock == 'AAPL'
                    assert result.get(1).spot == 497.48
                    assert result.get(1).daily == 510.56
                    return true
                })
                .verifyComplete()
        and:
        Duration.ofSeconds(6) > duration > Duration.ofSeconds(5)
    }

    def stubTwoItems() {
        createStub("/spot?stocks=GOOG%2CAAPL", "[{\"stock\": \"GOOG\", \"value\": 1580.42},{\"stock\": \"AAPL\", \"value\": 497.48}]")
        createStub("/daily?stocks=GOOG%2CAAPL", "[{\"stock\": \"GOOG\", \"value\": 1590.16},{\"stock\": \"AAPL\", \"value\": 510.56}]")
    }

    def createStub(String path, String response) {
        stubFor(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)))
    }


}
