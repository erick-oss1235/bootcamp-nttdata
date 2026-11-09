package pe.dti.banking.wallet;

import lombok.*;
import java.math.BigDecimal;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


@Document("wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {
	
	@Id
	private String id;
	@Version
	private Long version;
	@Indexed(unique = true)
	private String phone;
	private String documentType;
	private String documentNumber;
	private String imei;
	private String email;
	private String linkedDebitCardId;
	private BigDecimal balance = BigDecimal.ZERO;
}
