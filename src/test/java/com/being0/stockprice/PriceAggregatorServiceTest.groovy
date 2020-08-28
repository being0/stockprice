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

    def "test 2 stocks call, it should wait for 5 seconds"() {
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
            duration > Duration.ofSeconds(5)
            duration < Duration.ofSeconds(6)
    }

    def "test 5 stocks call, it should be a quick call since buffer is flushed by 5 items"() {
        given:
            stubFiveItems()
        when:
            Mono<List<PriceResult>> mono1 = aggregationService.getPrice(['GOOG', 'AAPL'])
            Mono<List<PriceResult>> mono2 = aggregationService.getPrice(['TSLA', 'TWTR', 'RACE'])
        then:
            StepVerifier verifier1 = StepVerifier
                    .create(mono1)
                    .thenConsumeWhile({ result ->
                        assert result != null
                        assert result.size() == 2
                        assert result.get(0).stock == 'GOOG'
                        assert result.get(1).stock == 'AAPL'
                        return true
                    }).thenCancel()
                    .verifyLater()

            StepVerifier verifier2 = StepVerifier
                    .create(mono2)
                    .thenConsumeWhile({ result ->
                        assert result != null
                        assert result.size() == 3
                        assert result.get(0).stock == 'TSLA'
                        assert result.get(1).stock == 'TWTR'
                        assert result.get(2).stock == 'RACE'
                        return true
                    }).thenCancel()
                    .verifyLater()
        and:
            Duration duration1 = verifier1.verify();
            Duration duration2 = verifier2.verify();
            duration1 < Duration.ofSeconds(1)
            duration2 < Duration.ofSeconds(1)
    }

    def stubTwoItems() {
        createStub("/spot?stocks=GOOG%2CAAPL", "[{\"stock\": \"GOOG\", \"value\": 1580.42},{\"stock\": \"AAPL\", \"value\": 497.48}]")
        createStub("/daily?stocks=GOOG%2CAAPL", "[{\"stock\": \"GOOG\", \"value\": 1590.16},{\"stock\": \"AAPL\", \"value\": 510.56}]")
    }

    def stubFiveItems() {
        createStub("/spot?stocks=GOOG%2CAAPL%2CTSLA%2CTWTR%2CRACE", "[{\"stock\": \"GOOG\", \"value\": 1580.42},{\"stock\": \"AAPL\", \"value\": 497.48},{\"stock\": \"TSLA\", \"value\": 2238.75},{\"stock\": \"TWTR\", \"value\": 40.39},{\"stock\": \"RACE\", \"value\": 195.51}]")
        createStub("/daily?stocks=GOOG%2CAAPL", "[{\"stock\": \"GOOG\", \"value\": 1590.16},{\"stock\": \"AAPL\", \"value\": 510.56}]")
        createStub("/daily?stocks=TSLA%2CTWTR%2CRACE", "[{\"stock\": \"TSLA\", \"value\": 2228.65},{\"stock\": \"TWTR\", \"value\": 41.26},{\"stock\": \"RACE\", \"value\": 188.21}]")
    }

    def createStub(String path, String response) {
        stubFor(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)))
    }


}
