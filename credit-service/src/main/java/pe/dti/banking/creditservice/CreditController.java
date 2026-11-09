package pe.dti.banking.creditservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;
import reactor.adapter.rxjava.RxJava3Adapter;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/credits")
public class CreditController {
	private final CreditService s;

	public CreditController(CreditService s) { 
		this.s = s;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<CreditProduct> create(@Valid @RequestBody CreditProduct c) { 
		return RxJava3Adapter.singleToMono(s.create(c));
	}

	@GetMapping
	public Flux<CreditProduct> all(@RequestParam(required = false) String customerId) { 
		return RxJava3Adapter.flowableToFlux(customerId == null ? s.all() : s.byCustomer(customerId));
	}

	@GetMapping("/{id}")
	public Mono<CreditProduct> find(@PathVariable String id) { 
		return RxJava3Adapter.singleToMono(s.find(id));
	}

	@PutMapping("/{id}")
	public Mono<CreditProduct> update(@PathVariable String id, @RequestBody CreditProduct c) { 
		return RxJava3Adapter.singleToMono(s.update(id, c));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> delete(@PathVariable String id) { 
		return RxJava3Adapter.completableToMono(s.delete(id));
	}

	@PostMapping("/{id}/payments")
	public Mono<CreditProduct> payment(@PathVariable String id, @Valid @RequestBody MoneyRequest in) { 
		return RxJava3Adapter.singleToMono(s.move(id, in.amount(), "PAYMENT"));
	}

	@PostMapping("/{id}/charges")
	public Mono<CreditProduct> charge(@PathVariable String id, @Valid @RequestBody MoneyRequest in) { //cargo
		return RxJava3Adapter.singleToMono(s.move(id, in.amount(), "CHARGE"));
	}

	@GetMapping("/{id}/movements")
	public Flux<CreditProduct.CreditMovement> movements(@PathVariable String id) {
		return find(id).flatMapMany(c -> Flux.fromIterable(c.getMovements()));
	}

	public record MoneyRequest(@NotNull @DecimalMin("0.01") BigDecimal amount) {
	}
}
