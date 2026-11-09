package pe.dti.banking.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BankingRulesTest {

	@Test
	void withdrawsAmountFromAvailableBalance() { 
		assertEquals(new BigDecimal("350.00"),
				BankingRules.withdrawalBalance(new BigDecimal("500.00"), new BigDecimal("150.00")));
	}

	@Test
	void rejectsWithdrawalAboveAvailableBalance() { 
		assertThrows(IllegalArgumentException.class,
				() -> BankingRules.withdrawalBalance(new BigDecimal("50.00"), new BigDecimal("150.00")));
	}

	@Test
	void rejectsCardConsumptionOverLimit() { 
		assertThrows(IllegalArgumentException.class, () -> BankingRules.cardOutstanding(new BigDecimal("450.00"),
				new BigDecimal("500.00"), new BigDecimal("100.00")));
	}

	@Test
	void calculatesMonthlyTransactionFee() { 
		BigDecimal fee = new BigDecimal("2.50");
		assertEquals(BigDecimal.ZERO, BankingRules.transactionFee(2, 3, fee));
		assertEquals(fee, BankingRules.transactionFee(3, 3, fee));
	}

	@Test
	void enforcesCustomerProfiles() { 
		assertTrue(BankingRules.compatibleProfile("PERSONAL", "VIP"));
		assertTrue(BankingRules.compatibleProfile("BUSINESS", "PYME"));
		assertFalse(BankingRules.compatibleProfile("PERSONAL", "PYME"));
		assertFalse(BankingRules.compatibleProfile("BUSINESS", "VIP"));
	}
}
