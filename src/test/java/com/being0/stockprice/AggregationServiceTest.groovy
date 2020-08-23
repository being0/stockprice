package com.being0.stockprice

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.tnt.assignment.model.AggregatedResult
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
class AggregationServiceTest extends Specification {

    @Autowired
    private AggregationService aggregationService;

    @Shared
    private WireMockServer wireMockServer;

    def setupSpec() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(9090));
        wireMockServer.start();
        com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", 9090);
    }

    def "when get is performed then the response has status 200 and content is 'Hello world!'"() {
        given:
            stubTwoItems();
        when:
        Mono<AggregatedResult> mono = aggregationService
                    .sendThenAggregate(Optional.of(Arrays.asList("NL", "CN")), Optional.of(Arrays.asList("1", "2")),
                            Optional.of(Arrays.asList("1", "2")));
        then:
        Duration duration = StepVerifier
                    .create(mono)
                    .thenConsumeWhile({ result ->
                        Assertions.assertThat(result).isNotNull();
                        Assertions.assertThat(result.getShipments()).hasSize(2);
                        Assertions.assertThat(result.getPricing()).hasSize(2);
                        Assertions.assertThat(result.getTrack()).hasSize(2);
                        return true;
                    })
                    .verifyComplete()
        then:
            duration > Duration.ofSeconds(5)
    }

    def stubTwoItems() {
        createStub("/pricing?q=CN%2CNL", "{\"CN\": 2, \"NL\": 1}");
        createStub("/track?q=1%2C2", "{\"1\": \"NEW\", \"2\": \"COLLECTING\"}");
        createStub("/shipments?q=1%2C2", "{\"1\": [\"box\", \"box\", \"pallet\"], \"2\": [\"envelope\"]}");
    }

    def createStub(String path, String response) {
        com.github.tomakehurst.wiremock.client.WireMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo(path))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }


}
