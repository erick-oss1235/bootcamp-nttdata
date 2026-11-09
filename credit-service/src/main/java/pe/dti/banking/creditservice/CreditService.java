package pe.dti.banking.creditservice;

import io.reactivex.rxjava3.core.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.*;
import reactor.adapter.rxjava.RxJava3Adapter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;


@Service
public class CreditService {
	private final CreditRepository repo;
	private final WebClient customers;

	public CreditService(CreditRepository repo, WebClient.Builder builder,
			@Value("${banking.customer-service-url}") String url) { 
		this.repo = repo;
		this.customers = builder.baseUrl(url).build();
	}

	private record CustomerView(String type) {
	}

	public Single<CreditProduct> create(CreditProduct c) {
		c.setId(null);
		c.setVersion(null);
		c.setOutstanding(c.getType() == CreditProduct.CreditType.PERSONAL_LOAN
				|| c.getType() == CreditProduct.CreditType.BUSINESS_LOAN ? c.getCreditLimit() : BigDecimal.ZERO);
		c.setMovements(new ArrayList<>());
		Mono<CreditProduct> result = customers.get().uri("/api/v1/customers/{id}", c.getCustomerId()).retrieve()
				.onStatus(HttpStatusCode::isError,
						r -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
								"No se encontró el cliente solicitado.")))
				.bodyToMono(CustomerView.class).flatMap(customer -> {
					boolean personal = customer.type().equals("PERSONAL");
					if (personal
							&& (c.getType() == CreditProduct.CreditType.BUSINESS_LOAN
									|| c.getType() == CreditProduct.CreditType.BUSINESS_CARD)
							|| !personal && (c.getType() == CreditProduct.CreditType.PERSONAL_LOAN
									|| c.getType() == CreditProduct.CreditType.PERSONAL_CARD))
						return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
								"El tipo de crédito no es compatible con el cliente."));
					return repo.findByCustomerId(c.getCustomerId()).collectList().flatMap(existing -> {
						if (existing.stream().anyMatch(x -> x.isPastDue() && x.getOutstanding().signum() > 0))
							return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
									"El cliente tiene una deuda de crédito vencida."));
						if (c.getType() == CreditProduct.CreditType.PERSONAL_LOAN && existing.stream()
								.anyMatch(x -> x.getType() == CreditProduct.CreditType.PERSONAL_LOAN))
							return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
									"Solo se permite un préstamo personal por cliente."));
						return repo.save(c);
					});
				});
		return RxJava3Adapter.monoToSingle(result);
	}

	public Single<CreditProduct> find(String id) { 
		return RxJava3Adapter.monoToSingle(
				repo.findById(id).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
						"No se encontró el producto de crédito solicitado."))));
	}

	public Flowable<CreditProduct> all() { 
		return RxJava3Adapter.fluxToFlowable(repo.findAll());
	}

	public Flowable<CreditProduct> byCustomer(String id) { 
		return RxJava3Adapter.fluxToFlowable(repo.findByCustomerId(id));
	}

	public Single<CreditProduct> update(String id, CreditProduct input) { // actualizar límite de crédito
		return find(id).flatMap(old -> {
			if (input.getCreditLimit() == null || input.getCreditLimit().compareTo(old.getOutstanding()) < 0)
				return Single.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"El límite de crédito no puede ser menor que la deuda pendiente."));
			old.setCreditLimit(input.getCreditLimit());
			return RxJava3Adapter.monoToSingle(repo.save(old));
		});
	}

	public Completable delete(String id) { // Eliminar producto de crédito sin deuda
		return find(id).flatMapCompletable(c -> c.getOutstanding().signum() != 0
				? Completable.error(new ResponseStatusException(HttpStatus.CONFLICT,
						"La deuda pendiente debe ser cero para eliminar el producto."))
				: RxJava3Adapter.monoToCompletable(repo.delete(c)));
	}

	public Single<CreditProduct> move(String id, BigDecimal amount, String kind) { 
		if (amount == null || amount.signum() <= 0)
			return Single
					.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "El importe debe ser mayor que cero."));
		return find(id).flatMap(c -> {
			if (kind.equals("CHARGE") && !(c.getType() == CreditProduct.CreditType.PERSONAL_CARD
					|| c.getType() == CreditProduct.CreditType.BUSINESS_CARD))
				return Single.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Los consumos solo pueden registrarse en tarjetas de crédito."));
			BigDecimal next = kind.equals("PAYMENT") ? c.getOutstanding().subtract(amount)
					: c.getOutstanding().add(amount);
			if (next.signum() < 0)
				return Single
						.error(new ResponseStatusException(HttpStatus.CONFLICT, "El pago supera la deuda pendiente."));
			if (next.compareTo(c.getCreditLimit()) > 0)
				return Single.error(new ResponseStatusException(HttpStatus.CONFLICT,
						"Se excedió el límite de crédito disponible."));
			c.setOutstanding(next);
			if (next.signum() == 0)
				c.setPastDue(false);
			c.getMovements().add(
					new CreditProduct.CreditMovement(UUID.randomUUID().toString(), kind, amount, Instant.now(), next));
			return RxJava3Adapter.monoToSingle(repo.save(c));
		});
	}
}
