package pe.dti.banking.accountservice;

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
        "spring.cloud.discovery.enabled=false",
        "spring.kafka.listener.auto-startup=false"
})
@Testcontainers(disabledWithoutDocker = true)
class AccountApiIT {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.14");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) { 
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @LocalServerPort
    private int port;

    @Test
    void depositsAndWithdrawsFromStoredAccount() { 
        WebTestClient client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        String accountId = client.post().uri("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"customerId":"personal-customer","type":"SAVINGS",
                         "monthlyMovementLimit":10,"freeMonthlyTransactions":5}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BankAccount.class)
                .returnResult()
                .getResponseBody()
                .getId();

        client.post().uri("/api/v1/accounts/{id}/deposits", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":500}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.balance").isEqualTo(500);

        client.post().uri("/api/v1/accounts/{id}/withdrawals", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":150}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.balance").isEqualTo(350);

        client.get().uri("/api/v1/accounts/{id}/movements", accountId)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(2);
    }

    @TestConfiguration
    static class CustomerStub {

        @Bean
        @Primary
        WebClient.Builder webClientBuilder() { 
            return WebClient.builder().exchangeFunction(request -> Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body("{\"type\":\"PERSONAL\",\"profile\":\"STANDARD\"}")
                            .build()));
        }
    }
}
