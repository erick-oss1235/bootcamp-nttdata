package pe.dti.banking.accountservice;

import io.reactivex.rxjava3.core.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.*;
import reactor.adapter.rxjava.RxJava3Adapter;
import java.math.BigDecimal;
import pe.dti.banking.domain.BankingRules;
import java.time.*;
import java.util.*;

@Service
public class AccountService {
	private final AccountRepository repo;
	private final WebClient customers;
	private final WebClient credits;

	public AccountService(AccountRepository repo, WebClient.Builder builder,
			@Value("${banking.customer-service-url}") String url,
			@Value("${banking.credit-service-url:http://localhost:8083}") String creditUrl) {
		this.repo = repo;
		this.customers = builder.clone().baseUrl(url).build();
		this.credits = builder.clone().baseUrl(creditUrl).build();
	}

	private Mono<CustomerView> customerData(String id) { 
		return customers.get().uri("/api/v1/customers/{id}", id).retrieve()
				.onStatus(HttpStatusCode::isError, r -> Mono.error(
						new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se encontró el cliente solicitado.")))
				.bodyToMono(CustomerView.class);
	}

	private Flux<CreditView> customerCredits(String id) { 
		return credits.get().uri(uri -> uri.path("/api/v1/credits").queryParam("customerId", id).build()).retrieve()
				.bodyToFlux(CreditView.class);
	}

	private record CustomerView(String type, String profile) {
	}

	private record CreditView(String type, boolean pastDue) {
	}

	public Single<BankAccount> create(BankAccount a) { 
		a.setId(null);
		a.setVersion(null);
		BigDecimal opening = a.getOpeningDeposit() == null ? BigDecimal.ZERO : a.getOpeningDeposit();
		a.setBalance(opening);
		a.setMovements(new ArrayList<>());
		Mono<BankAccount> flow = customerData(a.getCustomerId()).flatMap(customer -> {
			String type = customer.type();
			if (type.equals("BUSINESS") && a.getType() != BankAccount.AccountType.CHECKING)
				return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Los clientes empresariales solo pueden tener cuentas corrientes."));
			if (type.equals("PERSONAL") && (a.getType() == BankAccount.AccountType.SAVINGS
					|| a.getType() == BankAccount.AccountType.CHECKING))
				return repo.findByCustomerId(a.getCustomerId()).any(x -> x.getType() == a.getType())
						.flatMap(exists -> exists
								? Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
										"El cliente personal ya tiene una cuenta de este tipo."))
								: validateAndSave(a, customer));
			return validateAndSave(a, customer);
		});
		return RxJava3Adapter.monoToSingle(flow);
	}

	private Mono<BankAccount> validateAndSave(BankAccount a, CustomerView customer) { 
		String customerType = customer.type();
		String profile = customer.profile() == null ? "STANDARD" : customer.profile();
		if (a.getOpeningDeposit() != null && a.getOpeningDeposit().signum() < 0)
			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"El depósito de apertura no puede ser negativo."));
		if (a.getFreeMonthlyTransactions() != null && a.getFreeMonthlyTransactions() < 0)
			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"El límite mensual de operaciones gratuitas no es válido."));
		if (a.getTransactionFee() != null && a.getTransactionFee().signum() < 0)
			return Mono.error(
					new ResponseStatusException(HttpStatus.BAD_REQUEST, "La comisión por operación no es válida."));
		if (profile.equals("VIP") && a.getType() == BankAccount.AccountType.SAVINGS) {
			if (a.getMinimumDailyAverage() == null || a.getMinimumDailyAverage().signum() <= 0)
				return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"La cuenta de ahorro VIP requiere un promedio diario mínimo."));
			return customerCredits(a.getCustomerId()).any(c -> c.type().equals("PERSONAL_CARD"))
					.flatMap(hasCard -> hasCard ? saveWithRules(a, customerType, profile)
							: Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
									"El cliente VIP necesita una tarjeta de crédito para abrir esta cuenta.")));
		}
		if (profile.equals("PYME") && a.getType() == BankAccount.AccountType.CHECKING) {
			return customerCredits(a.getCustomerId()).any(c -> c.type().equals("BUSINESS_CARD"))
					.flatMap(hasCard -> hasCard ? saveWithRules(a, customerType, profile)
							: Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
									"La cuenta corriente PYME requiere una tarjeta de crédito empresarial.")));
		}
		return saveWithRules(a, customerType, profile);
	}

	private Mono<BankAccount> saveWithRules(BankAccount a, String customerType, String profile) {
		if (a.getType() == BankAccount.AccountType.SAVINGS) {
			a.setMaintenanceFee(BigDecimal.ZERO);
			if (a.getMonthlyMovementLimit() == null || a.getMonthlyMovementLimit() < 1)
				return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"La cuenta de ahorro requiere un límite mensual de movimientos mayor que cero."));
		}
		if (a.getType() == BankAccount.AccountType.FIXED_TERM) {
			a.setMaintenanceFee(BigDecimal.ZERO);
			if (a.getFixedDay() == null || a.getFixedDay() < 1 || a.getFixedDay() > 28)
				return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"La cuenta a plazo fijo requiere un día de operación entre el 1 y el 28."));
		}
		if (a.getType() == BankAccount.AccountType.CHECKING) {
			if (profile.equals("PYME"))
				a.setMaintenanceFee(BigDecimal.ZERO);
			else if (a.getMaintenanceFee() == null || a.getMaintenanceFee().signum() <= 0)
				return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"La cuenta corriente requiere una comisión de mantenimiento mayor que cero."));
		}
		if (customerType.equals("BUSINESS") && (a.getHolderIds() == null || a.getHolderIds().isEmpty()))
			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"La cuenta empresarial requiere al menos un titular."));
		return repo.save(a);
	}

	public Single<BankAccount> find(String id) { 
		return RxJava3Adapter.monoToSingle(repo.findById(id).switchIfEmpty(Mono.error(
				new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la cuenta bancaria solicitada."))));
	}

	public Flowable<BankAccount> all() { 
		return RxJava3Adapter.fluxToFlowable(repo.findAll());
	}

	public Flowable<BankAccount> byCustomer(String id) { 
		return RxJava3Adapter.fluxToFlowable(repo.findByCustomerId(id));
	}

	public Single<BankAccount> update(String id, BankAccount input) {
		return find(id).flatMap(old -> {
			old.setAuthorizedSignerIds(
					input.getAuthorizedSignerIds() == null ? new ArrayList<>() : input.getAuthorizedSignerIds());
			old.setHolderIds(input.getHolderIds() == null ? new ArrayList<>() : input.getHolderIds());
			return RxJava3Adapter.monoToSingle(repo.save(old));
		});
	}

	public Completable delete(String id) { 
		return find(id).flatMapCompletable(a -> {
			if (a.getBalance().signum() != 0)
				return Completable.error(new ResponseStatusException(HttpStatus.CONFLICT,
						"Solo puedes cerrar una cuenta que tenga saldo cero."));
			return RxJava3Adapter.monoToCompletable(repo.delete(a));
		});
	}

	public Single<BankAccount> move(String id, BigDecimal amount, String kind) {
		if (amount == null || amount.signum() <= 0)
			return Single
					.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "El importe debe ser mayor que cero."));
		return find(id).flatMap(a -> {
			LocalDate today = LocalDate.now(ZoneOffset.UTC);
			if (a.getType() == BankAccount.AccountType.FIXED_TERM && today.getDayOfMonth() != a.getFixedDay())
				return Single.error(new ResponseStatusException(HttpStatus.CONFLICT,
						"La cuenta a plazo fijo solo permite operaciones en el día configurado."));
			long count = a.getMovements().stream()
					.filter(x -> !"FEE".equals(x.getType())
							&& YearMonth.from(x.getTimestamp().atZone(ZoneOffset.UTC)).equals(YearMonth.from(today)))
					.count();
			if (a.getType() == BankAccount.AccountType.FIXED_TERM && count >= 1)
				return Single.error(new ResponseStatusException(HttpStatus.CONFLICT,
						"Ya se realizó el movimiento permitido de este mes en la cuenta a plazo fijo."));
			if (a.getType() == BankAccount.AccountType.SAVINGS && count >= a.getMonthlyMovementLimit())
				return Single.error(new ResponseStatusException(HttpStatus.CONFLICT,
						"Se alcanzó el límite mensual de movimientos de la cuenta de ahorro."));
			BigDecimal fee = BankingRules.transactionFee(count,
					a.getFreeMonthlyTransactions() == null ? Integer.MAX_VALUE : a.getFreeMonthlyTransactions(),
					a.getTransactionFee() == null ? BigDecimal.ZERO : a.getTransactionFee());
			BigDecimal next = (kind.equals("DEPOSIT") ? a.getBalance().add(amount) : a.getBalance().subtract(amount))
					.subtract(fee);
			if (next.signum() < 0)
				return Single.error(new ResponseStatusException(HttpStatus.CONFLICT,
						"Saldo insuficiente para realizar la operación."));
			a.setBalance(next);
			a.getMovements().add(
					new BankAccount.AccountMovement(UUID.randomUUID().toString(), kind, amount, Instant.now(), next));
			if (fee.signum() > 0)
				a.getMovements().add(
						new BankAccount.AccountMovement(UUID.randomUUID().toString(), "FEE", fee, Instant.now(), next));
			return RxJava3Adapter.monoToSingle(repo.save(a));
		});
	}
}
