package pe.dti.banking.creditservice;

import java.time.Instant;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.adapter.rxjava.RxJava3Adapter;


@RestController
@RequestMapping("/api/v1/credits/reports")
public class CreditReportController {
	private final CreditService credits;

	public CreditReportController(CreditService credits) { 
		this.credits = credits;
	}

	@GetMapping("/movements")
	public Flux<CreditMovementReport> report(@RequestParam Instant from, @RequestParam Instant to) { 
		return RxJava3Adapter.flowableToFlux(credits.all())
				.flatMapIterable(c -> c.getMovements().stream()
						.filter(m -> !m.getTimestamp().isBefore(from) && !m.getTimestamp().isAfter(to))
						.map(m -> new CreditMovementReport(c.getId(), c.getType().name(), m.getType(), m.getAmount(),
								m.getTimestamp()))
						.toList());
	}

	@GetMapping("/{id}/latest")
	public Flux<CreditProduct.CreditMovement> latest(@PathVariable String id) { 
		return RxJava3Adapter.singleToMono(credits.find(id)).flatMapMany(c -> Flux.fromStream(
				c.getMovements().stream().sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp())).limit(10)));
	}

	public record CreditMovementReport(String creditId, String creditType, String kind, java.math.BigDecimal amount,
			Instant timestamp) {
	}
}
