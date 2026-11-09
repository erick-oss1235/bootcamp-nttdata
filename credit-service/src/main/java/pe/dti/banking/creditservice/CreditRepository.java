package pe.dti.banking.creditservice;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;


public interface CreditRepository extends ReactiveMongoRepository<CreditProduct, String> {
	Flux<CreditProduct> findByCustomerId(String customerId); 
}
