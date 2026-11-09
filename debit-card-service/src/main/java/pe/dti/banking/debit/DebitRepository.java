package pe.dti.banking.debit;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;


public interface DebitRepository extends ReactiveMongoRepository<DebitCard, String> {
	Flux<DebitCard> findByCustomerId(String id); 
}
