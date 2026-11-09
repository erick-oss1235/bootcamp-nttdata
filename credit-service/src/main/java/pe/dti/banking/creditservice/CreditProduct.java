package pe.dti.banking.creditservice;

import lombok.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;


@Document("credit_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditProduct {
	
	@Id
	private String id;
	@Version
	private Long version;
	@NotBlank
	private String customerId;
	@NotNull
	private CreditType type;
	@NotNull
	@DecimalMin("0.01")
	private BigDecimal creditLimit;
	private BigDecimal outstanding = BigDecimal.ZERO;
	private boolean pastDue;
	private List<CreditMovement> movements = new ArrayList<>();

	public enum CreditType {
		PERSONAL_LOAN, BUSINESS_LOAN, PERSONAL_CARD, BUSINESS_CARD
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CreditMovement {
		private String id;
		private String type;
		private BigDecimal amount;
		private Instant timestamp;
		private BigDecimal resultingDebt;
	}
}
