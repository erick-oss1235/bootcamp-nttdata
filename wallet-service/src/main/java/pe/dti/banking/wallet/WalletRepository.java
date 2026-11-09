package pe.dti.banking.wallet;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface WalletRepository extends ReactiveMongoRepository<Wallet, String> {
	Mono<Wallet> findByPhone(String phone);
}
