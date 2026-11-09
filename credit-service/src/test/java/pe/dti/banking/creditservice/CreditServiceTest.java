package pe.dti.banking.creditservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
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
class CreditServiceTest {

    @Mock
    private CreditRepository repository;

    private CreditService service;

    @BeforeEach
    void setUp() { 
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"type\":\"PERSONAL\"}")
                        .build()));
        service = new CreditService(repository, builder, "http://customers");
    }

    @Test
    void allowsOnlyOnePersonalLoan() { 
        CreditProduct credit = credit(CreditProduct.CreditType.PERSONAL_LOAN);
        when(repository.findByCustomerId("c1")).thenReturn(Flux.just(credit));

        service.create(credit).test().assertError(ResponseStatusException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void registersLoanWithPrincipalOutstanding() { 
        CreditProduct credit = credit(CreditProduct.CreditType.PERSONAL_LOAN);
        when(repository.findByCustomerId("c1")).thenReturn(Flux.empty());
        when(repository.save(any(CreditProduct.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        service.create(credit).test()
                .assertValue(saved -> saved.getOutstanding().compareTo(new BigDecimal("500")) == 0)
                .assertComplete();
    }

    @Test
    void rejectsChargeAboveCardLimit() {
        CreditProduct credit = credit(CreditProduct.CreditType.PERSONAL_CARD);
        credit.setOutstanding(new BigDecimal("490"));
        when(repository.findById("card1")).thenReturn(Mono.just(credit));

        service.move("card1", new BigDecimal("20"), "CHARGE")
                .test().assertError(ResponseStatusException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void registersCreditCardPayment() { 
        CreditProduct credit = credit(CreditProduct.CreditType.PERSONAL_CARD);
        credit.setOutstanding(new BigDecimal("300"));
        when(repository.findById("card1")).thenReturn(Mono.just(credit));
        when(repository.save(any(CreditProduct.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

        service.move("card1", new BigDecimal("100"), "PAYMENT")
                .test().assertValue(saved -> saved.getOutstanding().compareTo(new BigDecimal("200")) == 0
                        && saved.getMovements().size() == 1)
                .assertComplete();
    }

    private CreditProduct credit(CreditProduct.CreditType type) { 
        CreditProduct credit = new CreditProduct();
        credit.setId("card1");
        credit.setCustomerId("c1");
        credit.setType(type);
        credit.setCreditLimit(new BigDecimal("500"));
        credit.setOutstanding(BigDecimal.ZERO);
        return credit;
    }
}
