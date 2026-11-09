import java.math.BigDecimal;
import pe.dti.banking.domain.BankingRules;

public class DomainSmokeTest {
    public static void main(String[] args) { // Iniciar la aplicación
        expect(BankingRules.withdrawalBalance(new BigDecimal("500"), new BigDecimal("150"))
                .compareTo(new BigDecimal("350")) == 0);
        expect(BankingRules.transactionFee(3, 3, new BigDecimal("2.50"))
                .compareTo(new BigDecimal("2.50")) == 0);
        expect(BankingRules.compatibleProfile("PERSONAL", "VIP"));
        expect(!BankingRules.compatibleProfile("PERSONAL", "PYME"));
        try {
            BankingRules.cardOutstanding(new BigDecimal("400"), new BigDecimal("500"),
                    new BigDecimal("200"));
            throw new AssertionError("No se aplicó la regla del límite de crédito.");
        } catch (IllegalArgumentException expected) {
            expect(expected.getMessage().contains("límite de crédito"));
        }
        System.out.println("Correcto: se aprobaron cinco comprobaciones de reglas bancarias.");
    }

    private static void expect(boolean expression) { // Comprobar condición esperada
        if (!expression) {
            throw new AssertionError("Falló una comprobación de las reglas bancarias.");
        }
    }
}
