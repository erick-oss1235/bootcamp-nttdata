package pe.dti.banking.debit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.*;

@RestController
@RequestMapping("/api/v1/debit-cards")
public class DebitController {
	private final DebitRepository repo;
	private final KafkaTemplate<String, String> kafka;

	public DebitController(DebitRepository repo, KafkaTemplate<String, String> kafka) { 
		this.repo = repo;
		this.kafka = kafka;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<DebitCard> create(@Valid @RequestBody NewCard req) { 
		DebitCard card = new DebitCard();
		card.setCustomerId(req.customerId());
		card.setPrimaryAccountId(req.primaryAccountId());
		card.setMaskedPan("**** **** **** " + req.lastFour());
		return repo.save(card);
	}

	@GetMapping
	public Flux<DebitCard> all(@RequestParam(required = false) String customerId) { 
		return customerId == null ? repo.findAll() : repo.findByCustomerId(customerId);
	}

	@GetMapping("/{id}")
	public Mono<DebitCard> get(@PathVariable String id) { 
		return repo.findById(id).switchIfEmpty(Mono.error(
				new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la tarjeta de débito solicitada.")));
	}

	@PutMapping("/{id}")
	public Mono<DebitCard> update(@PathVariable String id, @RequestBody UpdateCard req) { 
		return get(id).flatMap(c -> {
			c.setActive(req.active());
			return repo.save(c);
		});
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> delete(@PathVariable String id) { 
		return get(id).flatMap(repo::delete);
	}

	@PostMapping("/{id}/payment-requests")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Mono<PaymentReceipt> pay(@PathVariable String id, @Valid @RequestBody Payment req) { 
		return get(id).flatMap(c -> {
			if (!c.isActive())
				return Mono
						.error(new ResponseStatusException(HttpStatus.CONFLICT, "La tarjeta de débito está inactiva."));
			String eventId = java.util.UUID.randomUUID().toString();
			return Mono
					.fromFuture(kafka.send("banking.debit.payment.requests", eventId,
							eventId + "|" + c.getPrimaryAccountId() + "|" + req.amount()))
					.map(send -> new PaymentReceipt(eventId, "PENDING"));
		});
	}

	public record NewCard(@NotBlank String customerId, @NotBlank String primaryAccountId,
			@Pattern(regexp = "[0-9]{4}") String lastFour) {
	}

	public record UpdateCard(boolean active) {
	}

	public record Payment(@NotNull @DecimalMin("0.01") BigDecimal amount) {
	}

	public record PaymentReceipt(String id, String status) {
	}
}
