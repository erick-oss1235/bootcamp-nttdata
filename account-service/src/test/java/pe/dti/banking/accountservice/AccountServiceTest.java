package pe.dti.banking.accountservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository repository;

    private AccountService service;

    @BeforeEach
    void setUp() {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"type\":\"PERSONAL\"}")
                        .build()));
        service = new AccountService(repository, builder, "http://customers", "http://credits");
    }

    @Test
    void opensSavingsAccountForPersonalCustomerr() { 
        BankAccount account = account("a1", BigDecimal.ZERO);
        account.setMonthlyMovementLimit(10);
        when(repository.findByCustomerId("c1")).thenReturn(Flux.empty());
        when(repository.save(any(BankAccount.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        service.create(account).test()
                .assertComplete()
                .assertValue(saved -> saved.getBalance().compareTo(BigDecimal.ZERO) == 0);

        verify(repository).save(any(BankAccount.class));
    }

    @Test
    void rejectsSecondSavingsAccount() { 
        BankAccount account = account("a1", BigDecimal.ZERO);
        account.setMonthlyMovementLimit(10);
        when(repository.findByCustomerId("c1")).thenReturn(Flux.just(account("existing", BigDecimal.ZERO)));

        service.create(account).test().assertError(ResponseStatusException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsWithdrawalAboveBalance() { 
        when(repository.findById("a1")).thenReturn(Mono.just(account("a1", new BigDecimal("80"))));

        service.move("a1", new BigDecimal("100"), "WITHDRAWAL")
                .test().assertError(ResponseStatusException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void depositsMoneyAndRegistersMovement() { 
        when(repository.findById("a1")).thenReturn(Mono.just(account("a1", new BigDecimal("300"))));
        when(repository.save(any(BankAccount.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        service.move("a1", new BigDecimal("200"), "DEPOSIT")
                .test().assertValue(saved -> saved.getBalance().compareTo(new BigDecimal("500")) == 0
                        && saved.getMovements().size() == 1)
                .assertComplete();
    }

    private BankAccount account(String id, BigDecimal balance) { 
        BankAccount account = new BankAccount();
        account.setId(id);
        account.setCustomerId("c1");
        account.setType(BankAccount.AccountType.SAVINGS);
        account.setBalance(balance);
        account.setMonthlyMovementLimit(10);
        account.setMovements(new ArrayList<>());
        return account;
    }
}
