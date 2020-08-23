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
    private PriceAggregatorService aggregationService;

    @Shared
    private WireMockServer wireMockServer;

    def setupSpec() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(9090))
        wireMockServer.start()
        configureFor("localhost", 9090)
    }

    def "when get is performed then the response has status 200 and content is 'Hello world!'"() {
        given:
            stubTwoItems()
        when:
            Mono<List<PriceResult>> mono = aggregationService.getPrice(['GOOG', 'AAPL'])
        then:
            Duration duration = StepVerifier
                    .create(mono)
                    .thenConsumeWhile({ result ->
                        Assertions.assertThat(result).isNotNull()
                        Assertions.assertThat(result).hasSize(2)

                        Assertions.assertThat(result.get(0).stock).is('GOOG')
                        Assertions.assertThat(result.get(0).spot).is(1580.42)
                        Assertions.assertThat(result.get(0).daily).is(1590.16)

                        Assertions.assertThat(result.get(1).stock).is('AAPL')
                        Assertions.assertThat(result.get(1).spot).is(497.48)
                        Assertions.assertThat(result.get(1).daily).is(510.56)
                        return true
                    })
                    .verifyComplete()
        then:
            duration > Duration.ofSeconds(5)
    }

    def stubTwoItems() {
        createStub("/spot?stocks=GOOG,AAPL", "[{\"stock\": \"GOOG\", \"value\": 1580.42},{\"stock\": \"AAPL\", \"value\": 497.48}]")
        createStub("/daily?stocks=GOOD,AAPL", "[{\"stock\": \"GOOG\", \"value\": 1590.16},{\"stock\": \"AAPL\", \"value\": 510.56}]")
    }

    def createStub(String path, String response) {
        stubFor(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)))
    }


}
