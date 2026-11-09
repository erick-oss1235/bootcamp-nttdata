package pe.dti.banking.accountservice;

import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.adapter.rxjava.RxJava3Adapter;

@RestController
@RequestMapping("/api/v1/accounts/transfers")
public class TransferController {
	private final AccountService service;

	public TransferController(AccountService service) { 
		this.service = service;
	}

	@PostMapping
	public Mono<TransferResult> transfer(@Valid @RequestBody TransferCommand cmd) { 
		if (cmd.fromAccountId().equals(cmd.toAccountId()))
			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"La cuenta de origen y la de destino deben ser diferentes."));
		return RxJava3Adapter.singleToMono(service.move(cmd.fromAccountId(), cmd.amount(), "WITHDRAWAL"))
				.then(RxJava3Adapter.singleToMono(service.move(cmd.toAccountId(), cmd.amount(), "DEPOSIT")))
				.map(to -> new TransferResult(UUID.randomUUID().toString(), "COMPLETED", to.getBalance()));
	}

	public record TransferCommand(@NotBlank String fromAccountId, @NotBlank String toAccountId,
			@NotNull @DecimalMin("0.01") BigDecimal amount) {
	}

	public record TransferResult(String id, String status, BigDecimal destinationBalance) {
	}
}
