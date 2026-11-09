package pe.dti.banking.accountservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;
import reactor.adapter.rxjava.RxJava3Adapter;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
	private final AccountService s;

	public AccountController(AccountService s) {
		this.s = s;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<BankAccount> create(@Valid @RequestBody BankAccount a) {
		return RxJava3Adapter.singleToMono(s.create(a));
	}

	@GetMapping
	public Flux<BankAccount> all(@RequestParam(required = false) String customerId) {
		return RxJava3Adapter.flowableToFlux(customerId == null ? s.all() : s.byCustomer(customerId));
	}

	@GetMapping("/{id}")
	public Mono<BankAccount> find(@PathVariable String id) { 
		return RxJava3Adapter.singleToMono(s.find(id));
	}

	@PutMapping("/{id}")
	public Mono<BankAccount> update(@PathVariable String id, @RequestBody BankAccount a) { 
		return RxJava3Adapter.singleToMono(s.update(id, a));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> delete(@PathVariable String id) {
		return RxJava3Adapter.completableToMono(s.delete(id));
	}

	@PostMapping("/{id}/deposits")
	public Mono<BankAccount> deposit(@PathVariable String id, @Valid @RequestBody MoneyRequest input) {
		return RxJava3Adapter.singleToMono(s.move(id, input.amount(), "DEPOSIT"));
	}

	@PostMapping("/{id}/withdrawals")
	public Mono<BankAccount> withdraw(@PathVariable String id, @Valid @RequestBody MoneyRequest input) {
		return RxJava3Adapter.singleToMono(s.move(id, input.amount(), "WITHDRAWAL"));
	}

	@GetMapping("/{id}/movements")
	public Flux<BankAccount.AccountMovement> movements(@PathVariable String id) {
		return find(id).flatMapMany(a -> Flux.fromIterable(a.getMovements()));
	}

	public record MoneyRequest(@NotNull @DecimalMin("0.01") BigDecimal amount) {
	}
}
