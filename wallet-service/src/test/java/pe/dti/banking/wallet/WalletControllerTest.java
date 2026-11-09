package pe.dti.banking.wallet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
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
class WalletControllerTest {

	@Mock
	private WalletRepository repository;

	@Mock
	private KafkaTemplate<String, String> kafka;

	private WebTestClient client;

	@BeforeEach
	void setUp() { 
		client = WebTestClient.bindToController(new WalletController(repository, kafka)).build();
	}

	@Test
	void registersWalletWithoutBankAccount() { 
		when(repository.findByPhone("999888777")).thenReturn(Mono.empty());
		when(repository.save(any(Wallet.class))).thenAnswer(call -> {
			Wallet wallet = call.getArgument(0);
			wallet.setId("wallet1");
			return Mono.just(wallet);
		});

		client.post().uri("/api/v1/wallets").contentType(MediaType.APPLICATION_JSON).bodyValue("""
				{"documentType":"DNI","documentNumber":"12345678",
				 "phone":"999888777","imei":"123456789000001",
				 "email":"test@example.com"}
				""").exchange().expectStatus().isCreated().expectBody().jsonPath("$.id").isEqualTo("wallet1");
	}

	@Test
	void preventsWalletTransferWithInsufficientFunds() { 
		Wallet sender = wallet("999888777", new BigDecimal("10"));
		Wallet receiver = wallet("999777666", BigDecimal.ZERO);
		when(repository.findByPhone("999888777")).thenReturn(Mono.just(sender));
		when(repository.findByPhone("999777666")).thenReturn(Mono.just(receiver));

		client.post().uri("/api/v1/wallets/999888777/transfers").contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"toPhone\":\"999777666\",\"amount\":100}").exchange().expectStatus().isEqualTo(409);

		verify(repository, never()).save(any());
	}

	private Wallet wallet(String phone, BigDecimal balance) { 
		Wallet wallet = new Wallet();
		wallet.setPhone(phone);
		wallet.setBalance(balance);
		return wallet;
	}
}
