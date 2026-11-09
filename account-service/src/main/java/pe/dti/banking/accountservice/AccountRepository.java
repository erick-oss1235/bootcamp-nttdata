package pe.dti.banking.accountservice;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface AccountRepository extends ReactiveMongoRepository<BankAccount, String> {
	Flux<BankAccount> findByCustomerId(String id); 
}
