package pe.dti.banking.domain;

import java.math.BigDecimal;
import java.util.Objects;

public final class BankingRules {

	private BankingRules() { 
	}

	public static void requirePositive(BigDecimal amount) { 
		if (amount == null || amount.signum() <= 0) {
			throw new IllegalArgumentException("El importe debe ser mayor que cero.");
		}
	}

	public static BigDecimal withdrawalBalance(BigDecimal balance, BigDecimal withdrawal) { 
		requirePositive(withdrawal);
		Objects.requireNonNull(balance, "El saldo es obligatorio.");
		BigDecimal remaining = balance.subtract(withdrawal);
		if (remaining.signum() < 0) {
			throw new IllegalArgumentException("Saldo insuficiente para realizar la operación.");
		}
		return remaining;
	}

	public static BigDecimal cardOutstanding(BigDecimal outstanding, BigDecimal limit, BigDecimal charge) { 
		requirePositive(charge);
		Objects.requireNonNull(outstanding, "La deuda pendiente es obligatoria.");
		Objects.requireNonNull(limit, "El límite de crédito es obligatorio.");
		BigDecimal next = outstanding.add(charge);
		if (next.compareTo(limit) > 0) {
			throw new IllegalArgumentException("Se excedió el límite de crédito disponible.");
		}
		return next;
	}

	public static BigDecimal creditPayment(BigDecimal outstanding, BigDecimal payment) { 
		requirePositive(payment);
		Objects.requireNonNull(outstanding, "La deuda pendiente es obligatoria.");
		BigDecimal next = outstanding.subtract(payment);
		if (next.signum() < 0) {
			throw new IllegalArgumentException("El pago supera la deuda pendiente.");
		}
		return next;
	}

	public static BigDecimal transactionFee(long previousOperations, int freeOperations, BigDecimal fee) { 
		if (freeOperations < 0) {
			throw new IllegalArgumentException("El límite de operaciones gratuitas no puede ser negativo.");
		}
		if (previousOperations < freeOperations) {
			return BigDecimal.ZERO;
		}
		if (fee == null || fee.signum() < 0) {
			throw new IllegalArgumentException("La comisión por operación no es válida.");
		}
		return fee;
	}

	public static boolean compatibleProfile(String customerType, String profile) { 
		return switch (customerType) {
		case "PERSONAL" -> "STANDARD".equals(profile) || "VIP".equals(profile);
		case "BUSINESS" -> "STANDARD".equals(profile) || "PYME".equals(profile);
		default -> false;
		};
	}
}
