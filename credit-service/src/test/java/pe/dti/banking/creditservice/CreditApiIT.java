package pe.dti.banking.creditservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class CreditApiIT {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.14");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) { 
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @LocalServerPort
    private int port;

    @Test
    void persistsCardChargeAndPayment() { 
        WebTestClient client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        String creditId = client.post().uri("/api/v1/credits")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"customerId":"personal-customer","type":"PERSONAL_CARD","creditLimit":5000}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreditProduct.class)
                .returnResult()
                .getResponseBody()
                .getId();

        client.post().uri("/api/v1/credits/{id}/charges", creditId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":800}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.outstanding").isEqualTo(800);

        client.post().uri("/api/v1/credits/{id}/payments", creditId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":300}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.outstanding").isEqualTo(500);
    }

    @TestConfiguration
    static class CustomerStub {

        @Bean
        @Primary
        WebClient.Builder webClientBuilder() { 
            return WebClient.builder().exchangeFunction(request -> Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body("{\"type\":\"PERSONAL\"}")
                            .build()));
        }
    }
}
