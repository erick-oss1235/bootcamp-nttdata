package pe.dti.banking.wallet;

import jakarta.validation.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.*;


@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {
	private final WalletRepository repo;
	private final KafkaTemplate<String, String> kafka;

	public WalletController(WalletRepository repo, KafkaTemplate<String, String> kafka) { 
		this.repo = repo;
		this.kafka = kafka;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<Wallet> create(@Valid @RequestBody NewWallet req) { // Registrar monedero
		return repo.findByPhone(req.phone())
				.flatMap(w -> Mono.<Wallet>error(
						new ResponseStatusException(HttpStatus.CONFLICT, "El número de celular ya está registrado.")))
				.switchIfEmpty(Mono.defer(() -> {
					Wallet w = new Wallet();
					w.setPhone(req.phone());
					w.setDocumentType(req.documentType());
					w.setDocumentNumber(req.documentNumber());
					w.setImei(req.imei());
					w.setEmail(req.email());
					return repo.save(w);
				}));
	}

	@GetMapping("/{phone}")
	public Mono<Wallet> get(@PathVariable String phone) { // Consultar monedero por celular
		return existing(phone);
	}

	@GetMapping
	public Flux<Wallet> all() { 
		return repo.findAll();
	}

	@PutMapping("/{phone}")
	public Mono<Wallet> update(@PathVariable String phone, @Valid @RequestBody NewWallet req) { // Actualizar monedero
		return existing(phone).flatMap(w -> {
			w.setEmail(req.email());
			w.setImei(req.imei());
			w.setDocumentType(req.documentType());
			w.setDocumentNumber(req.documentNumber());
			return repo.save(w);
		});
	}

	@DeleteMapping("/{phone}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> delete(@PathVariable String phone) { // Eliminar monedero sin saldo
		return existing(phone).flatMap(w -> w.getBalance().signum() == 0 ? repo.delete(w)
				: Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
						"No se puede eliminar un monedero que tiene saldo disponible.")));
	}

	@PutMapping("/{phone}/debit-card/{cardId}")
	public Mono<Wallet> link(@PathVariable String phone, @PathVariable String cardId) { // Vincular tarjeta de débito
		return existing(phone).flatMap(w -> {
			w.setLinkedDebitCardId(cardId);
			return repo.save(w);
		});
	}

	@PostMapping("/{phone}/top-ups")
	public Mono<Wallet> topup(@PathVariable String phone, @Valid @RequestBody Money req) { // Recargar monedero
		return existing(phone).flatMap(w -> {
			w.setBalance(w.getBalance().add(req.amount()));
			return repo.save(w);
		});
	}

	@PostMapping("/{phone}/transfers")
	public Mono<TransferReceipt> transfer(@PathVariable String phone, @Valid @RequestBody Transfer req) { // Transferir
																											// entre
																											// monederos
		if (phone.equals(req.toPhone()))
			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"El número de celular de origen y destino debe ser diferente."));
		return Mono.zip(existing(phone), existing(req.toPhone())).flatMap(pair -> {
			Wallet sender = pair.getT1(), receiver = pair.getT2();
			if (sender.getBalance().compareTo(req.amount()) < 0)
				return Mono.error(
						new ResponseStatusException(HttpStatus.CONFLICT, "El monedero no tiene saldo suficiente."));
			sender.setBalance(sender.getBalance().subtract(req.amount()));
			receiver.setBalance(receiver.getBalance().add(req.amount()));
			String eventId = UUID.randomUUID().toString();
			return repo.save(sender).then(repo.save(receiver)).map(saved -> {
				kafka.send("banking.wallet.transfers", eventId,
						eventId + "|" + phone + "|" + req.toPhone() + "|" + req.amount());
				return new TransferReceipt(eventId, "ACCEPTED", saved.getBalance());
			});
		});
	}

	private Mono<Wallet> existing(String phone) { // Obtener monedero existente
		return repo.findByPhone(phone).switchIfEmpty(Mono
				.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el monedero solicitado.")));
	}

	public record NewWallet(@NotBlank String documentType, @NotBlank String documentNumber, @NotBlank String phone,
			@NotBlank String imei, @Email @NotBlank String email) {
	}

	public record Money(@NotNull @DecimalMin("0.01") BigDecimal amount) {
	}

	public record Transfer(@NotBlank String toPhone, @NotNull @DecimalMin("0.01") BigDecimal amount) {
	}

	public record TransferReceipt(String eventId, String status, BigDecimal recipientBalance) {
	}
}
