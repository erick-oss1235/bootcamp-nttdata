package pe.dti.banking.accountservice;

import java.time.Instant;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.adapter.rxjava.RxJava3Adapter;

@RestController
@RequestMapping("/api/v1/accounts/reports")
public class AccountReportController {
	private final AccountService accounts;

	public AccountReportController(AccountService accounts) {
		this.accounts = accounts;
	}

	@GetMapping("/movements")
	public Flux<MovementReport> report(@RequestParam Instant from, @RequestParam Instant to) { 
		return RxJava3Adapter.flowableToFlux(accounts.all())
				.flatMapIterable(a -> a.getMovements().stream()
						.filter(m -> !m.getTimestamp().isBefore(from) && !m.getTimestamp().isAfter(to))
						.map(m -> new MovementReport(a.getId(), a.getType().name(), m.getType(), m.getAmount(),
								m.getTimestamp()))
						.toList());
	}

	public record MovementReport(String accountId, String accountType, String kind, java.math.BigDecimal amount,
			Instant timestamp) {
	}
}
