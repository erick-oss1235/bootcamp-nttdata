package pe.dti.banking.accountservice;
import java.math.BigDecimal;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.*;
import reactor.adapter.rxjava.RxJava3Adapter;

@Component public class DebitPaymentConsumer  {
    private static final Logger log=LoggerFactory.getLogger(DebitPaymentConsumer.class);
    private final AccountService accounts;
    public DebitPaymentConsumer(AccountService accounts) { 
        this.accounts=accounts;
    }
    @KafkaListener(topics="banking.debit.payment.requests",groupId="banking-account-payments")
    public void handle(String message) { // Procesar evento de pago recibido
        try  {
            String[] p=message.split("\\|");
            if(p.length!=3)throw new IllegalArgumentException("El evento recibido no tiene el formato esperado.");
            RxJava3Adapter.singleToMono(accounts.move(p[1],new BigDecimal(p[2]),"WITHDRAWAL")).block();
            log.info("Evento de pago procesado correctamente. ID del evento: {}",p[0]);
        }
        catch(Exception e) {
            log.error("Error al procesar el pago. Configura la cola de mensajes fallidos y la política de reintentos.",e);
            throw e;
        }
    }
}
