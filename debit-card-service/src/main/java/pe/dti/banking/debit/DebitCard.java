package pe.dti.banking.debit;

import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;


@Document("debit_cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebitCard {
	
	@Id
	private String id;
	@Version
	private Long version;
	private String customerId;
	private String primaryAccountId;
	private String maskedPan;
	private boolean active = true;
	private Instant createdAt = Instant.now();
}
