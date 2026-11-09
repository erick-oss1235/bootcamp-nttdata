package pe.dti.banking.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
	private final String user;
	private final String passwordHash;
	private final JwtEncoder encoder;

	public AuthController(@Value("${banking.demo-user}") String user,
			@Value("${banking.demo-password-hash}") String passwordHash, 
			@Value("${banking.jwt-secret}") String secret) {
		this.user = user;
		this.passwordHash = passwordHash;
		this.encoder = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(
				secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
	}

	@PostMapping("/login")
	public Token login(@Valid @RequestBody Credentials body) { 
		if (!user.equals(body.username()) || !new BCryptPasswordEncoder().matches(body.password(), passwordHash))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El usuario o la contraseña son incorrectos.");
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer("dti-banking").subject(user).issuedAt(now)
				.expiresAt(now.plus(1, ChronoUnit.HOURS)).claim("scope", "banking.read banking.write").build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return new Token(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), "Bearer", 3600);
	}

	public record Credentials(@NotBlank String username, @NotBlank String password) {
	}

	public record Token(String accessToken, String tokenType, long expiresIn) {
	}
}
