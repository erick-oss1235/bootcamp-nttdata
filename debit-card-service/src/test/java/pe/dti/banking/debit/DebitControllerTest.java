package pe.dti.banking.debit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class DebitControllerTest {

	@Mock
	private DebitRepository repository;

	@Mock
	private KafkaTemplate<String, String> kafka;

	private WebTestClient client;

	@BeforeEach
	void setUp() {
		client = WebTestClient.bindToController(new DebitController(repository, kafka)).build();
	}

	@Test
	void createsMaskedDebitCard() { 
		when(repository.save(any(DebitCard.class))).thenAnswer(call -> {
			DebitCard card = call.getArgument(0);
			card.setId("d1");
			return Mono.just(card);
		});

		client.post().uri("/api/v1/debit-cards").contentType(MediaType.APPLICATION_JSON).bodyValue("""
				{"customerId":"c1","primaryAccountId":"a1","lastFour":"1234"}
				""").exchange().expectStatus().isCreated().expectBody().jsonPath("$.id").isEqualTo("d1")
				.jsonPath("$.maskedPan").isEqualTo("**** **** **** 1234");
	}

	@Test
	void rejectsPaymentOnInactiveCard() { 
		DebitCard card = new DebitCard();
		card.setId("d1");
		card.setActive(false);
		when(repository.findById("d1")).thenReturn(Mono.just(card));

		client.post().uri("/api/v1/debit-cards/d1/payment-requests").contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"amount\":100}").exchange().expectStatus().isEqualTo(409);

		verifyNoInteractions(kafka);
	}
}
