package pe.dti.banking.accountservice;

import lombok.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.util.*;

@Document("accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount {
	
	@Id
	private String id;
	@Version
	private Long version;
	@NotBlank
	private String customerId;
	@NotNull
	private AccountType type;
	private BigDecimal balance = BigDecimal.ZERO;
	private BigDecimal maintenanceFee = BigDecimal.ZERO;
	private Integer monthlyMovementLimit;
	private Integer freeMonthlyTransactions;
	private BigDecimal transactionFee = BigDecimal.ZERO;
	private BigDecimal openingDeposit = BigDecimal.ZERO;
	private BigDecimal minimumDailyAverage = BigDecimal.ZERO;
	private Integer fixedDay;
	private List<String> holderIds = new ArrayList<>();
	private List<String> authorizedSignerIds = new ArrayList<>();
	private List<AccountMovement> movements = new ArrayList<>();

	public enum AccountType {
		SAVINGS, CHECKING, FIXED_TERM
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AccountMovement {
		private String id;
		private String type;
		private BigDecimal amount;
		private java.time.Instant timestamp;
		private BigDecimal resultingBalance;
	}
}
